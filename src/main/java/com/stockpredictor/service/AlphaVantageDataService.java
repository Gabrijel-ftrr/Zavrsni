package com.stockpredictor.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stockpredictor.model.StockData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching historical stock data from Alpha Vantage API.
 * Provides methods for retrieving OHLCV data for any stock symbol.
 * 
 * Note: Alpha Vantage free tier allows 25 requests per day.
 * Get your free API key at: https://www.alphavantage.co/support/#api-key
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class AlphaVantageDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageDataService.class);
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private String apiKey;

    /**
     * Constructs a new AlphaVantageDataService with default API key.
     * Set your API key using setApiKey() or environment variable ALPHA_VANTAGE_API_KEY.
     */
    public AlphaVantageDataService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newCachedThreadPool();
        
        // Try to get API key from environment variable
        this.apiKey = System.getenv("ALPHA_VANTAGE_API_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            // Use demo key for testing (very limited)
            this.apiKey = "demo";
            logger.warn("No API key set. Using demo key with limited functionality. " +
                       "Set ALPHA_VANTAGE_API_KEY environment variable or call setApiKey()");
        }
    }

    /**
     * Constructs a new AlphaVantageDataService with specified API key.
     *
     * @param apiKey the Alpha Vantage API key
     */
    public AlphaVantageDataService(String apiKey) {
        this();
        setApiKey(apiKey);
    }

    /**
     * Sets the Alpha Vantage API key.
     *
     * @param apiKey the API key to use
     */
    public void setApiKey(String apiKey) {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.apiKey = apiKey;
            logger.info("Alpha Vantage API key set");
        }
    }

    /**
     * Gets the current API key (masked for security).
     *
     * @return masked API key
     */
    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.length() < 4) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****";
    }

    /**
     * Fetches historical stock data for a given symbol.
     *
     * @param symbol    the stock ticker symbol (e.g., "AAPL", "GOOGL")
     * @param startDate the start date for historical data
     * @param endDate   the end date for historical data
     * @return list of StockData objects sorted by date
     * @throws IOException if fetching fails
     */
    public List<StockData> fetchHistoricalData(String symbol, LocalDate startDate, 
                                                LocalDate endDate) throws IOException {
        logger.info("Fetching historical data for {} from {} to {}", symbol, startDate, endDate);

        String url = buildUrl(symbol, "TIME_SERIES_DAILY", "compact");
        
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseTimeSeriesResponse(responseBody, symbol, startDate, endDate);
        }
    }

    /**
     * Fetches daily adjusted data (includes adjusted close for splits/dividends).
     *
     * @param symbol    the stock ticker symbol
     * @param startDate the start date
     * @param endDate   the end date
     * @return list of StockData objects
     * @throws IOException if fetching fails
     */
    public List<StockData> fetchDailyAdjusted(String symbol, LocalDate startDate,
                                               LocalDate endDate) throws IOException {
        logger.info("Fetching daily adjusted data for {}", symbol);

        String url = buildUrl(symbol, "TIME_SERIES_DAILY_ADJUSTED", "compact");
        
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API request failed: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseAdjustedTimeSeriesResponse(responseBody, symbol, startDate, endDate);
        }
    }

    /**
     * Fetches historical data asynchronously.
     *
     * @param symbol    the stock ticker symbol
     * @param startDate the start date
     * @param endDate   the end date
     * @return CompletableFuture containing the stock data
     */
    public CompletableFuture<List<StockData>> fetchHistoricalDataAsync(String symbol,
                                                                        LocalDate startDate,
                                                                        LocalDate endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchHistoricalData(symbol, startDate, endDate);
            } catch (IOException e) {
                logger.error("Async fetch failed for {}: {}", symbol, e.getMessage());
                throw new RuntimeException("Failed to fetch data for " + symbol, e);
            }
        }, executor);
    }

    /**
     * Fetches the last N days of historical data.
     *
     * @param symbol the stock ticker symbol
     * @param days   number of days to fetch
     * @return list of StockData objects
     * @throws IOException if fetching fails
     */
    public List<StockData> fetchLastNDays(String symbol, int days) throws IOException {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return fetchHistoricalData(symbol, startDate, endDate);
    }

    /**
     * Validates if a stock symbol exists by attempting to fetch data.
     *
     * @param symbol the stock ticker symbol
     * @return true if the symbol is valid
     */
    public boolean isValidSymbol(String symbol) {
        try {
            String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                    BASE_URL, symbol, apiKey);
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return false;
                }
                
                String body = response.body() != null ? response.body().string() : "";
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                
                return json.has("Global Quote") && 
                       !json.getAsJsonObject("Global Quote").entrySet().isEmpty();
            }
        } catch (Exception e) {
            logger.debug("Symbol validation failed for {}: {}", symbol, e.getMessage());
            return false;
        }
    }

    /**
     * Gets stock overview information including company name.
     *
     * @param symbol the stock ticker symbol
     * @return the company name, or null if not found
     */
    public String getStockName(String symbol) {
        try {
            String url = String.format("%s?function=OVERVIEW&symbol=%s&apikey=%s",
                    BASE_URL, symbol, apiKey);
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }
                
                String body = response.body() != null ? response.body().string() : "";
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                
                if (json.has("Name")) {
                    return json.get("Name").getAsString();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get stock name for {}: {}", symbol, e.getMessage());
        }
        return symbol; // Return symbol as fallback
    }

    /**
     * Gets the current quote for a symbol.
     *
     * @param symbol the stock ticker symbol
     * @return current price, or -1 if unavailable
     */
    public double getCurrentPrice(String symbol) {
        try {
            String url = String.format("%s?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                    BASE_URL, symbol, apiKey);
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return -1;
                }
                
                String body = response.body() != null ? response.body().string() : "";
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                
                if (json.has("Global Quote")) {
                    JsonObject quote = json.getAsJsonObject("Global Quote");
                    if (quote.has("05. price")) {
                        return quote.get("05. price").getAsDouble();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get current price for {}: {}", symbol, e.getMessage());
        }
        return -1;
    }

    /**
     * Builds the API URL for a given function.
     */
    private String buildUrl(String symbol, String function, String outputSize) {
        return String.format("%s?function=%s&symbol=%s&outputsize=%s&apikey=%s",
                BASE_URL, function, symbol, outputSize, apiKey);
    }

    /**
     * Parses the TIME_SERIES_DAILY response.
     */
    private List<StockData> parseTimeSeriesResponse(String responseBody, String symbol,
                                                     LocalDate startDate, LocalDate endDate) 
                                                     throws IOException {
        List<StockData> result = new ArrayList<>();
        
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        
        // Check for API errors
        if (json.has("Error Message")) {
            throw new IOException("API Error: " + json.get("Error Message").getAsString());
        }
        
        if (json.has("Note")) {
            logger.warn("API Note: {}", json.get("Note").getAsString());
        }
        
        if (json.has("Information")) {
            throw new IOException("API limit reached: " + json.get("Information").getAsString());
        }

        JsonObject timeSeries = json.getAsJsonObject("Time Series (Daily)");
        if (timeSeries == null) {
            logger.warn("No time series data found in response");
            return result;
        }

        for (Map.Entry<String, JsonElement> entry : timeSeries.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
            
            // Filter by date range
            if (date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }

            JsonObject dayData = entry.getValue().getAsJsonObject();
            
            StockData stockData = new StockData(
                date,
                symbol,
                parseDoubleField(dayData, "1. open"),
                parseDoubleField(dayData, "2. high"),
                parseDoubleField(dayData, "3. low"),
                parseDoubleField(dayData, "4. close"),
                parseLongField(dayData, "5. volume")
            );
            
            result.add(stockData);
        }

        // Sort by date ascending
        result.sort(Comparator.comparing(StockData::getDate));
        
        logger.info("Retrieved {} data points for {}", result.size(), symbol);
        return result;
    }

    /**
     * Parses the TIME_SERIES_DAILY_ADJUSTED response.
     */
    private List<StockData> parseAdjustedTimeSeriesResponse(String responseBody, String symbol,
                                                             LocalDate startDate, LocalDate endDate) 
                                                             throws IOException {
        List<StockData> result = new ArrayList<>();
        
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        
        // Check for API errors
        if (json.has("Error Message")) {
            throw new IOException("API Error: " + json.get("Error Message").getAsString());
        }

        JsonObject timeSeries = json.getAsJsonObject("Time Series (Daily)");
        if (timeSeries == null) {
            logger.warn("No time series data found in response");
            return result;
        }

        for (Map.Entry<String, JsonElement> entry : timeSeries.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
            
            if (date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }

            JsonObject dayData = entry.getValue().getAsJsonObject();
            
            // Use adjusted close for the close price
            double adjustedClose = parseDoubleField(dayData, "5. adjusted close");
            if (adjustedClose == 0) {
                adjustedClose = parseDoubleField(dayData, "4. close");
            }
            
            StockData stockData = new StockData(
                date,
                symbol,
                parseDoubleField(dayData, "1. open"),
                parseDoubleField(dayData, "2. high"),
                parseDoubleField(dayData, "3. low"),
                adjustedClose,
                parseLongField(dayData, "6. volume")
            );
            
            result.add(stockData);
        }

        result.sort(Comparator.comparing(StockData::getDate));
        
        logger.info("Retrieved {} adjusted data points for {}", result.size(), symbol);
        return result;
    }

    /**
     * Safely parses a double field from JSON.
     */
    private double parseDoubleField(JsonObject json, String field) {
        try {
            if (json.has(field)) {
                return json.get(field).getAsDouble();
            }
        } catch (Exception e) {
            logger.debug("Failed to parse field {}: {}", field, e.getMessage());
        }
        return 0.0;
    }

    /**
     * Safely parses a long field from JSON.
     */
    private long parseLongField(JsonObject json, String field) {
        try {
            if (json.has(field)) {
                return json.get(field).getAsLong();
            }
        } catch (Exception e) {
            logger.debug("Failed to parse field {}: {}", field, e.getMessage());
        }
        return 0L;
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executor.shutdown();
        logger.info("AlphaVantageDataService executor shutdown");
    }
}
