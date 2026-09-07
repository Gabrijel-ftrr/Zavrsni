package com.stockpredictor.model;

import weka.classifiers.Classifier;
import weka.core.Instances;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Wrapper class for a trained Weka classifier with associated metadata.
 * Contains the model, training parameters, and normalization information.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class TrainedModel implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final Classifier classifier;
    private final ModelType modelType;
    private final ModelMetrics metrics;
    private final LocalDateTime trainedAt;
    private final String symbol;
    private final NormalizationType normalizationType;
    private final double[] normalizationParams; // [min, max] or [mean, std]
    private final Instances dataStructure;

    /**
     * Constructs a TrainedModel with all required components.
     *
     * @param classifier          the trained Weka classifier
     * @param modelType           the type of model
     * @param metrics             evaluation metrics for the model
     * @param symbol              the stock symbol this was trained on
     * @param normalizationType   the normalization method used
     * @param normalizationParams parameters for denormalization
     * @param dataStructure       the Weka Instances structure (without data)
     */
    public TrainedModel(Classifier classifier, ModelType modelType, ModelMetrics metrics,
                        String symbol, NormalizationType normalizationType,
                        double[] normalizationParams, Instances dataStructure) {
        this.classifier = Objects.requireNonNull(classifier, "Classifier cannot be null");
        this.modelType = Objects.requireNonNull(modelType, "Model type cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "Metrics cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.normalizationType = Objects.requireNonNull(normalizationType);
        this.normalizationParams = normalizationParams != null ? 
                                   normalizationParams.clone() : new double[0];
        this.dataStructure = dataStructure;
        this.trainedAt = LocalDateTime.now();
    }

    /**
     * Denormalizes a predicted value back to the original scale.
     *
     * @param normalizedValue the normalized prediction
     * @return the denormalized value
     */
    public double denormalize(double normalizedValue) {
        if (normalizationType == NormalizationType.NONE || normalizationParams.length < 2) {
            return normalizedValue;
        }
        
        return switch (normalizationType) {
            case MIN_MAX -> normalizedValue * (normalizationParams[1] - normalizationParams[0]) 
                           + normalizationParams[0];
            case STANDARD -> normalizedValue * normalizationParams[1] + normalizationParams[0];
            default -> normalizedValue;
        };
    }

    // Getters
    public Classifier getClassifier() { return classifier; }
    public ModelType getModelType() { return modelType; }
    public ModelMetrics getMetrics() { return metrics; }
    public LocalDateTime getTrainedAt() { return trainedAt; }
    public String getSymbol() { return symbol; }
    public NormalizationType getNormalizationType() { return normalizationType; }
    public double[] getNormalizationParams() { return normalizationParams.clone(); }
    public Instances getDataStructure() { return dataStructure; }

    @Override
    public String toString() {
        return String.format("TrainedModel{type=%s, symbol='%s', RMSE=%.4f, trainedAt=%s}",
                modelType.getDisplayName(), symbol, metrics.getRmse(), trainedAt);
    }
}
