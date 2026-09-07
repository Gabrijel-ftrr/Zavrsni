package com.stockpredictor.controller;

/**
 * Base interface for all controllers in the application.
 * Defines common lifecycle methods.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public interface BaseController {

    /**
     * Initializes the controller after FXML loading.
     * Called automatically by JavaFX FXMLLoader.
     */
    void initialize();

    /**
     * Refreshes the controller's view with current data.
     */
    void refresh();

    /**
     * Cleans up resources before the controller is destroyed.
     */
    default void cleanup() {
        // Default implementation - override if needed
    }
}
