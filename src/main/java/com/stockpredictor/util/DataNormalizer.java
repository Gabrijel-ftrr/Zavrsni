package com.stockpredictor.util;

import com.stockpredictor.model.NormalizationType;
import com.stockpredictor.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for normalizing and denormalizing stock data.
 * Supports Min-Max and Standard (z-score) normalization.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public final class DataNormalizer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataNormalizer.class);

    private DataNormalizer() {
        // Utility class - prevent instantiation
    }

    /**
     * Normalizes a list of stock data using the specified method.
     *
     * @param data              the data to normalize
     * @param normalizationType the normalization method
     * @return normalized data with normalization parameters
     */
    public static NormalizationResult normalize(List<StockData> data, 
                                                 NormalizationType normalizationType) {
        if (data == null || data.isEmpty()) {
            logger.warn("Empty data provided for normalization");
            return new NormalizationResult(data, new double[0]);
        }

        if (normalizationType == NormalizationType.NONE) {
            logger.debug("No normalization applied");
            return new NormalizationResult(data, new double[0]);
        }

        return switch (normalizationType) {
            case MIN_MAX -> applyMinMaxNormalization(data);
            case STANDARD -> applyStandardNormalization(data);
            default -> new NormalizationResult(data, new double[0]);
        };
    }

    /**
     * Applies Min-Max normalization to scale values to [0, 1].
     */
    private static NormalizationResult applyMinMaxNormalization(List<StockData> data) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (StockData stock : data) {
            min = Math.min(min, stock.getClose());
            max = Math.max(max, stock.getClose());
        }

        double range = max - min;
        if (range == 0) {
            logger.warn("Zero range in data - all values are equal");
            range = 1;
        }

        List<StockData> normalized = new ArrayList<>();
        for (StockData stock : data) {
            double normalizedClose = (stock.getClose() - min) / range;
            StockData normalizedStock = stock.withFeatures(
                stock.getMovingAverage(),
                stock.getRsi(),
                normalizedClose
            );
            normalized.add(normalizedStock);
        }

        logger.debug("Applied Min-Max normalization: min={}, max={}", min, max);
        return new NormalizationResult(normalized, new double[]{min, max});
    }

    /**
     * Applies Standard (z-score) normalization.
     */
    private static NormalizationResult applyStandardNormalization(List<StockData> data) {
        double sum = 0;
        for (StockData stock : data) {
            sum += stock.getClose();
        }
        double mean = sum / data.size();

        double sumSquaredDiff = 0;
        for (StockData stock : data) {
            sumSquaredDiff += Math.pow(stock.getClose() - mean, 2);
        }
        double std = Math.sqrt(sumSquaredDiff / data.size());
        
        if (std == 0) {
            logger.warn("Zero standard deviation - all values are equal");
            std = 1;
        }

        List<StockData> normalized = new ArrayList<>();
        for (StockData stock : data) {
            double normalizedClose = (stock.getClose() - mean) / std;
            StockData normalizedStock = stock.withFeatures(
                stock.getMovingAverage(),
                stock.getRsi(),
                normalizedClose
            );
            normalized.add(normalizedStock);
        }

        logger.debug("Applied Standard normalization: mean={}, std={}", mean, std);
        return new NormalizationResult(normalized, new double[]{mean, std});
    }

    /**
     * Denormalizes a value using the provided parameters.
     *
     * @param value             the normalized value
     * @param normalizationType the normalization type used
     * @param params            normalization parameters [min, max] or [mean, std]
     * @return the denormalized value
     */
    public static double denormalize(double value, NormalizationType normalizationType,
                                     double[] params) {
        if (normalizationType == NormalizationType.NONE || params == null || params.length < 2) {
            return value;
        }

        return switch (normalizationType) {
            case MIN_MAX -> value * (params[1] - params[0]) + params[0];
            case STANDARD -> value * params[1] + params[0];
            default -> value;
        };
    }

    /**
     * Holds the result of a normalization operation.
     */
    public record NormalizationResult(List<StockData> data, double[] params) {}
}
