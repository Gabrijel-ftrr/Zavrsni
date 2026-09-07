package com.stockpredictor.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Holds evaluation metrics for a trained machine learning model.
 * Includes RMSE, MAE, R² score, and training metadata.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class ModelMetrics {
    
    private final String modelName;
    private final double rmse;
    private final double mae;
    private final double rSquared;
    private final long trainingTimeMs;
    private final int trainingDataSize;
    private final int testDataSize;
    private final LocalDateTime evaluationTime;

    /**
     * Constructs a ModelMetrics instance with all evaluation data.
     *
     * @param modelName       the name of the evaluated model
     * @param rmse            Root Mean Square Error
     * @param mae             Mean Absolute Error
     * @param rSquared        R² (coefficient of determination)
     * @param trainingTimeMs  training time in milliseconds
     * @param trainingDataSize size of training dataset
     * @param testDataSize    size of test dataset
     */
    public ModelMetrics(String modelName, double rmse, double mae, double rSquared,
                        long trainingTimeMs, int trainingDataSize, int testDataSize) {
        this.modelName = Objects.requireNonNull(modelName, "Model name cannot be null");
        this.rmse = rmse;
        this.mae = mae;
        this.rSquared = rSquared;
        this.trainingTimeMs = trainingTimeMs;
        this.trainingDataSize = trainingDataSize;
        this.testDataSize = testDataSize;
        this.evaluationTime = LocalDateTime.now();
    }

    /**
     * Compares this model's performance to another based on RMSE (lower is better).
     *
     * @param other the other ModelMetrics to compare
     * @return negative if this model is better, positive if worse, 0 if equal
     */
    public int compareTo(ModelMetrics other) {
        return Double.compare(this.rmse, other.rmse);
    }

    /**
     * Checks if this model outperforms another based on RMSE.
     *
     * @param other the model to compare against
     * @return true if this model has lower RMSE
     */
    public boolean isBetterThan(ModelMetrics other) {
        return this.rmse < other.rmse;
    }

    /**
     * Returns a formatted summary of the metrics.
     *
     * @return a human-readable summary string
     */
    public String getSummary() {
        return String.format(
            "Model: %s%n" +
            "  RMSE: %.4f%n" +
            "  MAE: %.4f%n" +
            "  R²: %.4f%n" +
            "  Training Time: %d ms%n" +
            "  Train/Test Size: %d/%d",
            modelName, rmse, mae, rSquared, trainingTimeMs, trainingDataSize, testDataSize
        );
    }

    // Getters
    public String getModelName() { return modelName; }
    public double getRmse() { return rmse; }
    public double getMae() { return mae; }
    public double getRSquared() { return rSquared; }
    public long getTrainingTimeMs() { return trainingTimeMs; }
    public int getTrainingDataSize() { return trainingDataSize; }
    public int getTestDataSize() { return testDataSize; }
    public LocalDateTime getEvaluationTime() { return evaluationTime; }

    @Override
    public String toString() {
        return String.format("ModelMetrics{model='%s', RMSE=%.4f, MAE=%.4f, R²=%.4f}",
                modelName, rmse, mae, rSquared);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelMetrics that = (ModelMetrics) o;
        return Double.compare(that.rmse, rmse) == 0 &&
               Double.compare(that.mae, mae) == 0 &&
               modelName.equals(that.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelName, rmse, mae);
    }
}
