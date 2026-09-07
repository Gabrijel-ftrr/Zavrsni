package com.stockpredictor.controller;

import com.stockpredictor.config.AppConfig;
import com.stockpredictor.config.ServiceProvider;
import com.stockpredictor.model.*;
import com.stockpredictor.service.BacktestingService;
import com.stockpredictor.service.ChartService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller for the backtesting view.
 * Handles backtesting execution and results display.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class BacktestController implements BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(BacktestController.class);

    @FXML private TextField investmentField;
    @FXML private Button runBacktestButton;
    @FXML private Button compareModelsButton;
    @FXML private ProgressBar backtestProgress;
    @FXML private Label backtestStatusLabel;
    @FXML private BorderPane predictionChartContainer;
    @FXML private BorderPane errorChartContainer;
    @FXML private BorderPane profitChartContainer;
    @FXML private TextArea resultsArea;
    @FXML private VBox metricsBox;
    @FXML private Label profitLabel;
    @FXML private Label winRateLabel;
    @FXML private Label maxDrawdownLabel;
    @FXML private Label avgErrorLabel;

    private final BacktestingService backtestingService;
    private final ChartService chartService;
    private final AppConfig config;

    private TrainedModel currentModel;
    private List<StockData> testData;
    private Map<ModelType, TrainedModel> allModels;
    private BacktestResult lastResult;

    /**
     * Constructs the BacktestController with required services.
     */
    public BacktestController() {
        ServiceProvider provider = ServiceProvider.getInstance();
        this.backtestingService = provider.getBacktestingService();
        this.chartService = provider.getChartService();
        this.config = AppConfig.getInstance();
    }

    @Override
    @FXML
    public void initialize() {
        logger.info("Initializing Backtest controller");
        
        setupEventHandlers();
        setDefaultValues();
        hideProgress();
    }

    private void setupEventHandlers() {
        runBacktestButton.setOnAction(e -> runBacktest());
        compareModelsButton.setOnAction(e -> compareAllModels());

        // Validate investment input
        investmentField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                investmentField.setText(oldVal);
            }
        });
    }

    private void setDefaultValues() {
        investmentField.setText(String.format("%.2f", config.getInitialInvestment()));
    }

    /**
     * Sets the model and test data for backtesting.
     *
     * @param model    the trained model to backtest
     * @param testData the test data
     */
    public void setModelAndData(TrainedModel model, List<StockData> testData) {
        this.currentModel = model;
        this.testData = testData;
        
        if (model != null && testData != null && !testData.isEmpty()) {
            backtestStatusLabel.setText("Spremno za simulaciju: " + model.getModelType().getDisplayName() +
                                        " \u2013 zapisa: " + testData.size());
            runBacktestButton.setDisable(false);
        } else {
            backtestStatusLabel.setText("Select and train a model first");
            runBacktestButton.setDisable(true);
        }
    }

    /**
     * Sets all trained models for comparison.
     *
     * @param models map of trained models
     */
    public void setAllModels(Map<ModelType, TrainedModel> models) {
        this.allModels = models;
        compareModelsButton.setDisable(models == null || models.isEmpty());
    }

    /**
     * Runs a backtest with the current model.
     */
    @FXML
    public void runBacktest() {
        if (currentModel == null || testData == null) {
            showStatus("Nema dostupnog modela ili podataka", true);
            return;
        }

        double investment = parseInvestment();
        if (investment <= 0) {
            showStatus("Neispravan iznos ulaganja", true);
            return;
        }

        showProgress();
        backtestStatusLabel.setText("Simulacija u tijeku...");

        Task<BacktestResult> backtestTask = new Task<>() {
            @Override
            protected BacktestResult call() {
                return backtestingService.runBacktest(currentModel, testData, investment);
            }
        };

        backtestTask.setOnSucceeded(e -> {
            lastResult = backtestTask.getValue();
            displayResults(lastResult);
            hideProgress();
            showStatus("Simulacija dovršena", false);
        });

        backtestTask.setOnFailed(e -> {
            logger.error("Backtest failed: {}", backtestTask.getException().getMessage());
            hideProgress();
            showStatus("Simulacija nije uspjela: " + backtestTask.getException().getMessage(), true);
        });

        new Thread(backtestTask).start();
    }

    /**
     * Compares all trained models through backtesting.
     */
    @FXML
    public void compareAllModels() {
        if (allModels == null || allModels.isEmpty() || testData == null) {
            showStatus("Nema modela za usporedbu", true);
            return;
        }

        double investment = parseInvestment();
        if (investment <= 0) {
            showStatus("Neispravan iznos ulaganja", true);
            return;
        }

        showProgress();
        backtestStatusLabel.setText("Uspoređivanje modela...");

        Task<List<BacktestResult>> compareTask = new Task<>() {
            @Override
            protected List<BacktestResult> call() {
                return backtestingService.compareModels(allModels, testData);
            }
        };

        compareTask.setOnSucceeded(e -> {
            List<BacktestResult> results = compareTask.getValue();
            displayComparisonResults(results);
            hideProgress();
            showStatus("Usporedba dovršena", false);
        });

        compareTask.setOnFailed(e -> {
            logger.error("Comparison failed: {}", compareTask.getException().getMessage());
            hideProgress();
            showStatus("Usporedba nije uspjela: " + compareTask.getException().getMessage(), true);
        });

        new Thread(compareTask).start();
    }

    private void displayResults(BacktestResult result) {
        // Update metrics labels
        Platform.runLater(() -> {
            profitLabel.setText(String.format("$%.2f (%.2f%%)", 
                result.getTotalProfit(), result.getProfitPercentage()));
            profitLabel.setStyle(result.getTotalProfit() >= 0 ? 
                "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
            
            winRateLabel.setText(String.format("%.1f%%", result.getWinRate()));
            maxDrawdownLabel.setText(String.format("%.2f%%", result.getMaxDrawdown()));
            avgErrorLabel.setText(String.format("%.4f", result.getAverageError()));
            
            metricsBox.setVisible(true);
        });

        // Create prediction vs actual chart
        LineChart<Number, Number> predChart = chartService.createPredictionChart(
            result.getPredictions(), "Predviđena i stvarna cijena"
        );
        chartService.applyDefaultStyle(predChart);
        Platform.runLater(() -> predictionChartContainer.setCenter(predChart));

        // Create error chart
        LineChart<Number, Number> errorChart = chartService.createErrorChart(result);
        chartService.applyDefaultStyle(errorChart);
        Platform.runLater(() -> errorChartContainer.setCenter(errorChart));

        // Create profit chart
        LineChart<Number, Number> profitChart = chartService.createProfitChart(
            result, result.getInitialInvestment()
        );
        chartService.applyDefaultStyle(profitChart);
        Platform.runLater(() -> profitChartContainer.setCenter(profitChart));

        // Update results text area
        Platform.runLater(() -> resultsArea.setText(result.getSummary()));
    }

    private void displayComparisonResults(List<BacktestResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("MODEL COMPARISON RESULTS\n");
        sb.append("========================\n\n");

        // Calculate buy and hold for comparison
        double buyAndHold = backtestingService.calculateBuyAndHold(
            testData, parseInvestment()
        );
        sb.append(String.format("Strategija drži i čekaj: %.2f%%\n\n", buyAndHold));

        int rank = 1;
        for (BacktestResult result : results) {
            sb.append(String.format("#%d - %s\n", rank++, result.getModelName()));
            sb.append(String.format("   Profit: $%.2f (%.2f%%)\n", 
                result.getTotalProfit(), result.getProfitPercentage()));
            sb.append(String.format("   Win Rate: %.1f%%\n", result.getWinRate()));
            sb.append(String.format("   Max Drawdown: %.2f%%\n", result.getMaxDrawdown()));
            sb.append(String.format("   Avg Error: %.4f\n\n", result.getAverageError()));
        }

        if (!results.isEmpty()) {
            BacktestResult best = results.get(0);
            sb.append("RECOMMENDATION\n");
            sb.append("--------------\n");
            sb.append(String.format("Najuspješniji model: %s\n", best.getModelName()));
            sb.append(String.format("Bolje od strategije drži i čekaj za: %.2f%%\n", 
                best.getProfitPercentage() - buyAndHold));
            
            lastResult = best;
            displayResults(best);
        }

        Platform.runLater(() -> resultsArea.setText(sb.toString()));
    }

    /**
     * Gets the last backtest result.
     *
     * @return the last result
     */
    public BacktestResult getLastResult() {
        return lastResult;
    }

    private double parseInvestment() {
        try {
            return Double.parseDouble(investmentField.getText());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void showStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            backtestStatusLabel.setText(message);
            backtestStatusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    private void showProgress() {
        Platform.runLater(() -> {
            backtestProgress.setVisible(true);
            backtestProgress.setProgress(-1);
            runBacktestButton.setDisable(true);
            compareModelsButton.setDisable(true);
        });
    }

    private void hideProgress() {
        Platform.runLater(() -> {
            backtestProgress.setVisible(false);
            runBacktestButton.setDisable(currentModel == null);
            compareModelsButton.setDisable(allModels == null || allModels.isEmpty());
        });
    }

    @Override
    public void refresh() {
        if (lastResult != null) {
            displayResults(lastResult);
        }
    }

    @Override
    public void cleanup() {
        lastResult = null;
        currentModel = null;
        testData = null;
    }
}
