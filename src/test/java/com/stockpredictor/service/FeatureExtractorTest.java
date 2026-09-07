package com.stockpredictor.service;

import com.stockpredictor.model.StockData;
import com.stockpredictor.util.FeatureExtractor;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FeatureExtractor utility class.
 */
class FeatureExtractorTest {

    @Test
    @DisplayName("Should calculate moving average correctly")
    void testMovingAverage() {
        List<StockData> data = createConstantPriceData(20, 100.0);
        
        List<Double> ma = FeatureExtractor.calculateMovingAverage(data, 5);
        
        assertEquals(20, ma.size());
        // First 4 values should be null (insufficient data)
        for (int i = 0; i < 4; i++) {
            assertNull(ma.get(i));
        }
        // From index 4 onwards, MA should equal 100.0 (constant price)
        for (int i = 4; i < 20; i++) {
            assertNotNull(ma.get(i));
            assertEquals(100.0, ma.get(i), 0.001);
        }
    }

    @Test
    @DisplayName("Should calculate RSI correctly")
    void testRSI() {
        List<StockData> data = createTrendingData(30);
        
        List<Double> rsi = FeatureExtractor.calculateRSI(data, 14);
        
        assertEquals(30, rsi.size());
        // First 14 values should be null
        for (int i = 0; i < 14; i++) {
            assertNull(rsi.get(i));
        }
        // From index 14 onwards, RSI should be between 0 and 100
        for (int i = 14; i < 30; i++) {
            assertNotNull(rsi.get(i));
            assertTrue(rsi.get(i) >= 0 && rsi.get(i) <= 100, 
                "RSI should be between 0 and 100");
        }
    }

    @Test
    @DisplayName("RSI should be 100 for only gains")
    void testRSIOnlyGains() {
        List<StockData> data = createIncreasingData(20);
        
        List<Double> rsi = FeatureExtractor.calculateRSI(data, 14);
        
        // For constantly increasing prices, RSI should approach 100
        Double lastRsi = rsi.get(rsi.size() - 1);
        assertNotNull(lastRsi);
        assertEquals(100.0, lastRsi, 0.001);
    }

    @Test
    @DisplayName("Should calculate EMA correctly")
    void testEMA() {
        List<StockData> data = createConstantPriceData(20, 100.0);
        
        List<Double> ema = FeatureExtractor.calculateEMA(data, 10);
        
        assertEquals(20, ema.size());
        // For constant price, EMA should converge to that price
        assertEquals(100.0, ema.get(ema.size() - 1), 0.01);
    }

    @Test
    @DisplayName("Should extract all features correctly")
    void testExtractFeatures() {
        List<StockData> data = createTrendingData(50);
        
        List<StockData> result = FeatureExtractor.extractFeatures(data, true, true);
        
        assertEquals(50, result.size());
        
        // Check that features are populated for later data points
        StockData lastPoint = result.get(result.size() - 1);
        assertNotNull(lastPoint.getMovingAverage());
        assertNotNull(lastPoint.getRsi());
    }

    @Test
    @DisplayName("Should handle empty data gracefully")
    void testEmptyData() {
        List<StockData> emptyData = new ArrayList<>();
        
        List<StockData> result = FeatureExtractor.extractFeatures(emptyData, true, true);
        
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle insufficient data for MA")
    void testInsufficientDataForMA() {
        List<StockData> smallData = createConstantPriceData(3, 100.0);
        
        List<Double> ma = FeatureExtractor.calculateMovingAverage(smallData, 20);
        
        assertEquals(3, ma.size());
        assertTrue(ma.stream().allMatch(v -> v == null));
    }

    @Test
    @DisplayName("MA period should affect calculation window")
    void testMAPeriod() {
        List<StockData> data = createTrendingData(50);
        
        List<Double> ma5 = FeatureExtractor.calculateMovingAverage(data, 5);
        List<Double> ma20 = FeatureExtractor.calculateMovingAverage(data, 20);
        
        // MA5 should have fewer nulls than MA20
        long nullCount5 = ma5.stream().filter(v -> v == null).count();
        long nullCount20 = ma20.stream().filter(v -> v == null).count();
        
        assertEquals(4, nullCount5);
        assertEquals(19, nullCount20);
    }

    private List<StockData> createConstantPriceData(int count, double price) {
        List<StockData> data = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        
        for (int i = 0; i < count; i++) {
            data.add(new StockData(
                baseDate.plusDays(i),
                "TEST",
                price, price, price, price,
                1000000L
            ));
        }
        
        return data;
    }

    private List<StockData> createIncreasingData(int count) {
        List<StockData> data = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        
        for (int i = 0; i < count; i++) {
            double price = 100.0 + i;
            data.add(new StockData(
                baseDate.plusDays(i),
                "TEST",
                price - 0.5, price + 0.5, price - 1, price,
                1000000L
            ));
        }
        
        return data;
    }

    private List<StockData> createTrendingData(int count) {
        List<StockData> data = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        
        for (int i = 0; i < count; i++) {
            double price = 100.0 + Math.sin(i * 0.2) * 10 + i * 0.5;
            data.add(new StockData(
                baseDate.plusDays(i),
                "TEST",
                price - 1, price + 2, price - 2, price,
                1000000L + (long)(Math.random() * 50000)
            ));
        }
        
        return data;
    }
}
