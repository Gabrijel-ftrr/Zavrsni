package com.stockpredictor.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds a dataset with training and testing splits.
 * Provides access to both raw stock data and processed data.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class DataSet {
    
    private final List<StockData> allData;
    private final List<StockData> trainingData;
    private final List<StockData> testData;
    private final String symbol;
    private final double trainTestRatio;
    private final NormalizationType normalizationType;
    private final double[] normalizationParams;

    /**
     * Constructs a DataSet with training and test splits.
     *
     * @param allData             complete dataset
     * @param trainingData        training portion
     * @param testData            testing portion
     * @param symbol              stock symbol
     * @param trainTestRatio      ratio of training to total data
     * @param normalizationType   normalization method applied
     * @param normalizationParams normalization parameters
     */
    public DataSet(List<StockData> allData, List<StockData> trainingData,
                   List<StockData> testData, String symbol, double trainTestRatio,
                   NormalizationType normalizationType, double[] normalizationParams) {
        this.allData = Collections.unmodifiableList(Objects.requireNonNull(allData));
        this.trainingData = Collections.unmodifiableList(Objects.requireNonNull(trainingData));
        this.testData = Collections.unmodifiableList(Objects.requireNonNull(testData));
        this.symbol = Objects.requireNonNull(symbol);
        this.trainTestRatio = trainTestRatio;
        this.normalizationType = Objects.requireNonNull(normalizationType);
        this.normalizationParams = normalizationParams != null ? 
                                   normalizationParams.clone() : new double[0];
    }

    /**
     * Creates a DataSet without normalization.
     *
     * @param allData       complete dataset
     * @param trainingData  training portion
     * @param testData      testing portion
     * @param symbol        stock symbol
     * @param trainTestRatio ratio of training to total data
     * @return a new DataSet instance
     */
    public static DataSet create(List<StockData> allData, List<StockData> trainingData,
                                 List<StockData> testData, String symbol, double trainTestRatio) {
        return new DataSet(allData, trainingData, testData, symbol, trainTestRatio,
                          NormalizationType.NONE, new double[0]);
    }

    /**
     * Returns statistics about the dataset.
     *
     * @return a summary string
     */
    public String getSummary() {
        return String.format(
            "DataSet for %s%n" +
            "  Total samples: %d%n" +
            "  Training samples: %d (%.1f%%)%n" +
            "  Test samples: %d (%.1f%%)%n" +
            "  Normalization: %s",
            symbol, allData.size(),
            trainingData.size(), trainTestRatio * 100,
            testData.size(), (1 - trainTestRatio) * 100,
            normalizationType.getDisplayName()
        );
    }

    // Getters
    public List<StockData> getAllData() { return allData; }
    public List<StockData> getTrainingData() { return trainingData; }
    public List<StockData> getTestData() { return testData; }
    public String getSymbol() { return symbol; }
    public double getTrainTestRatio() { return trainTestRatio; }
    public NormalizationType getNormalizationType() { return normalizationType; }
    public double[] getNormalizationParams() { return normalizationParams.clone(); }

    @Override
    public String toString() {
        return String.format("DataSet{symbol='%s', total=%d, train=%d, test=%d}",
                symbol, allData.size(), trainingData.size(), testData.size());
    }
}
