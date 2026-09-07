package com.stockpredictor.controller;

import com.stockpredictor.config.AppConfig;
import com.stockpredictor.config.ServiceProvider;
import com.stockpredictor.model.StockData;
import com.stockpredictor.service.AlphaVantageDataService;
import com.stockpredictor.service.ChartService;
import com.stockpredictor.util.FeatureExtractor;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for the main dashboard view.
 * Handles stock symbol input and price chart display.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class DashboardController implements BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private TextField symbolField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button fetchButton;
    @FXML private Button importButton;
    @FXML private CheckBox showMACheckbox;
    @FXML private CheckBox showRSICheckbox;
    @FXML private BorderPane chartContainer;
    @FXML private VBox rsiChartContainer;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label stockInfoLabel;

    private final AlphaVantageDataService dataService;
    private final ChartService chartService;
    private final AppConfig config;
    
    private List<StockData> currentData;

    /**
     * Constructs the DashboardController with required services.
     */
    public DashboardController() {
        ServiceProvider provider = ServiceProvider.getInstance();
        this.dataService = provider.getAlphaVantageDataService();
        this.chartService = provider.getChartService();
        this.config = AppConfig.getInstance();
    }

    @Override
    @FXML
    public void initialize() {
        logger.info("Initializing Dashboard controller");
        
        setupDatePickers();
        setupEventHandlers();
        setDefaultValues();
        hideLoading();
    }

    private void setupDatePickers() {
        LocalDate today = LocalDate.now();
        endDatePicker.setValue(today);
        startDatePicker.setValue(today.minusMonths(6));
    }

    private void setupEventHandlers() {
        fetchButton.setOnAction(e -> fetchData());
        importButton.setOnAction(e -> importFromCsv());
        
        showMACheckbox.setOnAction(e -> updateChart());
        showRSICheckbox.setOnAction(e -> updateChart());

        symbolField.setOnAction(e -> fetchData());
    }

    private void setDefaultValues() {
        symbolField.setText(config.getDefaultSymbol());
        showMACheckbox.setSelected(config.isExtractMovingAverage());
        showRSICheckbox.setSelected(config.isExtractRSI());
    }

    /**
     * Fetches stock data from Alpha Vantage.
     */
    @FXML
    public void fetchData() {
        String symbol = symbolField.getText().trim().toUpperCase();
        
        if (symbol.isEmpty()) {
            showStatus("Unesite oznaku dionice", true);
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            showStatus("Neispravan raspon datuma", true);
            return;
        }

        showLoading();
        
        Task<List<StockData>> fetchTask = new Task<>() {
            @Override
            protected List<StockData> call() throws Exception {
                return dataService.fetchHistoricalData(symbol, startDate, endDate);
            }
        };

        fetchTask.setOnSucceeded(e -> {
            currentData = fetchTask.getValue();
            
            if (currentData.isEmpty()) {
                showStatus("Nema podataka za " + symbol, true);
                hideLoading();
                return;
            }

            // Add features
            currentData = FeatureExtractor.extractFeatures(
                currentData, 
                showMACheckbox.isSelected(), 
                showRSICheckbox.isSelected()
            );

            updateStockInfo(symbol);
            updateChart();
            showStatus("Učitano: " + currentData.size() + " zapisa za " + symbol, false);
            hideLoading();
        });

        fetchTask.setOnFailed(e -> {
            logger.error("Failed to fetch data: {}", fetchTask.getException().getMessage());
            showStatus("Error: " + fetchTask.getException().getMessage(), true);
            hideLoading();
        });

        new Thread(fetchTask).start();
    }

    /**
     * Uvozi povijesne podatke o dionici iz CSV datoteke.
     * Koristi se kada besplatna razina mrežnog servisa ne omogućuje
     * dohvat dovoljno dugoga razdoblja.
     */
    private void importFromCsv() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Odaberite CSV datoteku s podatcima o dionici");
        chooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("CSV datoteke", "*.csv"));

        java.io.File file = chooser.showOpenDialog(fetchButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        String symbol = symbolField.getText().trim().toUpperCase();
        if (symbol.isEmpty()) {
            symbol = file.getName().replaceAll("(?i)\\.csv$", "").toUpperCase();
            symbolField.setText(symbol);
        }

        try {
            currentData = ServiceProvider.getInstance()
                    .getCsvService()
                    .importStockData(file.toPath(), symbol);

            if (currentData == null || currentData.isEmpty()) {
                showStatus("Datoteka ne sadrži upotrebljive podatke", true);
                return;
            }

            currentData = FeatureExtractor.extractFeatures(
                currentData,
                showMACheckbox.isSelected(),
                showRSICheckbox.isSelected()
            );

            updateStockInfo(symbol);
            updateChart();
            showStatus("Uvezeno: " + currentData.size() + " zapisa za " + symbol, false);

        } catch (Exception ex) {
            logger.error("CSV import failed: {}", ex.getMessage());
            showStatus("Uvoz nije uspio: " + ex.getMessage(), true);
        }
    }

    private void updateStockInfo(String symbol) {
        if (currentData == null || currentData.isEmpty()) {
            return;
        }

        StockData latest = currentData.get(currentData.size() - 1);
        StockData first = currentData.get(0);
        
        double priceChange = latest.getClose() - first.getClose();
        double percentChange = (priceChange / first.getClose()) * 100;
        String changeStr = String.format("%+.2f (%+.2f%%)", priceChange, percentChange);
        
        String stockName = dataService.getStockName(symbol);
        String displayName = stockName != null ? stockName : symbol;

        String info = String.format("%s | Latest: $%.2f | Change: %s | Range: $%.2f - $%.2f",
            displayName, latest.getClose(), changeStr,
            currentData.stream().mapToDouble(StockData::getLow).min().orElse(0),
            currentData.stream().mapToDouble(StockData::getHigh).max().orElse(0)
        );

        Platform.runLater(() -> stockInfoLabel.setText(info));
    }

    private void updateChart() {
        if (currentData == null || currentData.isEmpty()) {
            return;
        }

        String symbol = symbolField.getText().trim().toUpperCase();
        boolean showMA = showMACheckbox.isSelected();
        boolean showRSI = showRSICheckbox.isSelected();

        // Recalculate features if needed
        currentData = FeatureExtractor.extractFeatures(currentData, showMA, showRSI);

        // Create main price chart
        LineChart<Number, Number> priceChart = chartService.createChartWithIndicators(
            currentData, showMA, symbol + " Stock Price"
        );
        chartService.applyDefaultStyle(priceChart);
        
        Platform.runLater(() -> chartContainer.setCenter(priceChart));

        // Create RSI chart if enabled
        if (showRSI && rsiChartContainer != null) {
            LineChart<Number, Number> rsiChart = chartService.createRSIChart(
                currentData, "Indeks relativne snage"
            );
            chartService.applyDefaultStyle(rsiChart);
            
            Platform.runLater(() -> {
                rsiChartContainer.getChildren().clear();
                rsiChartContainer.getChildren().add(rsiChart);
            });
        } else if (rsiChartContainer != null) {
            Platform.runLater(() -> rsiChartContainer.getChildren().clear());
        }
    }

    /**
     * Gets the currently loaded stock data.
     *
     * @return list of stock data
     */
    public List<StockData> getCurrentData() {
        return currentData;
    }

    /**
     * Gets the current stock symbol.
     *
     * @return the stock symbol
     */
    public String getCurrentSymbol() {
        return symbolField.getText().trim().toUpperCase();
    }

    private void showStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    private void showLoading() {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(true);
            fetchButton.setDisable(true);
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            fetchButton.setDisable(false);
        });
    }

    @Override
    public void refresh() {
        if (currentData != null && !currentData.isEmpty()) {
            updateChart();
        }
    }

    @Override
    public void cleanup() {
        currentData = null;
    }
}
