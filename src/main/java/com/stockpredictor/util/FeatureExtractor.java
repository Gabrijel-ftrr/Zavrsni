package com.stockpredictor.util;

import com.stockpredictor.model.StockData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting technical indicators from stock data.
 * Supports Moving Average and RSI (Relative Strength Index) calculation.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public final class FeatureExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureExtractor.class);
    private static final int DEFAULT_MA_PERIOD = 20;
    private static final int DEFAULT_RSI_PERIOD = 14;

    private FeatureExtractor() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts all features from stock data using default periods.
     *
     * @param data              the stock data
     * @param includeMA         whether to calculate moving average
     * @param includeRSI        whether to calculate RSI
     * @return data with extracted features
     */
    public static List<StockData> extractFeatures(List<StockData> data, 
                                                   boolean includeMA, boolean includeRSI) {
        return extractFeatures(data, includeMA, includeRSI, DEFAULT_MA_PERIOD, DEFAULT_RSI_PERIOD);
    }

    /**
     * Extracts features from stock data with custom periods.
     *
     * @param data       the stock data
     * @param includeMA  whether to calculate moving average
     * @param includeRSI whether to calculate RSI
     * @param maPeriod   moving average period
     * @param rsiPeriod  RSI period
     * @return data with extracted features
     */
    public static List<StockData> extractFeatures(List<StockData> data, boolean includeMA,
                                                   boolean includeRSI, int maPeriod, int rsiPeriod) {
        if (data == null || data.isEmpty()) {
            logger.warn("Empty data provided for feature extraction");
            return new ArrayList<>();
        }

        logger.debug("Extracting features: MA={}, RSI={}", includeMA, includeRSI);

        List<Double> movingAverages = includeMA ? calculateMovingAverage(data, maPeriod) : null;
        List<Double> rsiValues = includeRSI ? calculateRSI(data, rsiPeriod) : null;

        List<StockData> result = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            StockData stock = data.get(i);
            Double ma = movingAverages != null ? movingAverages.get(i) : null;
            Double rsi = rsiValues != null ? rsiValues.get(i) : null;
            
            StockData withFeatures = stock.withFeatures(ma, rsi, stock.getNormalizedClose());
            result.add(withFeatures);
        }

        logger.debug("Feature extraction complete for {} data points", data.size());
        return result;
    }

    /**
     * Calculates Simple Moving Average for the given period.
     *
     * @param data   stock data
     * @param period moving average period
     * @return list of MA values (null for insufficient data points)
     */
    public static List<Double> calculateMovingAverage(List<StockData> data, int period) {
        List<Double> result = new ArrayList<>();
        
        for (int i = 0; i < data.size(); i++) {
            if (i < period - 1) {
                result.add(null);
                continue;
            }

            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += data.get(j).getClose();
            }
            result.add(sum / period);
        }

        logger.debug("Calculated {}-period moving average", period);
        return result;
    }

    /**
     * Calculates Relative Strength Index (RSI).
     *
     * @param data   stock data
     * @param period RSI period (typically 14)
     * @return list of RSI values (0-100 scale, null for insufficient data)
     */
    public static List<Double> calculateRSI(List<StockData> data, int period) {
        List<Double> result = new ArrayList<>();
        
        if (data.size() < period + 1) {
            logger.warn("Insufficient data for RSI calculation");
            for (int i = 0; i < data.size(); i++) {
                result.add(null);
            }
            return result;
        }

        // Calculate price changes
        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();
        gains.add(0.0);
        losses.add(0.0);

        for (int i = 1; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            gains.add(change > 0 ? change : 0);
            losses.add(change < 0 ? -change : 0);
        }

        // Calculate RSI for each point
        for (int i = 0; i < data.size(); i++) {
            if (i < period) {
                result.add(null);
                continue;
            }

            double avgGain = 0;
            double avgLoss = 0;

            // Initial average for first RSI calculation
            if (i == period) {
                for (int j = 1; j <= period; j++) {
                    avgGain += gains.get(j);
                    avgLoss += losses.get(j);
                }
                avgGain /= period;
                avgLoss /= period;
            } else {
                // Smoothed moving average for subsequent calculations
                double prevAvgGain = calculateInitialAverage(gains, i - 1, period);
                double prevAvgLoss = calculateInitialAverage(losses, i - 1, period);
                avgGain = (prevAvgGain * (period - 1) + gains.get(i)) / period;
                avgLoss = (prevAvgLoss * (period - 1) + losses.get(i)) / period;
            }

            double rsi;
            if (avgLoss == 0) {
                rsi = 100;
            } else {
                double rs = avgGain / avgLoss;
                rsi = 100 - (100 / (1 + rs));
            }
            result.add(rsi);
        }

        logger.debug("Calculated {}-period RSI", period);
        return result;
    }

    /**
     * Helper method to calculate initial average for RSI.
     */
    private static double calculateInitialAverage(List<Double> values, int endIndex, int period) {
        double sum = 0;
        int startIndex = Math.max(1, endIndex - period + 1);
        for (int i = startIndex; i <= endIndex; i++) {
            sum += values.get(i);
        }
        return sum / period;
    }

    /**
     * Calculates Exponential Moving Average (EMA).
     *
     * @param data   stock data
     * @param period EMA period
     * @return list of EMA values
     */
    public static List<Double> calculateEMA(List<StockData> data, int period) {
        List<Double> result = new ArrayList<>();
        
        if (data.isEmpty()) {
            return result;
        }

        double multiplier = 2.0 / (period + 1);
        double ema = data.get(0).getClose();
        result.add(ema);

        for (int i = 1; i < data.size(); i++) {
            ema = (data.get(i).getClose() - ema) * multiplier + ema;
            result.add(ema);
        }

        logger.debug("Calculated {}-period EMA", period);
        return result;
    }
}
