package com.stockpredictor.model;

/**
 * Enumeration of data normalization methods.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public enum NormalizationType {
    
    /**
     * No normalization applied.
     */
    NONE("Bez normalizacije", "Raw data without normalization"),
    
    /**
     * Min-Max scaling to [0, 1] range.
     */
    MIN_MAX("Min-Max Scaling", "Scales data to [0, 1] range"),
    
    /**
     * Standard scaling (z-score normalization).
     */
    STANDARD("Standard Scaling", "Scales data to zero mean and unit variance");

    private final String displayName;
    private final String description;

    NormalizationType(String displayName, String description) {
        this.displayName = displayName;
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
     * Returns the description of this normalization method.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
