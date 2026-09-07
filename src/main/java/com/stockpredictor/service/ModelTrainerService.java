package com.stockpredictor.service;

import com.stockpredictor.model.*;
import com.stockpredictor.util.DataNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service for training and managing machine learning models for stock prediction.
 * Supports Linear Regression, Random Forest, and Multilayer Perceptron models.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class ModelTrainerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelTrainerService.class);
    private static final int LOOKBACK_WINDOW = 5; // Days of history for prediction
    
    private final ExecutorService executor;
    private final Map<ModelType, TrainedModel> trainedModels;

    /**
     * Constructs a new ModelTrainerService.
     */
    public ModelTrainerService() {
        this.executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
        );
        this.trainedModels = new ConcurrentHashMap<>();
    }

    /**
     * Trains a specific model type on the given dataset.
     *
     * @param dataSet   the training data
     * @param modelType the type of model to train
     * @return the trained model with metrics
     * @throws Exception if training fails
     */
    public TrainedModel trainModel(DataSet dataSet, ModelType modelType) throws Exception {
        logger.info("Training {} model on {} samples", modelType.getDisplayName(), 
                   dataSet.getTrainingData().size());

        long startTime = System.currentTimeMillis();

        // Prepare Weka instances
        Instances trainingInstances = prepareInstances(dataSet.getTrainingData(), "train");
        Instances testInstances = prepareInstances(dataSet.getTestData(), "test");

        // Create and train classifier
        Classifier classifier = createClassifier(modelType);
        classifier.buildClassifier(trainingInstances);

        long trainingTime = System.currentTimeMillis() - startTime;

        // Evaluate on test set
        ModelMetrics metrics = evaluateModel(classifier, testInstances, modelType, 
                                             trainingTime, trainingInstances.numInstances());

        // Create trained model wrapper
        TrainedModel trainedModel = new TrainedModel(
            classifier, modelType, metrics, dataSet.getSymbol(),
            dataSet.getNormalizationType(), dataSet.getNormalizationParams(),
            new Instances(trainingInstances, 0) // Empty structure for predictions
        );

        trainedModels.put(modelType, trainedModel);
        
        logger.info("Training complete: {} - RMSE: {}", modelType.getDisplayName(), 
                   metrics.getRmse());
        
        return trainedModel;
    }

    /**
     * Trains all models and returns the best one based on RMSE.
     *
     * @param dataSet  the training data
     * @param quickMode if true, only trains lightweight models
     * @return map of model type to trained model
     */
    public Map<ModelType, TrainedModel> trainAllModels(DataSet dataSet, boolean quickMode) {
        logger.info("Training all models (quickMode: {})", quickMode);

        Map<ModelType, TrainedModel> results = new ConcurrentHashMap<>();
        List<Future<TrainedModel>> futures = new ArrayList<>();

        for (ModelType type : ModelType.values()) {
            if (quickMode && !type.isLightweight()) {
                logger.debug("Skipping {} in quick mode", type);
                continue;
            }

            futures.add(executor.submit(() -> {
                try {
                    return trainModel(dataSet, type);
                } catch (Exception e) {
                    logger.error("Failed to train {}: {}", type, e.getMessage());
                    return null;
                }
            }));
        }

        for (Future<TrainedModel> future : futures) {
            try {
                TrainedModel model = future.get(5, TimeUnit.MINUTES);
                if (model != null) {
                    results.put(model.getModelType(), model);
                }
            } catch (TimeoutException e) {
                logger.warn("Model training timed out");
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error during parallel training: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * Selects the best model based on RMSE.
     *
     * @param models map of trained models
     * @return the best performing model
     */
    public TrainedModel selectBestModel(Map<ModelType, TrainedModel> models) {
        return models.values().stream()
            .min(Comparator.comparing(m -> m.getMetrics().getRmse()))
            .orElse(null);
    }

    /**
     * Makes a prediction using a trained model.
     *
     * @param model     the trained model
     * @param inputData recent stock data for prediction
     * @return the predicted price
     * @throws Exception if prediction fails
     */
    public double predict(TrainedModel model, List<StockData> inputData) throws Exception {
        if (inputData.size() < LOOKBACK_WINDOW) {
            throw new IllegalArgumentException("Need at least " + LOOKBACK_WINDOW + " data points");
        }

        Instances structure = model.getDataStructure();
        Instance instance = createInstance(inputData, structure);
        
        double prediction = model.getClassifier().classifyInstance(instance);
        return model.denormalize(prediction);
    }

    /**
     * Creates a Weka classifier for the given model type.
     */
    private Classifier createClassifier(ModelType modelType) throws Exception {
        return switch (modelType) {
            case LINEAR_REGRESSION -> {
                LinearRegression lr = new LinearRegression();
                lr.setOptions(new String[]{"-S", "0", "-R", "1.0E-8"});
                yield lr;
            }
            case RANDOM_FOREST -> {
                RandomForest rf = new RandomForest();
                rf.setNumIterations(100);
                rf.setMaxDepth(10);
                rf.setNumFeatures(0); // Auto-select
                yield rf;
            }
            case MLP -> {
                MultilayerPerceptron mlp = new MultilayerPerceptron();
                mlp.setHiddenLayers("10,5");
                mlp.setLearningRate(0.1);
                mlp.setMomentum(0.2);
                mlp.setTrainingTime(500);
                yield mlp;
            }
        };
    }

    /**
     * Prepares Weka Instances from stock data.
     */
    private Instances prepareInstances(List<StockData> data, String relationName) {
        // Create attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        
        // Lookback features
        for (int i = 0; i < LOOKBACK_WINDOW; i++) {
            attributes.add(new Attribute("close_t-" + (i + 1)));
            attributes.add(new Attribute("volume_t-" + (i + 1)));
        }
        
        // Optional features
        attributes.add(new Attribute("moving_average"));
        attributes.add(new Attribute("rsi"));
        
        // Target variable
        attributes.add(new Attribute("target_close"));

        Instances instances = new Instances(relationName, attributes, data.size());
        instances.setClassIndex(instances.numAttributes() - 1);

        // Add instances
        for (int i = LOOKBACK_WINDOW; i < data.size(); i++) {
            Instance instance = createTrainingInstance(data, i, instances);
            instances.add(instance);
        }

        return instances;
    }

    /**
     * Creates a training instance from stock data.
     */
    private Instance createTrainingInstance(List<StockData> data, int index, Instances structure) {
        double[] values = new double[structure.numAttributes()];
        int attrIdx = 0;

        // Lookback features
        for (int i = 0; i < LOOKBACK_WINDOW; i++) {
            StockData past = data.get(index - i - 1);
            values[attrIdx++] = past.getNormalizedClose() != null ? 
                               past.getNormalizedClose() : past.getClose();
            values[attrIdx++] = past.getVolume();
        }

        // Optional features
        StockData current = data.get(index - 1);
        values[attrIdx++] = current.getMovingAverage() != null ? current.getMovingAverage() : 0;
        values[attrIdx++] = current.getRsi() != null ? current.getRsi() : 50;

        // Target
        StockData target = data.get(index);
        values[attrIdx] = target.getNormalizedClose() != null ? 
                         target.getNormalizedClose() : target.getClose();

        return new DenseInstance(1.0, values);
    }

    /**
     * Creates a prediction instance from recent stock data.
     */
    private Instance createInstance(List<StockData> data, Instances structure) {
        double[] values = new double[structure.numAttributes()];
        int attrIdx = 0;
        int startIdx = data.size() - LOOKBACK_WINDOW;

        for (int i = 0; i < LOOKBACK_WINDOW; i++) {
            StockData past = data.get(startIdx + LOOKBACK_WINDOW - i - 1);
            values[attrIdx++] = past.getNormalizedClose() != null ? 
                               past.getNormalizedClose() : past.getClose();
            values[attrIdx++] = past.getVolume();
        }

        StockData last = data.get(data.size() - 1);
        values[attrIdx++] = last.getMovingAverage() != null ? last.getMovingAverage() : 0;
        values[attrIdx++] = last.getRsi() != null ? last.getRsi() : 50;
        values[attrIdx] = 0; // Unknown target

        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(structure);
        return instance;
    }

    /**
     * Evaluates a trained model on test data.
     */
    private ModelMetrics evaluateModel(Classifier classifier, Instances testData,
                                        ModelType modelType, long trainingTime, 
                                        int trainSize) throws Exception {
        double sumSquaredError = 0;
        double sumAbsoluteError = 0;
        double sumActual = 0;
        double sumSquaredTotal = 0;
        int count = testData.numInstances();

        // Calculate mean for R²
        for (int i = 0; i < count; i++) {
            sumActual += testData.instance(i).classValue();
        }
        double meanActual = sumActual / count;

        // Calculate errors
        for (int i = 0; i < count; i++) {
            Instance instance = testData.instance(i);
            double actual = instance.classValue();
            double predicted = classifier.classifyInstance(instance);
            
            double error = predicted - actual;
            sumSquaredError += error * error;
            sumAbsoluteError += Math.abs(error);
            sumSquaredTotal += Math.pow(actual - meanActual, 2);
        }

        double rmse = Math.sqrt(sumSquaredError / count);
        double mae = sumAbsoluteError / count;
        double rSquared = 1 - (sumSquaredError / sumSquaredTotal);

        return new ModelMetrics(
            modelType.getDisplayName(), rmse, mae, rSquared,
            trainingTime, trainSize, count
        );
    }

    /**
     * Gets a previously trained model.
     *
     * @param modelType the model type
     * @return the trained model, or null if not trained
     */
    public TrainedModel getTrainedModel(ModelType modelType) {
        return trainedModels.get(modelType);
    }

    /**
     * Clears all trained models.
     */
    public void clearModels() {
        trainedModels.clear();
        logger.info("Cleared all trained models");
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executor.shutdown();
        logger.info("ModelTrainerService executor shutdown");
    }
}
