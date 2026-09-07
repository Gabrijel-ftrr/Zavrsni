package com.stockpredictor.config;

import com.stockpredictor.model.NormalizationType;

/**
 * App konfiguracijske postavke.
 * Sprema korisnikove preferencije i defaultne vrijednosti.
 *
 * @author Gabrijel Matanović
 * @version 1.0.0
 */
public class AppConfig {
    
    private static AppConfig instance;

    // API postavke
    private String alphaVantageApiKey = "WAQ7MMUTF1PQ41OY";

    // Data postavke
    private double trainTestRatio = 0.8;
    private NormalizationType normalizationType = NormalizationType.MIN_MAX;
    private boolean extractMovingAverage = true;
    private boolean extractRSI = true;
    private int movingAveragePeriod = 20;
    private int rsiPeriod = 14;

    // Backtest postavke
    private double initialInvestment = 10000.0;
    private double transactionCost = 0.001;

    // UI postavke
    private String defaultSymbol = "AAPL";
    private int defaultHistoryDays = 365;
    private boolean showGridLines = true;
    private boolean animateCharts = false;

    // Model postavke
    private boolean quickModeDefault = true;
    private int randomForestTrees = 100;
    private int mlpHiddenLayers = 2;
    private double mlpLearningRate = 0.1;

    private AppConfig() {
        // Privatni konstruktor za singleton
        // Pokušaj učitati API iz okruženja
        String envKey = System.getenv("ALPHA_VANTAGE_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            this.alphaVantageApiKey = envKey;
        }
    }

    /**
     * Uzimasingleton instancu.
     *
     * @return  AppConfig instancu
     */
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    // API Key metoda
    public String getAlphaVantageApiKey() { return alphaVantageApiKey; }
    public void setAlphaVantageApiKey(String apiKey) { 
        this.alphaVantageApiKey = apiKey != null ? apiKey : ""; 
    }
    
    public boolean hasApiKey() {
        return alphaVantageApiKey != null && !alphaVantageApiKey.isEmpty();
    }

    // Getters and Setters
    public double getTrainTestRatio() { return trainTestRatio; }
    public void setTrainTestRatio(double trainTestRatio) { 
        this.trainTestRatio = Math.max(0.5, Math.min(0.95, trainTestRatio)); 
    }

    public NormalizationType getNormalizationType() { return normalizationType; }
    public void setNormalizationType(NormalizationType normalizationType) { 
        this.normalizationType = normalizationType; 
    }

    public boolean isExtractMovingAverage() { return extractMovingAverage; }
    public void setExtractMovingAverage(boolean extractMovingAverage) { 
        this.extractMovingAverage = extractMovingAverage; 
    }

    public boolean isExtractRSI() { return extractRSI; }
    public void setExtractRSI(boolean extractRSI) { 
        this.extractRSI = extractRSI; 
    }

    public int getMovingAveragePeriod() { return movingAveragePeriod; }
    public void setMovingAveragePeriod(int movingAveragePeriod) { 
        this.movingAveragePeriod = Math.max(5, Math.min(100, movingAveragePeriod)); 
    }

    public int getRsiPeriod() { return rsiPeriod; }
    public void setRsiPeriod(int rsiPeriod) { 
        this.rsiPeriod = Math.max(5, Math.min(50, rsiPeriod)); 
    }

    public double getInitialInvestment() { return initialInvestment; }
    public void setInitialInvestment(double initialInvestment) { 
        this.initialInvestment = Math.max(100, initialInvestment); 
    }

    public double getTransactionCost() { return transactionCost; }
    public void setTransactionCost(double transactionCost) { 
        this.transactionCost = Math.max(0, Math.min(0.05, transactionCost)); 
    }

    public String getDefaultSymbol() { return defaultSymbol; }
    public void setDefaultSymbol(String defaultSymbol) { 
        this.defaultSymbol = defaultSymbol != null ? defaultSymbol.toUpperCase() : "AAPL"; 
    }

    public int getDefaultHistoryDays() { return defaultHistoryDays; }
    public void setDefaultHistoryDays(int defaultHistoryDays) { 
        this.defaultHistoryDays = Math.max(30, Math.min(3650, defaultHistoryDays)); 
    }

    public boolean isShowGridLines() { return showGridLines; }
    public void setShowGridLines(boolean showGridLines) { 
        this.showGridLines = showGridLines; 
    }

    public boolean isAnimateCharts() { return animateCharts; }
    public void setAnimateCharts(boolean animateCharts) { 
        this.animateCharts = animateCharts; 
    }

    public boolean isQuickModeDefault() { return quickModeDefault; }
    public void setQuickModeDefault(boolean quickModeDefault) { 
        this.quickModeDefault = quickModeDefault; 
    }

    public int getRandomForestTrees() { return randomForestTrees; }
    public void setRandomForestTrees(int randomForestTrees) { 
        this.randomForestTrees = Math.max(10, Math.min(500, randomForestTrees)); 
    }

    public int getMlpHiddenLayers() { return mlpHiddenLayers; }
    public void setMlpHiddenLayers(int mlpHiddenLayers) { 
        this.mlpHiddenLayers = Math.max(1, Math.min(5, mlpHiddenLayers)); 
    }

    public double getMlpLearningRate() { return mlpLearningRate; }
    public void setMlpLearningRate(double mlpLearningRate) { 
        this.mlpLearningRate = Math.max(0.001, Math.min(1.0, mlpLearningRate)); 
    }

    /**
     * Resetir sve postavke na defoult  (osim API key).
     */
    public void resetToDefaults() {
        // Napomena: API key nije resetian
        trainTestRatio = 0.8;
        normalizationType = NormalizationType.MIN_MAX;
        extractMovingAverage = true;
        extractRSI = true;
        movingAveragePeriod = 20;
        rsiPeriod = 14;
        initialInvestment = 10000.0;
        transactionCost = 0.001;
        defaultSymbol = "AAPL";
        defaultHistoryDays = 365;
        showGridLines = true;
        animateCharts = false;
        quickModeDefault = true;
        randomForestTrees = 100;
        mlpHiddenLayers = 2;
        mlpLearningRate = 0.1;
    }
}
