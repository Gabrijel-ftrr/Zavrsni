package com.stockpredictor.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Contains the results of a backtesting session, including predictions,
 * error metrics over time, and simulated profit/loss.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class BacktestResult {
    
    private final String modelName;
    private final String symbol;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<PredictionResult> predictions;
    private final double totalProfit;
    private final double profitPercentage;
    private final double maxDrawdown;
    private final int winningTrades;
    private final int losingTrades;
    private final double averageError;
    private final double initialInvestment;

    /**
     * Constructs a BacktestResult with all backtest data.
     *
     * @param modelName        the name of the tested model
     * @param symbol           the stock symbol tested
     * @param startDate        start date of the backtest period
     * @param endDate          end date of the backtest period
     * @param predictions      list of all predictions made
     * @param totalProfit      total profit/loss in currency
     * @param profitPercentage percentage profit/loss
     * @param maxDrawdown      maximum drawdown during the period
     * @param winningTrades    number of winning trades
     * @param losingTrades     number of losing trades
     * @param averageError     average prediction error
     * @param initialInvestment initial investment amount
     */
    public BacktestResult(String modelName, String symbol, LocalDate startDate, 
                          LocalDate endDate, List<PredictionResult> predictions,
                          double totalProfit, double profitPercentage, double maxDrawdown,
                          int winningTrades, int losingTrades, double averageError,
                          double initialInvestment) {
        this.modelName = Objects.requireNonNull(modelName);
        this.symbol = Objects.requireNonNull(symbol);
        this.startDate = Objects.requireNonNull(startDate);
        this.endDate = Objects.requireNonNull(endDate);
        this.predictions = Collections.unmodifiableList(predictions);
        this.totalProfit = totalProfit;
        this.profitPercentage = profitPercentage;
        this.maxDrawdown = maxDrawdown;
        this.winningTrades = winningTrades;
        this.losingTrades = losingTrades;
        this.averageError = averageError;
        this.initialInvestment = initialInvestment;
    }

    /**
     * Calculates the win rate as a percentage.
     *
     * @return win rate percentage (0-100)
     */
    public double getWinRate() {
        int totalTrades = winningTrades + losingTrades;
        if (totalTrades == 0) {
            return 0.0;
        }
        return (double) winningTrades / totalTrades * 100;
    }

    /**
     * Returns a formatted summary of the backtest results.
     *
     * @return human-readable summary string
     */
    public String getSummary() {
        return String.format(
            "Backtest Results for %s using %s%n" +
            "Period: %s to %s%n" +
            "Initial Investment: $%.2f%n" +
            "Total Profit: $%.2f (%.2f%%)%n" +
            "Max Drawdown: %.2f%%%n" +
            "Win Rate: %.1f%% (%d/%d trades)%n" +
            "Average Prediction Error: %.4f",
            symbol, modelName, startDate, endDate, initialInvestment,
            totalProfit, profitPercentage, maxDrawdown,
            getWinRate(), winningTrades, winningTrades + losingTrades, averageError
        );
    }

    // Getters
    public String getModelName() { return modelName; }
    public String getSymbol() { return symbol; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public List<PredictionResult> getPredictions() { return predictions; }
    public double getTotalProfit() { return totalProfit; }
    public double getProfitPercentage() { return profitPercentage; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public int getWinningTrades() { return winningTrades; }
    public int getLosingTrades() { return losingTrades; }
    public double getAverageError() { return averageError; }
    public double getInitialInvestment() { return initialInvestment; }

    @Override
    public String toString() {
        return String.format("BacktestResult{model='%s', symbol='%s', profit=%.2f%%}",
                modelName, symbol, profitPercentage);
    }
}
