package com.stockpredictor.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a prediction result from a machine learning model.
 * Contains both the predicted value and the actual value (if available) for comparison.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class PredictionResult {
    
    private final LocalDate date;
    private final double predictedValue;
    private final Double actualValue;
    private final String modelName;
    private final LocalDateTime predictionTimestamp;

    /**
     * Constructs a prediction result with both predicted and actual values.
     *
     * @param date           the date for which the prediction was made
     * @param predictedValue the predicted stock price
     * @param actualValue    the actual stock price (null if not yet known)
     * @param modelName      the name of the model that made the prediction
     */
    public PredictionResult(LocalDate date, double predictedValue, 
                           Double actualValue, String modelName) {
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        this.predictedValue = predictedValue;
        this.actualValue = actualValue;
        this.modelName = Objects.requireNonNull(modelName, "Model name cannot be null");
        this.predictionTimestamp = LocalDateTime.now();
    }

    /**
     * Calculates the prediction error if actual value is available.
     *
     * @return the absolute error, or null if actual value is not available
     */
    public Double getError() {
        if (actualValue == null) {
            return null;
        }
        return Math.abs(predictedValue - actualValue);
    }

    /**
     * Calculates the percentage error if actual value is available.
     *
     * @return the percentage error, or null if actual value is not available
     */
    public Double getPercentageError() {
        if (actualValue == null || actualValue == 0) {
            return null;
        }
        return (Math.abs(predictedValue - actualValue) / actualValue) * 100;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public double getPredictedValue() { return predictedValue; }
    public Double getActualValue() { return actualValue; }
    public String getModelName() { return modelName; }
    public LocalDateTime getPredictionTimestamp() { return predictionTimestamp; }

    @Override
    public String toString() {
        return String.format("PredictionResult{date=%s, predicted=%.2f, actual=%s, model='%s'}",
                date, predictedValue, 
                actualValue != null ? String.format("%.2f", actualValue) : "N/A",
                modelName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PredictionResult that = (PredictionResult) o;
        return Double.compare(that.predictedValue, predictedValue) == 0 &&
               date.equals(that.date) &&
               modelName.equals(that.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, predictedValue, modelName);
    }
}
