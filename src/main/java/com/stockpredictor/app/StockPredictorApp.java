package com.stockpredictor.app;

import com.stockpredictor.config.ServiceProvider;
import com.stockpredictor.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX klasa.
 * Inicijalizira aplikaciju.
 *
 * @author Gabrijel Matanović
 * @version 1.0.0
 */
public class StockPredictorApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(StockPredictorApp.class);
    private static final String APP_TITLE = "Predviđanje cijena dionica metodama strojnog učenja \u2013 Gabrijel Matanovi\u0107";
    private static final int MIN_WIDTH = 1200;
    private static final int MIN_HEIGHT = 800;

    private MainController mainController;

    /**
     * Ulazna točka.
     *
     * @param args Argumenti
     */
    public static void main(String[] args) {
        logger.info("Starting Stock Predictor application");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Initializing application...");
            
            // Inicijalizira servise
            ServiceProvider.getInstance();
            
            // Učitava main FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            mainController = loader.getController();
            
            // Stvara scenu
            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
            
            // Učitava CSS
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
            
            // Configure stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
            
            // Try to load application icon
            try {
                Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                logger.debug("Application icon not found, using default");
            }
            
            // Handle close request
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application closing...");
                shutdown();
            });
            
            primaryStage.show();
            logger.info("Application started successfully");
            
        } catch (IOException e) {
            logger.error("Pokretanje aplikacije nije uspjelo: {}", e.getMessage(), e);
            showErrorAndExit("U\u010ditavanje aplikacije nije uspjelo: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        logger.info("Application stop called");
        shutdown();
    }

    /**
     * Performs graceful shutdown of the application.
     */
    private void shutdown() {
        logger.info("Shutting down application...");
        
        try {
            if (mainController != null) {
                mainController.cleanup();
            }
            ServiceProvider.getInstance().shutdown();
        } catch (Exception e) {
            logger.error("Error during shutdown: {}", e.getMessage());
        }
        
        Platform.exit();
    }

    /**
     * Shows an error dialog and exits the application.
     *
     * @param message the error message
     */
    private void showErrorAndExit(String message) {
        logger.error("Fatal error: {}", message);
        
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Pogre\u0161ka aplikacije");
        alert.setHeaderText("Failed to start application");
        alert.setContentText(message);
        alert.showAndWait();
        
        Platform.exit();
        System.exit(1);
    }
}
