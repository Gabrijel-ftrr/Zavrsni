package com.stockpredictor.controller;

import com.stockpredictor.config.AppConfig;
import com.stockpredictor.config.ServiceProvider;
import com.stockpredictor.model.*;
import com.stockpredictor.service.DataPreparationService;
import com.stockpredictor.service.ModelTrainerService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controller for model training and selection view.
 * Handles ML model training, evaluation, and automatic selection.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class ModelController implements BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelController.class);

    @FXML private ComboBox<NormalizationType> normalizationCombo;
    @FXML private Slider trainTestSlider;
    @FXML private Label trainTestLabel;
    @FXML private CheckBox extractMACheckbox;
    @FXML private CheckBox extractRSICheckbox;
    @FXML private RadioButton quickModeRadio;
    @FXML private RadioButton fullModeRadio;
    @FXML private Button trainButton;
    @FXML private Button autoSelectButton;
    @FXML private ProgressBar trainingProgress;
    @FXML private Label trainingStatusLabel;
    @FXML private TableView<ModelMetrics> metricsTable;
    @FXML private TableColumn<ModelMetrics, String> modelNameColumn;
    @FXML private TableColumn<ModelMetrics, Double> rmseColumn;
    @FXML private TableColumn<ModelMetrics, Double> maeColumn;
    @FXML private TableColumn<ModelMetrics, Double> r2Column;
    @FXML private TableColumn<ModelMetrics, Long> timeColumn;
    @FXML private VBox bestModelBox;
    @FXML private Label bestModelLabel;
    @FXML private TextArea modelDetailsArea;

    private final ModelTrainerService modelTrainerService;
    private final DataPreparationService dataPreparationService;
    private final AppConfig config;

    private List<StockData> stockData;
    private String symbol;
    private DataSet currentDataSet;
    private Map<ModelType, TrainedModel> trainedModels;
    private TrainedModel bestModel;
    private Consumer<TrainedModel> onModelSelected;

    /**
     * Constructs the ModelController with required services.
     */
    public ModelController() {
        ServiceProvider provider = ServiceProvider.getInstance();
        this.modelTrainerService = provider.getModelTrainerService();
        this.dataPreparationService = provider.getDataPreparationService();
        this.config = AppConfig.getInstance();
    }

    @Override
    @FXML
    public void initialize() {
        logger.info("Initializing Model controller");
        
        setupNormalizationCombo();
        setupTrainTestSlider();
        setupRadioButtons();
        setupTable();
        setupEventHandlers();
        setDefaultValues();
        hideProgress();
    }

    private void setupNormalizationCombo() {
        normalizationCombo.getItems().addAll(NormalizationType.values());
        normalizationCombo.setValue(config.getNormalizationType());
    }

    private void setupTrainTestSlider() {
        trainTestSlider.setMin(0.5);
        trainTestSlider.setMax(0.95);
        trainTestSlider.setValue(config.getTrainTestRatio());
        
        trainTestSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTrainTestLabel(newVal.doubleValue());
        });
        
        updateTrainTestLabel(trainTestSlider.getValue());
    }

    private void updateTrainTestLabel(double ratio) {
        trainTestLabel.setText(String.format("Train: %.0f%% / Test: %.0f%%", 
            ratio * 100, (1 - ratio) * 100));
    }

    private void setupRadioButtons() {
        ToggleGroup modeGroup = new ToggleGroup();
        quickModeRadio.setToggleGroup(modeGroup);
        fullModeRadio.setToggleGroup(modeGroup);
        
        if (config.isQuickModeDefault()) {
            quickModeRadio.setSelected(true);
        } else {
            fullModeRadio.setSelected(true);
        }
    }

    private void setupTable() {
        modelNameColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getModelName()));
        rmseColumn.setCellValueFactory(data -> 
            new SimpleDoubleProperty(data.getValue().getRmse()).asObject());
        maeColumn.setCellValueFactory(data -> 
            new SimpleDoubleProperty(data.getValue().getMae()).asObject());
        r2Column.setCellValueFactory(data -> 
            new SimpleDoubleProperty(data.getValue().getRSquared()).asObject());
        timeColumn.setCellValueFactory(data -> 
            new SimpleLongProperty(data.getValue().getTrainingTimeMs()).asObject());

        // Format columns
        rmseColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.4f", item));
            }
        });

        maeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.4f", item));
            }
        });

        r2Column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("%.4f", item));
            }
        });

        timeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item + " ms");
            }
        });

        // Selection listener
        metricsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showModelDetails(newVal);
            }
        });
    }

    private void setupEventHandlers() {
        trainButton.setOnAction(e -> trainModels());
        autoSelectButton.setOnAction(e -> autoSelectBestModel());
    }

    private void setDefaultValues() {
        extractMACheckbox.setSelected(config.isExtractMovingAverage());
        extractRSICheckbox.setSelected(config.isExtractRSI());
    }

    /**
     * Sets the stock data to use for training.
     *
     * @param data   the stock data
     * @param symbol the stock symbol
     */
    public void setStockData(List<StockData> data, String symbol) {
        this.stockData = data;
        this.symbol = symbol;
        
        if (data != null && !data.isEmpty()) {
            trainingStatusLabel.setText("Spremno za treniranje: " + data.size() + " zapisa");
            trainButton.setDisable(false);
        } else {
            trainingStatusLabel.setText("Podatci nisu učitani – najprije ih dohvatite");
            trainButton.setDisable(true);
        }
    }

    /**
     * Sets a callback for when a model is selected.
     *
     * @param callback the callback function
     */
    public void setOnModelSelected(Consumer<TrainedModel> callback) {
        this.onModelSelected = callback;
    }

    /**
     * Trains all selected models.
     */
    @FXML
    public void trainModels() {
        if (stockData == null || stockData.isEmpty()) {
            showStatus("Podatci nisu učitani", true);
            return;
        }

        showProgress();
        trainingStatusLabel.setText("Priprema podataka...");

        Task<Map<ModelType, TrainedModel>> trainTask = new Task<>() {
            @Override
            protected Map<ModelType, TrainedModel> call() throws Exception {
                // Prepare dataset
                currentDataSet = dataPreparationService.prepareDataSet(
                    stockData, symbol,
                    trainTestSlider.getValue(),
                    normalizationCombo.getValue(),
                    extractMACheckbox.isSelected(),
                    extractRSICheckbox.isSelected()
                );

                Platform.runLater(() -> 
                    trainingStatusLabel.setText("Treniranje u tijeku..."));

                // Train models
                boolean quickMode = quickModeRadio.isSelected();
                return modelTrainerService.trainAllModels(currentDataSet, quickMode);
            }
        };

        trainTask.setOnSucceeded(e -> {
            trainedModels = trainTask.getValue();
            updateMetricsTable();
            hideProgress();
            showStatus(trainedModels.isEmpty()
                ? "Nijedan model nije istreniran \u2013 provjerite zapisnik"
                : "Treniranje dovr\u0161eno \u2013 istreniranih modela: " + trainedModels.size(),
                trainedModels.isEmpty());
        });

        trainTask.setOnFailed(e -> {
            logger.error("Training failed: {}", trainTask.getException().getMessage());
            hideProgress();
            showStatus("Training failed: " + trainTask.getException().getMessage(), true);
        });

        new Thread(trainTask).start();
    }

    private void updateMetricsTable() {
        metricsTable.getItems().clear();
        
        for (TrainedModel model : trainedModels.values()) {
            metricsTable.getItems().add(model.getMetrics());
        }

        // Sort by RMSE
        metricsTable.getSortOrder().clear();
        rmseColumn.setSortType(TableColumn.SortType.ASCENDING);
        metricsTable.getSortOrder().add(rmseColumn);
        metricsTable.sort();
    }

    /**
     * Automatically selects the best performing model.
     */
    @FXML
    public void autoSelectBestModel() {
        if (trainedModels == null || trainedModels.isEmpty()) {
            showStatus("Train models first", true);
            return;
        }

        bestModel = modelTrainerService.selectBestModel(trainedModels);
        
        if (bestModel != null) {
            displayBestModel();
            
            if (onModelSelected != null) {
                onModelSelected.accept(bestModel);
            }
        }
    }

    private void displayBestModel() {
        if (bestModel == null) {
            return;
        }

        ModelMetrics metrics = bestModel.getMetrics();
        bestModelLabel.setText("Najbolji model: " + metrics.getModelName());
        bestModelBox.setVisible(true);

        String details = String.format(
            "Selected Model: %s%n%n" +
            "Mjere kvalitete:%n" +
            "  RMSE: %.4f%n" +
            "  MAE: %.4f%n" +
            "  R² Score: %.4f%n%n" +
            "Training Info:%n" +
            "  Training Time: %d ms%n" +
            "  Training Samples: %d%n" +
            "  Test Samples: %d%n%n" +
            "This model was selected because it has the lowest RMSE (Root Mean Square Error) " +
            "among all trained models, indicating better prediction accuracy.",
            metrics.getModelName(),
            metrics.getRmse(), metrics.getMae(), metrics.getRSquared(),
            metrics.getTrainingTimeMs(), metrics.getTrainingDataSize(), metrics.getTestDataSize()
        );

        modelDetailsArea.setText(details);
    }

    private void showModelDetails(ModelMetrics metrics) {
        String details = String.format(
            "Model: %s%n%n" +
            "RMSE: %.6f%n" +
            "MAE: %.6f%n" +
            "R²: %.6f%n%n" +
            "Training Time: %d ms%n" +
            "Podjela podataka: %d / %d (učenje / ispitivanje)",
            metrics.getModelName(),
            metrics.getRmse(), metrics.getMae(), metrics.getRSquared(),
            metrics.getTrainingTimeMs(), 
            metrics.getTrainingDataSize(), metrics.getTestDataSize()
        );

        modelDetailsArea.setText(details);
    }

    /**
     * Gets the best trained model.
     *
     * @return the best model
     */
    public TrainedModel getBestModel() {
        return bestModel;
    }

    /**
     * Gets the current dataset.
     *
     * @return the prepared dataset
     */
    public DataSet getCurrentDataSet() {
        return currentDataSet;
    }

    /**
     * Gets all trained models.
     *
     * @return map of trained models
     */
    public Map<ModelType, TrainedModel> getTrainedModels() {
        return trainedModels;
    }

    private void showStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            trainingStatusLabel.setText(message);
            trainingStatusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
        });
    }

    private void showProgress() {
        Platform.runLater(() -> {
            trainingProgress.setVisible(true);
            trainingProgress.setProgress(-1);
            trainButton.setDisable(true);
            autoSelectButton.setDisable(true);
        });
    }

    private void hideProgress() {
        Platform.runLater(() -> {
            trainingProgress.setVisible(false);
            trainButton.setDisable(false);
            autoSelectButton.setDisable(trainedModels == null || trainedModels.isEmpty());
        });
    }

    @Override
    public void refresh() {
        if (trainedModels != null) {
            updateMetricsTable();
        }
    }

    @Override
    public void cleanup() {
        modelTrainerService.clearModels();
        trainedModels = null;
        bestModel = null;
    }
}
