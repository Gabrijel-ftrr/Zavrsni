package com.stockpredictor.model;

/**
 * Enumeration of available machine learning model types for stock prediction.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public enum ModelType {
    
    /**
     * Linear Regression - Simple, fast, interpretable baseline model.
     */
    LINEAR_REGRESSION("Linearna regresija", true, "Simple linear model for baseline predictions"),
    
    /**
     * Random Forest - Ensemble method with good accuracy and robustness.
     */
    RANDOM_FOREST("Slučajna šuma", false, "Ensemble tree-based model for complex patterns"),
    
    /**
     * Multilayer Perceptron - Neural network for capturing non-linear relationships.
     */
    MLP("Višeslojni perceptron", false, "Neural network for non-linear relationships");

    private final String displayName;
    private final boolean lightweight;
    private final String description;

    ModelType(String displayName, boolean lightweight, String description) {
        this.displayName = displayName;
        this.lightweight = lightweight;
        this.description = description;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Indicates if this is a lightweight model suitable for quick mode.
     *
     * @return true if lightweight
     */
    public boolean isLightweight() {
        return lightweight;
    }

    /**
     * Returns the model description.
     *
     * @return description of the model
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
