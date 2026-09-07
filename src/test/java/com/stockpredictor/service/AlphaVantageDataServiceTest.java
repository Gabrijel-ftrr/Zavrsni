package com.stockpredictor.service;

import com.stockpredictor.model.StockData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AlphaVantageDataService.
 * Note: Some tests require a valid API key set via ALPHA_VANTAGE_API_KEY environment variable.
 */
class AlphaVantageDataServiceTest {

    private AlphaVantageDataService service;

    @BeforeEach
    void setUp() {
        service = new AlphaVantageDataService();
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    @DisplayName("Service should initialize without API key")
    void testInitializationWithoutApiKey() {
        assertNotNull(service);
        assertNotNull(service.getMaskedApiKey());
    }

    @Test
    @DisplayName("Should set API key correctly")
    void testSetApiKey() {
        service.setApiKey("testkey123");
        assertEquals("test****", service.getMaskedApiKey());
    }

    @Test
    @DisplayName("Should handle null API key gracefully")
    void testNullApiKey() {
        service.setApiKey(null);
        // Should not throw exception
        assertNotNull(service.getMaskedApiKey());
    }

    @Test
    @DisplayName("Masked API key should hide most characters")
    void testMaskedApiKey() {
        service.setApiKey("ABCDEFGHIJKLMNOP");
        String masked = service.getMaskedApiKey();
        assertTrue(masked.contains("****"));
        assertEquals(8, masked.length());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ALPHA_VANTAGE_API_KEY", matches = ".+")
    @DisplayName("Should fetch historical data with valid API key")
    void testFetchHistoricalData() throws IOException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        
        List<StockData> data = service.fetchHistoricalData("IBM", startDate, endDate);
        
        assertNotNull(data);
        assertFalse(data.isEmpty());
        
        // Verify data structure
        StockData firstRecord = data.get(0);
        assertNotNull(firstRecord.getDate());
        assertNotNull(firstRecord.getSymbol());
        assertTrue(firstRecord.getClose() > 0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ALPHA_VANTAGE_API_KEY", matches = ".+")
    @DisplayName("Should validate symbol correctly")
    void testIsValidSymbol() {
        assertTrue(service.isValidSymbol("IBM"));
        assertFalse(service.isValidSymbol("INVALID_SYMBOL_XYZ123"));
    }

    @Test
    @DisplayName("Data should be sorted by date ascending")
    void testDataSorting() throws IOException {
        // This test uses the demo API which only works with IBM
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(10);
            
            List<StockData> data = service.fetchHistoricalData("IBM", startDate, endDate);
            
            if (!data.isEmpty()) {
                for (int i = 1; i < data.size(); i++) {
                    assertTrue(
                        data.get(i).getDate().isAfter(data.get(i - 1).getDate()) ||
                        data.get(i).getDate().isEqual(data.get(i - 1).getDate()),
                        "Data should be sorted by date ascending"
                    );
                }
            }
        } catch (IOException e) {
            // Skip if API limit reached
            System.out.println("Skipping test due to API limit: " + e.getMessage());
        }
    }
}
