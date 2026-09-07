package com.stockpredictor.service;

import com.stockpredictor.model.DataSet;
import com.stockpredictor.model.NormalizationType;
import com.stockpredictor.model.StockData;
import com.stockpredictor.util.DataNormalizer;
import com.stockpredictor.util.FeatureExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for preparing stock data for machine learning.
 * Handles train/test splitting, feature extraction, and normalization.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class DataPreparationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataPreparationService.class);
    private static final double DEFAULT_TRAIN_RATIO = 0.8;

    /**
     * Prepares a complete dataset with train/test split.
     *
     * @param rawData           raw stock data
     * @param symbol            stock symbol
     * @param trainRatio        ratio of data for training (0.0-1.0)
     * @param normalizationType normalization method to apply
     * @param extractMA         whether to extract moving average
     * @param extractRSI        whether to extract RSI
     * @return prepared DataSet
     */
    public DataSet prepareDataSet(List<StockData> rawData, String symbol, double trainRatio,
                                   NormalizationType normalizationType, 
                                   boolean extractMA, boolean extractRSI) {
        logger.info("Preparing dataset for {} with {} samples", symbol, rawData.size());

        validateInputs(rawData, trainRatio);

        // Extract features first
        List<StockData> dataWithFeatures = FeatureExtractor.extractFeatures(
            rawData, extractMA, extractRSI
        );

        // Apply normalization
        DataNormalizer.NormalizationResult normResult = DataNormalizer.normalize(
            dataWithFeatures, normalizationType
        );
        List<StockData> normalizedData = normResult.data();
        double[] normParams = normResult.params();

        // Split into train/test
        int splitIndex = (int) (normalizedData.size() * trainRatio);
        List<StockData> trainingData = new ArrayList<>(normalizedData.subList(0, splitIndex));
        List<StockData> testData = new ArrayList<>(normalizedData.subList(splitIndex, 
                                                                          normalizedData.size()));

        DataSet dataSet = new DataSet(
            normalizedData, trainingData, testData, symbol, trainRatio,
            normalizationType, normParams
        );

        logger.info("Dataset prepared: {} train, {} test samples", 
                   trainingData.size(), testData.size());
        
        return dataSet;
    }

    /**
     * Prepares a dataset with default settings.
     *
     * @param rawData raw stock data
     * @param symbol  stock symbol
     * @return prepared DataSet
     */
    public DataSet prepareDataSet(List<StockData> rawData, String symbol) {
        return prepareDataSet(rawData, symbol, DEFAULT_TRAIN_RATIO, 
                             NormalizationType.MIN_MAX, true, true);
    }

    /**
     * Prepares data for prediction (no split needed).
     *
     * @param rawData           raw stock data
     * @param normalizationType normalization method
     * @param normParams        existing normalization parameters
     * @param extractMA         whether to extract MA
     * @param extractRSI        whether to extract RSI
     * @return processed data ready for prediction
     */
    public List<StockData> prepareForPrediction(List<StockData> rawData,
                                                 NormalizationType normalizationType,
                                                 double[] normParams,
                                                 boolean extractMA, boolean extractRSI) {
        logger.debug("Preparing {} samples for prediction", rawData.size());

        // Extract features
        List<StockData> dataWithFeatures = FeatureExtractor.extractFeatures(
            rawData, extractMA, extractRSI
        );

        // Apply same normalization as training
        if (normalizationType != NormalizationType.NONE && normParams != null) {
            return applyNormalization(dataWithFeatures, normalizationType, normParams);
        }

        return dataWithFeatures;
    }

    /**
     * Applies normalization using existing parameters.
     */
    private List<StockData> applyNormalization(List<StockData> data, 
                                                NormalizationType type, double[] params) {
        List<StockData> result = new ArrayList<>();

        for (StockData stock : data) {
            double normalizedClose;
            
            if (type == NormalizationType.MIN_MAX) {
                double range = params[1] - params[0];
                normalizedClose = range > 0 ? (stock.getClose() - params[0]) / range : 0;
            } else {
                normalizedClose = params[1] > 0 ? (stock.getClose() - params[0]) / params[1] : 0;
            }

            result.add(stock.withFeatures(stock.getMovingAverage(), stock.getRsi(), 
                                          normalizedClose));
        }

        return result;
    }

    /**
     * Creates a time-series cross-validation split.
     *
     * @param data    the full dataset
     * @param nSplits number of folds
     * @return list of train/test pairs
     */
    public List<TrainTestSplit> createTimeSeriesSplits(List<StockData> data, int nSplits) {
        logger.debug("Creating {} time-series splits", nSplits);

        List<TrainTestSplit> splits = new ArrayList<>();
        int minTrainSize = data.size() / (nSplits + 1);
        int foldSize = (data.size() - minTrainSize) / nSplits;

        for (int i = 0; i < nSplits; i++) {
            int trainEnd = minTrainSize + i * foldSize;
            int testEnd = trainEnd + foldSize;

            if (testEnd > data.size()) {
                testEnd = data.size();
            }

            List<StockData> trainData = new ArrayList<>(data.subList(0, trainEnd));
            List<StockData> testData = new ArrayList<>(data.subList(trainEnd, testEnd));

            splits.add(new TrainTestSplit(trainData, testData));
        }

        return splits;
    }

    /**
     * Filters out incomplete data points.
     *
     * @param data raw data
     * @return filtered data with only complete records
     */
    public List<StockData> filterIncompleteData(List<StockData> data) {
        return data.stream()
            .filter(d -> d.getClose() > 0 && d.getVolume() >= 0)
            .toList();
    }

    /**
     * Removes outliers based on z-score.
     *
     * @param data      stock data
     * @param threshold z-score threshold (typically 3.0)
     * @return data with outliers removed
     */
    public List<StockData> removeOutliers(List<StockData> data, double threshold) {
        if (data.size() < 3) {
            return data;
        }

        // Calculate mean and std of close prices
        double sum = data.stream().mapToDouble(StockData::getClose).sum();
        double mean = sum / data.size();
        
        double sumSquaredDiff = data.stream()
            .mapToDouble(d -> Math.pow(d.getClose() - mean, 2))
            .sum();
        double std = Math.sqrt(sumSquaredDiff / data.size());

        if (std == 0) {
            return data;
        }

        List<StockData> filtered = data.stream()
            .filter(d -> Math.abs((d.getClose() - mean) / std) <= threshold)
            .toList();

        logger.debug("Removed {} outliers from {} data points", 
                    data.size() - filtered.size(), data.size());
        
        return filtered;
    }

    /**
     * Validates input data and parameters.
     */
    private void validateInputs(List<StockData> data, double trainRatio) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        if (trainRatio <= 0 || trainRatio >= 1) {
            throw new IllegalArgumentException("Train ratio must be between 0 and 1");
        }
        if (data.size() < 10) {
            throw new IllegalArgumentException("Need at least 10 data points for training");
        }
    }

    /**
     * Represents a train/test split for cross-validation.
     */
    public record TrainTestSplit(List<StockData> trainData, List<StockData> testData) {}
}
