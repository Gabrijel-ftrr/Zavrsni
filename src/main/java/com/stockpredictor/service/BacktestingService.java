package com.stockpredictor.service;

import com.stockpredictor.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for backtesting trading strategies based on model predictions.
 * Simulates trading and calculates performance metrics.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class BacktestingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BacktestingService.class);
    private static final double DEFAULT_INITIAL_INVESTMENT = 10000.0;
    private static final double TRANSACTION_COST = 0.001; // 0.1% per trade

    private final ModelTrainerService modelTrainerService;

    /**
     * Constructs a BacktestingService.
     *
     * @param modelTrainerService the model trainer service for predictions
     */
    public BacktestingService(ModelTrainerService modelTrainerService) {
        this.modelTrainerService = modelTrainerService;
    }

    /**
     * Runs a backtest on historical data using the specified model.
     *
     * @param model     the trained model to use
     * @param testData  the test data for backtesting
     * @param initialInvestment starting capital
     * @return the backtest results
     */
    public BacktestResult runBacktest(TrainedModel model, List<StockData> testData,
                                       double initialInvestment) {
        logger.info("Running backtest for {} with {} data points", 
                   model.getModelType().getDisplayName(), testData.size());

        List<PredictionResult> predictions = new ArrayList<>();
        List<Double> portfolioValues = new ArrayList<>();
        
        double cash = initialInvestment;
        double shares = 0;
        double maxPortfolioValue = initialInvestment;
        double maxDrawdown = 0;
        int winningTrades = 0;
        int losingTrades = 0;
        double totalError = 0;
        int lookbackWindow = 5;

        portfolioValues.add(initialInvestment);

        for (int i = lookbackWindow; i < testData.size(); i++) {
            // Get historical window for prediction
            List<StockData> window = testData.subList(i - lookbackWindow, i);
            StockData actual = testData.get(i);

            double predictedPrice;
            try {
                predictedPrice = modelTrainerService.predict(model, window);
            } catch (Exception e) {
                logger.warn("Prediction failed at index {}: {}", i, e.getMessage());
                continue;
            }

            double actualPrice = actual.getClose();
            double previousPrice = testData.get(i - 1).getClose();

            // Create prediction result
            PredictionResult prediction = new PredictionResult(
                actual.getDate(), predictedPrice, actualPrice, 
                model.getModelType().getDisplayName()
            );
            predictions.add(prediction);
            totalError += Math.abs(predictedPrice - actualPrice);

            // Trading logic: Buy if predicted to go up, sell if predicted to go down
            TradeResult trade = executeTradeLogic(
                predictedPrice, previousPrice, actualPrice,
                cash, shares, TRANSACTION_COST
            );

            cash = trade.cash;
            shares = trade.shares;

            if (trade.isWinningTrade) {
                winningTrades++;
            } else if (trade.isLosingTrade) {
                losingTrades++;
            }

            // Track portfolio value
            double portfolioValue = cash + shares * actualPrice;
            portfolioValues.add(portfolioValue);

            // Track max drawdown
            maxPortfolioValue = Math.max(maxPortfolioValue, portfolioValue);
            double drawdown = (maxPortfolioValue - portfolioValue) / maxPortfolioValue * 100;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
        }

        // Final portfolio value
        double finalValue = cash + shares * testData.get(testData.size() - 1).getClose();
        double totalProfit = finalValue - initialInvestment;
        double profitPercentage = (totalProfit / initialInvestment) * 100;
        double averageError = predictions.isEmpty() ? 0 : totalError / predictions.size();

        LocalDate startDate = testData.get(lookbackWindow).getDate();
        LocalDate endDate = testData.get(testData.size() - 1).getDate();

        BacktestResult result = new BacktestResult(
            model.getModelType().getDisplayName(),
            model.getSymbol(),
            startDate, endDate,
            predictions,
            totalProfit, profitPercentage, maxDrawdown,
            winningTrades, losingTrades, averageError,
            initialInvestment
        );

        logger.info("Backtest complete: {} profit ({:.2f}%)", 
                   totalProfit >= 0 ? "+" + totalProfit : totalProfit, profitPercentage);

        return result;
    }

    /**
     * Runs a backtest with default initial investment.
     *
     * @param model    the trained model
     * @param testData the test data
     * @return the backtest results
     */
    public BacktestResult runBacktest(TrainedModel model, List<StockData> testData) {
        return runBacktest(model, testData, DEFAULT_INITIAL_INVESTMENT);
    }

    /**
     * Compares multiple models through backtesting.
     *
     * @param models   map of trained models
     * @param testData the test data
     * @return list of backtest results for each model
     */
    public List<BacktestResult> compareModels(java.util.Map<ModelType, TrainedModel> models,
                                               List<StockData> testData) {
        List<BacktestResult> results = new ArrayList<>();
        
        for (TrainedModel model : models.values()) {
            BacktestResult result = runBacktest(model, testData);
            results.add(result);
        }

        // Sort by profit
        results.sort((a, b) -> Double.compare(b.getProfitPercentage(), a.getProfitPercentage()));
        
        return results;
    }

    /**
     * Calculates buy-and-hold strategy performance for comparison.
     *
     * @param testData          the test data
     * @param initialInvestment starting capital
     * @return the profit percentage from buy-and-hold
     */
    public double calculateBuyAndHold(List<StockData> testData, double initialInvestment) {
        if (testData.isEmpty()) {
            return 0;
        }

        double startPrice = testData.get(0).getClose();
        double endPrice = testData.get(testData.size() - 1).getClose();
        double shares = initialInvestment / startPrice;
        double finalValue = shares * endPrice;

        return ((finalValue - initialInvestment) / initialInvestment) * 100;
    }

    /**
     * Executes trading logic based on prediction.
     */
    private TradeResult executeTradeLogic(double predictedPrice, double previousPrice,
                                           double actualPrice, double cash, double shares,
                                           double transactionCost) {
        boolean isWinning = false;
        boolean isLosing = false;

        // Predict price will go up - buy if we have cash
        if (predictedPrice > previousPrice * 1.001 && cash > 0) {
            double cost = cash * (1 + transactionCost);
            shares = cash / actualPrice;
            cash = 0;
            
            // Check if prediction was correct
            isWinning = actualPrice > previousPrice;
            isLosing = actualPrice < previousPrice;
        }
        // Predict price will go down - sell if we have shares
        else if (predictedPrice < previousPrice * 0.999 && shares > 0) {
            cash = shares * actualPrice * (1 - transactionCost);
            
            // Check if prediction was correct
            isWinning = actualPrice < previousPrice;
            isLosing = actualPrice > previousPrice;
            shares = 0;
        }

        return new TradeResult(cash, shares, isWinning, isLosing);
    }

    /**
     * Helper record for trade execution results.
     */
    private record TradeResult(double cash, double shares, 
                               boolean isWinningTrade, boolean isLosingTrade) {}

    /**
     * Calculates rolling error metrics over time.
     *
     * @param predictions list of predictions
     * @param windowSize  rolling window size
     * @return list of rolling RMSE values
     */
    public List<Double> calculateRollingError(List<PredictionResult> predictions, 
                                               int windowSize) {
        List<Double> rollingErrors = new ArrayList<>();

        for (int i = windowSize; i <= predictions.size(); i++) {
            List<PredictionResult> window = predictions.subList(i - windowSize, i);
            double sumSquaredError = 0;
            int count = 0;

            for (PredictionResult p : window) {
                Double error = p.getError();
                if (error != null) {
                    sumSquaredError += error * error;
                    count++;
                }
            }

            if (count > 0) {
                rollingErrors.add(Math.sqrt(sumSquaredError / count));
            }
        }

        return rollingErrors;
    }
}
