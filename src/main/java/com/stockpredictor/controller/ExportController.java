package com.stockpredictor.controller;

import com.stockpredictor.config.AppConfig;
import com.stockpredictor.config.ServiceProvider;
import com.stockpredictor.model.*;
import com.stockpredictor.service.AlphaVantageDataService;
import com.stockpredictor.service.CSVService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for export and settings view.
 * Handles data export and application configuration.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class ExportController implements BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
    private static final DateTimeFormatter FILE_DATE_FORMAT = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Export section
    @FXML private Button exportDataButton;
    @FXML private Button exportPredictionsButton;
    @FXML private Button exportCombinedButton;
    @FXML private TextField exportPathField;
    @FXML private Button browseButton;
    @FXML private Label exportStatusLabel;

    // Settings section
    @FXML private TextField apiKeyField;
    @FXML private Button saveApiKeyButton;
    @FXML private ComboBox<NormalizationType> defaultNormCombo;
    @FXML private Slider defaultTrainRatioSlider;
    @FXML private Label trainRatioLabel;
    @FXML private TextField defaultInvestmentField;
    @FXML private CheckBox defaultMACheckbox;
    @FXML private CheckBox defaultRSICheckbox;
    @FXML private Button saveSettingsButton;
    @FXML private Button resetSettingsButton;
    @FXML private Label settingsStatusLabel;

    private final CSVService csvService;
    private final AlphaVantageDataService dataService;
    private final AppConfig config;

    private List<StockData> stockData;
    private List<PredictionResult> predictions;
    private String symbol;
    private TrainedModel currentModel;

    /**
     * Constructs the ExportController with required services.
     */
    public ExportController() {
        ServiceProvider provider = ServiceProvider.getInstance();
        this.csvService = provider.getCsvService();
        this.dataService = provider.getAlphaVantageDataService();
        this.config = AppConfig.getInstance();
    }

    @Override
    @FXML
    public void initialize() {
        logger.info("Initializing Export controller");
        
        setupNormalizationCombo();
        setupTrainRatioSlider();
        setupEventHandlers();
        loadCurrentSettings();
        setDefaultExportPath();
    }

    private void setupNormalizationCombo() {
        defaultNormCombo.getItems().addAll(NormalizationType.values());
    }

    private void setupTrainRatioSlider() {
        defaultTrainRatioSlider.setMin(0.5);
        defaultTrainRatioSlider.setMax(0.95);
        
        defaultTrainRatioSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            trainRatioLabel.setText(String.format("%.0f%% / %.0f%%", 
                newVal.doubleValue() * 100, (1 - newVal.doubleValue()) * 100));
        });
    }

    private void setupEventHandlers() {
        // Export handlers
        exportDataButton.setOnAction(e -> exportStockData());
        exportPredictionsButton.setOnAction(e -> exportPredictions());
        exportCombinedButton.setOnAction(e -> exportCombined());
        browseButton.setOnAction(e -> browseExportPath());

        // Settings handlers
        saveApiKeyButton.setOnAction(e -> saveApiKey());
        saveSettingsButton.setOnAction(e -> saveSettings());
        resetSettingsButton.setOnAction(e -> resetSettings());

        // Input validation
        defaultInvestmentField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                defaultInvestmentField.setText(oldVal);
            }
        });
    }

    private void loadCurrentSettings() {
        // API Key (masked)
        String apiKey = config.getAlphaVantageApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            apiKeyField.setText(apiKey.length() > 4 ? 
                apiKey.substring(0, 4) + "****" : "****");
        }

        // Data settings
        defaultNormCombo.setValue(config.getNormalizationType());
        defaultTrainRatioSlider.setValue(config.getTrainTestRatio());
        defaultInvestmentField.setText(String.format("%.2f", config.getInitialInvestment()));
        defaultMACheckbox.setSelected(config.isExtractMovingAverage());
        defaultRSICheckbox.setSelected(config.isExtractRSI());
    }

    private void setDefaultExportPath() {
        String userHome = System.getProperty("user.home");
        exportPathField.setText(userHome + File.separator + "stock_exports");
    }

    /**
     * Sets the data available for export.
     *
     * @param stockData   the stock data
     * @param predictions the predictions
     * @param symbol      the stock symbol
     * @param model       the trained model
     */
    public void setExportData(List<StockData> stockData, List<PredictionResult> predictions,
                               String symbol, TrainedModel model) {
        this.stockData = stockData;
        this.predictions = predictions;
        this.symbol = symbol;
        this.currentModel = model;
        
        updateExportButtons();
    }

    private void updateExportButtons() {
        exportDataButton.setDisable(stockData == null || stockData.isEmpty());
        exportPredictionsButton.setDisable(predictions == null || predictions.isEmpty());
        exportCombinedButton.setDisable(stockData == null || predictions == null);
    }

    /**
     * Exports stock data to CSV.
     */
    @FXML
    public void exportStockData() {
        if (stockData == null || stockData.isEmpty()) {
            showExportStatus("Nema podataka za izvoz", true);
            return;
        }

        Path exportPath = getExportFilePath("stock_data");
        
        try {
            ensureDirectoryExists(exportPath.getParent());
            csvService.exportStockData(stockData, exportPath);
            showExportStatus("Podatci izvezeni u: " + exportPath, false);
            logger.info("Exported stock data to {}", exportPath);
        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage());
            showExportStatus("Izvoz nije uspio: " + e.getMessage(), true);
        }
    }

    /**
     * Exports predictions to CSV.
     */
    @FXML
    public void exportPredictions() {
        if (predictions == null || predictions.isEmpty()) {
            showExportStatus("Nema predikcija za izvoz", true);
            return;
        }

        Path exportPath = getExportFilePath("predictions");
        
        try {
            ensureDirectoryExists(exportPath.getParent());
            
            String metadata = buildMetadata();
            csvService.exportPredictions(predictions, exportPath, metadata);
            
            showExportStatus("Predikcije izvezene u: " + exportPath, false);
            logger.info("Exported predictions to {}", exportPath);
        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage());
            showExportStatus("Izvoz nije uspio: " + e.getMessage(), true);
        }
    }

    /**
     * Exports combined stock data and predictions.
     */
    @FXML
    public void exportCombined() {
        if (stockData == null || predictions == null) {
            showExportStatus("Nedovoljno podataka za zajednički izvoz", true);
            return;
        }

        Path exportPath = getExportFilePath("combined");
        
        try {
            ensureDirectoryExists(exportPath.getParent());
            csvService.exportCombined(stockData, predictions, exportPath);
            showExportStatus("Zajednički podatci izvezeni u: " + exportPath, false);
            logger.info("Exported combined data to {}", exportPath);
        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage());
            showExportStatus("Izvoz nije uspio: " + e.getMessage(), true);
        }
    }

    private Path getExportFilePath(String prefix) {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        String symbolPart = symbol != null ? symbol + "_" : "";
        String filename = String.format("%s%s_%s.csv", symbolPart, prefix, timestamp);
        return Path.of(exportPathField.getText(), filename);
    }

    private void ensureDirectoryExists(Path directory) {
        File dir = directory.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String buildMetadata() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbol: ").append(symbol != null ? symbol : "Unknown").append("; ");
        
        if (currentModel != null) {
            sb.append("Model: ").append(currentModel.getModelType().getDisplayName()).append("; ");
            sb.append("RMSE: ").append(String.format("%.4f", currentModel.getMetrics().getRmse()));
        }
        
        return sb.toString();
    }

    /**
     * Opens a directory chooser for export path.
     */
    @FXML
    public void browseExportPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Odaberite mapu za izvoz");
        chooser.setInitialDirectory(new File(exportPathField.getText()));
        
        File selected = chooser.showDialog(exportPathField.getScene().getWindow());
        if (selected != null) {
            exportPathField.setText(selected.getAbsolutePath());
        }
    }

    /**
     * Saves the API key.
     */
    @FXML
    public void saveApiKey() {
        String apiKey = apiKeyField.getText().trim();
        
        // Don't save if it's masked
        if (apiKey.contains("****")) {
            showSettingsStatus("Unesite novi ključ za izmjenu", true);
            return;
        }
        
        config.setAlphaVantageApiKey(apiKey);
        dataService.setApiKey(apiKey);
        
        // Mask the displayed key
        if (apiKey.length() > 4) {
            apiKeyField.setText(apiKey.substring(0, 4) + "****");
        }
        
        showSettingsStatus("Pristupni ključ spremljen", false);
        logger.info("API key updated");
    }

    /**
     * Saves all settings.
     */
    @FXML
    public void saveSettings() {
        try {
            config.setNormalizationType(defaultNormCombo.getValue());
            config.setTrainTestRatio(defaultTrainRatioSlider.getValue());
            config.setInitialInvestment(Double.parseDouble(defaultInvestmentField.getText()));
            config.setExtractMovingAverage(defaultMACheckbox.isSelected());
            config.setExtractRSI(defaultRSICheckbox.isSelected());
            
            showSettingsStatus("Settings saved", false);
            logger.info("Settings saved");
        } catch (Exception e) {
            logger.error("Failed to save settings: {}", e.getMessage());
            showSettingsStatus("Spremanje nije uspjelo: " + e.getMessage(), true);
        }
    }

    /**
     * Resets settings to defaults.
     */
    @FXML
    public void resetSettings() {
        config.resetToDefaults();
        loadCurrentSettings();
        showSettingsStatus("Settings reset to defaults", false);
        logger.info("Settings reset to defaults");
    }

    private void showExportStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            exportStatusLabel.setText(message);
            exportStatusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    private void showSettingsStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            settingsStatusLabel.setText(message);
            settingsStatusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    @Override
    public void refresh() {
        loadCurrentSettings();
        updateExportButtons();
    }

    @Override
    public void cleanup() {
        stockData = null;
        predictions = null;
    }
}
