package com.stockpredictor.service;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVService.
 */
class CSVServiceTest {

    private CSVService csvService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        csvService = new CSVService();
    }

    @Test
    @DisplayName("Should export stock data to CSV")
    void testExportStockData() throws IOException {
        List<StockData> data = createSampleStockData();
        Path exportPath = tempDir.resolve("test_export.csv");
        
        csvService.exportStockData(data, exportPath);
        
        assertTrue(Files.exists(exportPath));
        List<String> lines = Files.readAllLines(exportPath);
        assertEquals(4, lines.size()); // Header + 3 data rows
        assertTrue(lines.get(0).contains("Date"));
        assertTrue(lines.get(0).contains("Close"));
    }

    @Test
    @DisplayName("Should export predictions to CSV")
    void testExportPredictions() throws IOException {
        List<PredictionResult> predictions = createSamplePredictions();
        Path exportPath = tempDir.resolve("test_predictions.csv");
        
        csvService.exportPredictions(predictions, exportPath, "Test metadata");
        
        assertTrue(Files.exists(exportPath));
        String content = Files.readString(exportPath);
        assertTrue(content.contains("PredictedValue"));
        assertTrue(content.contains("ActualValue"));
    }

    @Test
    @DisplayName("Should import stock data from CSV")
    void testImportStockData() throws IOException {
        // First export, then import
        List<StockData> originalData = createSampleStockData();
        Path csvPath = tempDir.resolve("test_import.csv");
        csvService.exportStockData(originalData, csvPath);
        
        List<StockData> importedData = csvService.importStockData(csvPath, "AAPL");
        
        assertNotNull(importedData);
        assertEquals(originalData.size(), importedData.size());
    }

    @Test
    @DisplayName("Should handle empty data gracefully")
    void testExportEmptyData() throws IOException {
        List<StockData> emptyData = new ArrayList<>();
        Path exportPath = tempDir.resolve("empty_export.csv");
        
        csvService.exportStockData(emptyData, exportPath);
        
        assertTrue(Files.exists(exportPath));
        List<String> lines = Files.readAllLines(exportPath);
        assertEquals(1, lines.size()); // Only header
    }

    @Test
    @DisplayName("Should export combined data correctly")
    void testExportCombined() throws IOException {
        List<StockData> stockData = createSampleStockData();
        List<PredictionResult> predictions = createSamplePredictions();
        Path exportPath = tempDir.resolve("combined_export.csv");
        
        csvService.exportCombined(stockData, predictions, exportPath);
        
        assertTrue(Files.exists(exportPath));
        String content = Files.readString(exportPath);
        assertTrue(content.contains("ActualClose"));
        assertTrue(content.contains("PredictedClose"));
    }

    private List<StockData> createSampleStockData() {
        List<StockData> data = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        
        for (int i = 0; i < 3; i++) {
            data.add(new StockData(
                baseDate.plusDays(i),
                "AAPL",
                150.0 + i,
                155.0 + i,
                148.0 + i,
                152.0 + i,
                1000000L + i * 10000
            ));
        }
        
        return data;
    }

    private List<PredictionResult> createSamplePredictions() {
        List<PredictionResult> predictions = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        
        for (int i = 0; i < 3; i++) {
            predictions.add(new PredictionResult(
                baseDate.plusDays(i),
                150.0 + i + 0.5, // Predicted
                150.0 + i,       // Actual
                "TestModel"
            ));
        }
        
        return predictions;
    }
}
