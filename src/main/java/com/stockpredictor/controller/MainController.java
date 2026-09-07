package com.stockpredictor.controller;

import com.stockpredictor.config.ServiceProvider;
import com.stockpredictor.model.BacktestResult;
import com.stockpredictor.model.DataSet;
import com.stockpredictor.model.TrainedModel;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main controller that coordinates all tab controllers.
 * Handles data flow between different views.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class MainController implements BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private TabPane mainTabPane;
    @FXML private Tab dashboardTab;
    @FXML private Tab modelTab;
    @FXML private Tab backtestTab;
    @FXML private Tab exportTab;

    @FXML private DashboardController dashboardController;
    @FXML private ModelController modelController;
    @FXML private BacktestController backtestController;
    @FXML private ExportController exportController;

    /**
     * Constructs the MainController.
     */
    public MainController() {
        // Default constructor
    }

    @Override
    @FXML
    public void initialize() {
        logger.info("Initializing Main controller");
        
        setupTabListeners();
        setupDataFlow();
    }

    private void setupTabListeners() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == modelTab) {
                prepareModelTab();
            } else if (newTab == backtestTab) {
                prepareBacktestTab();
            } else if (newTab == exportTab) {
                prepareExportTab();
            }
        });
    }

    private void setupDataFlow() {
        // When a model is selected in ModelController, update BacktestController
        if (modelController != null) {
            modelController.setOnModelSelected(this::onModelSelected);
        }
    }

    private void prepareModelTab() {
        if (dashboardController == null || modelController == null) {
            return;
        }

        var data = dashboardController.getCurrentData();
        String symbol = dashboardController.getCurrentSymbol();
        
        if (data != null && !data.isEmpty()) {
            modelController.setStockData(data, symbol);
        }
    }

    private void prepareBacktestTab() {
        if (modelController == null || backtestController == null) {
            return;
        }

        TrainedModel model = modelController.getBestModel();
        DataSet dataSet = modelController.getCurrentDataSet();
        
        if (model != null && dataSet != null) {
            backtestController.setModelAndData(model, dataSet.getTestData());
            backtestController.setAllModels(modelController.getTrainedModels());
        }
    }

    private void prepareExportTab() {
        if (exportController == null) {
            return;
        }

        var stockData = dashboardController != null ? dashboardController.getCurrentData() : null;
        String symbol = dashboardController != null ? dashboardController.getCurrentSymbol() : null;
        
        BacktestResult result = backtestController != null ? backtestController.getLastResult() : null;
        var predictions = result != null ? result.getPredictions() : null;
        
        TrainedModel model = modelController != null ? modelController.getBestModel() : null;
        
        exportController.setExportData(stockData, predictions, symbol, model);
    }

    private void onModelSelected(TrainedModel model) {
        logger.debug("Model selected: {}", model.getModelType().getDisplayName());
        
        // Auto-prepare backtest tab when model is selected
        DataSet dataSet = modelController.getCurrentDataSet();
        if (dataSet != null) {
            backtestController.setModelAndData(model, dataSet.getTestData());
            backtestController.setAllModels(modelController.getTrainedModels());
        }
    }

    /**
     * Switches to the specified tab.
     *
     * @param tabIndex the tab index (0-3)
     */
    public void switchToTab(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < mainTabPane.getTabs().size()) {
            mainTabPane.getSelectionModel().select(tabIndex);
        }
    }

    /**
     * Gets the dashboard controller.
     *
     * @return the dashboard controller
     */
    public DashboardController getDashboardController() {
        return dashboardController;
    }

    /**
     * Gets the model controller.
     *
     * @return the model controller
     */
    public ModelController getModelController() {
        return modelController;
    }

    /**
     * Gets the backtest controller.
     *
     * @return the backtest controller
     */
    public BacktestController getBacktestController() {
        return backtestController;
    }

    /**
     * Gets the export controller.
     *
     * @return the export controller
     */
    public ExportController getExportController() {
        return exportController;
    }

    @Override
    public void refresh() {
        if (dashboardController != null) dashboardController.refresh();
        if (modelController != null) modelController.refresh();
        if (backtestController != null) backtestController.refresh();
        if (exportController != null) exportController.refresh();
    }

    /**
     * Prikazuje podatke o aplikaciji i autoru.
     */
    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("O aplikaciji");
        alert.setHeaderText("Predviđanje cijena dionica metodama strojnog učenja");
        alert.setContentText(
            "Autor: Gabrijel Matanović\n\n" +
            "Studij: Elektroničko poslovanje i programsko inženjerstvo\n\n" +
            "Fakultet turizma i ruralnog razvoja u Požegi\n" +
            "Sveučilište Josipa Jurja Strossmayera u Osijeku\n\n" +
            "Aplikacija je izrađena u sklopu završnog rada.\n" +
            "Tehnologije: Java 17, JavaFX 21, Weka 3.8.6, Alpha Vantage API.\n\n" +
            "Napomena: predikcije su namijenjene isključivo obrazovnoj svrsi\n" +
            "i ne predstavljaju savjet za ulaganje."
        );
        alert.getDialogPane().setMinWidth(460);
        alert.showAndWait();
    }

    @Override
    public void cleanup() {
        ServiceProvider.getInstance().shutdown();
        
        if (dashboardController != null) dashboardController.cleanup();
        if (modelController != null) modelController.cleanup();
        if (backtestController != null) backtestController.cleanup();
        if (exportController != null) exportController.cleanup();
    }
}
