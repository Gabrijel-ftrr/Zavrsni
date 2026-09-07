package com.stockpredictor.service;

import com.stockpredictor.model.DataSet;
import com.stockpredictor.model.NormalizationType;
import com.stockpredictor.model.StockData;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataPreparationService.
 */
class DataPreparationServiceTest {

    private DataPreparationService service;

    @BeforeEach
    void setUp() {
        service = new DataPreparationService();
    }

    @Test
    @DisplayName("Should prepare dataset with correct train/test split")
    void testPrepareDataSet() {
        List<StockData> data = createSampleData(100);
        
        DataSet dataSet = service.prepareDataSet(data, "AAPL", 0.8, 
            NormalizationType.NONE, false, false);
        
        assertNotNull(dataSet);
        assertEquals("AAPL", dataSet.getSymbol());
        assertEquals(80, dataSet.getTrainingData().size());
        assertEquals(20, dataSet.getTestData().size());
        assertEquals(0.8, dataSet.getTrainTestRatio());
    }

    @Test
    @DisplayName("Should apply Min-Max normalization")
    void testMinMaxNormalization() {
        List<StockData> data = createSampleData(50);
        
        DataSet dataSet = service.prepareDataSet(data, "AAPL", 0.8, 
            NormalizationType.MIN_MAX, false, false);
        
        assertEquals(NormalizationType.MIN_MAX, dataSet.getNormalizationType());
        double[] params = dataSet.getNormalizationParams();
        assertNotNull(params);
        assertEquals(2, params.length);
    }

    @Test
    @DisplayName("Should apply Standard normalization")
    void testStandardNormalization() {
        List<StockData> data = createSampleData(50);
        
        DataSet dataSet = service.prepareDataSet(data, "AAPL", 0.8, 
            NormalizationType.STANDARD, false, false);
        
        assertEquals(NormalizationType.STANDARD, dataSet.getNormalizationType());
        double[] params = dataSet.getNormalizationParams();
        assertNotNull(params);
        assertEquals(2, params.length);
    }

    @Test
    @DisplayName("Should extract features when enabled")
    void testFeatureExtraction() {
        List<StockData> data = createSampleData(50);
        
        DataSet dataSet = service.prepareDataSet(data, "AAPL", 0.8, 
            NormalizationType.NONE, true, true);
        
        // Check that at least some data points have features
        List<StockData> allData = dataSet.getAllData();
        boolean hasMA = allData.stream().anyMatch(d -> d.getMovingAverage() != null);
        boolean hasRSI = allData.stream().anyMatch(d -> d.getRsi() != null);
        
        assertTrue(hasMA, "Should have moving average calculated");
        assertTrue(hasRSI, "Should have RSI calculated");
    }

    @Test
    @DisplayName("Should throw exception for empty data")
    void testEmptyDataException() {
        List<StockData> emptyData = new ArrayList<>();
        
        assertThrows(IllegalArgumentException.class, () -> 
            service.prepareDataSet(emptyData, "AAPL", 0.8, 
                NormalizationType.NONE, false, false)
        );
    }

    @Test
    @DisplayName("Should throw exception for invalid train ratio")
    void testInvalidTrainRatio() {
        List<StockData> data = createSampleData(50);
        
        assertThrows(IllegalArgumentException.class, () -> 
            service.prepareDataSet(data, "AAPL", 1.5, 
                NormalizationType.NONE, false, false)
        );
    }

    @Test
    @DisplayName("Should throw exception for insufficient data")
    void testInsufficientData() {
        List<StockData> smallData = createSampleData(5);
        
        assertThrows(IllegalArgumentException.class, () -> 
            service.prepareDataSet(smallData, "AAPL", 0.8, 
                NormalizationType.NONE, false, false)
        );
    }

    @Test
    @DisplayName("Should filter incomplete data")
    void testFilterIncompleteData() {
        List<StockData> data = createSampleData(50);
        // Add incomplete data
        data.add(new StockData(LocalDate.now(), "AAPL", 0, 0, 0, 0, 0));
        
        List<StockData> filtered = service.filterIncompleteData(data);
        
        assertEquals(50, filtered.size()); // Should exclude the zero-close record
    }

    @Test
    @DisplayName("Should create time series splits")
    void testTimeSeriesSplits() {
        List<StockData> data = createSampleData(100);
        
        List<DataPreparationService.TrainTestSplit> splits = 
            service.createTimeSeriesSplits(data, 5);
        
        assertEquals(5, splits.size());
        
        // Each split should have training data before test data
        for (var split : splits) {
            assertFalse(split.trainData().isEmpty());
            assertFalse(split.testData().isEmpty());
        }
    }

    @Test
    @DisplayName("Should remove outliers")
    void testRemoveOutliers() {
        List<StockData> data = new ArrayList<>(createSampleData(50));
        // Add outlier
        data.add(new StockData(LocalDate.now(), "AAPL", 
            1000000, 1000000, 1000000, 1000000, 1000000L));
        
        List<StockData> cleaned = service.removeOutliers(data, 3.0);
        
        assertTrue(cleaned.size() < data.size());
    }

    private List<StockData> createSampleData(int count) {
        List<StockData> data = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        
        for (int i = 0; i < count; i++) {
            double basePrice = 150.0 + Math.sin(i * 0.1) * 10 + i * 0.1;
            data.add(new StockData(
                baseDate.plusDays(i),
                "AAPL",
                basePrice - 1,
                basePrice + 2,
                basePrice - 2,
                basePrice,
                1000000L + (long)(Math.random() * 100000)
            ));
        }
        
        return data;
    }
}
