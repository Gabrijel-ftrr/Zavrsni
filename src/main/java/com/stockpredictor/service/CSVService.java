package com.stockpredictor.service;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for importing and exporting stock data and predictions to/from CSV files.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class CSVService {
    
    private static final Logger logger = LoggerFactory.getLogger(CSVService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // CSV headers
    private static final String[] STOCK_DATA_HEADERS = {
        "Date", "Symbol", "Open", "High", "Low", "Close", "Volume", "MovingAverage", "RSI"
    };
    
    private static final String[] PREDICTION_HEADERS = {
        "Date", "PredictedValue", "ActualValue", "Error", "PercentageError", 
        "ModelName", "PredictionTimestamp"
    };

    /**
     * Imports stock data from a CSV file.
     *
     * @param filePath path to the CSV file
     * @param symbol   the stock symbol to assign
     * @return list of imported StockData objects
     * @throws IOException if reading fails
     */
    public List<StockData> importStockData(Path filePath, String symbol) throws IOException {
        logger.info("Importing stock data from: {}", filePath);
        
        List<StockData> result = new ArrayList<>();
        
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            
            for (CSVRecord record : parser) {
                StockData data = parseStockRecord(record, symbol);
                if (data != null) {
                    result.add(data);
                }
            }
        }

        logger.info("Imported {} records from CSV", result.size());
        return result;
    }

    /**
     * Parses a CSV record into StockData.
     */
    private StockData parseStockRecord(CSVRecord record, String symbol) {
        try {
            LocalDate date = parseDate(getValueOrDefault(record, "Date", ""));
            if (date == null) {
                return null;
            }

            double open = parseDouble(getValueOrDefault(record, "Open", "0"));
            double high = parseDouble(getValueOrDefault(record, "High", "0"));
            double low = parseDouble(getValueOrDefault(record, "Low", "0"));
            double close = parseDouble(getValueOrDefault(record, "Close", "0"));
            long volume = parseLong(getValueOrDefault(record, "Volume", "0"));

            return new StockData(date, symbol, open, high, low, close, volume);
        } catch (Exception e) {
            logger.warn("Failed to parse CSV record: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Safely gets a value from a CSV record.
     */
    private String getValueOrDefault(CSVRecord record, String column, String defaultValue) {
        try {
            return record.isMapped(column) ? record.get(column) : defaultValue;
        } catch (IllegalArgumentException e) {
            // Try by index for files without proper headers
            try {
                int index = switch (column) {
                    case "Date" -> 0;
                    case "Open" -> 1;
                    case "High" -> 2;
                    case "Low" -> 3;
                    case "Close" -> 4;
                    case "Volume" -> 5;
                    default -> -1;
                };
                return index >= 0 && index < record.size() ? record.get(index) : defaultValue;
            } catch (Exception ex) {
                return defaultValue;
            }
        }
    }

    /**
     * Exports stock data to a CSV file.
     *
     * @param data     the stock data to export
     * @param filePath destination file path
     * @throws IOException if writing fails
     */
    public void exportStockData(List<StockData> data, Path filePath) throws IOException {
        logger.info("Exporting {} stock records to: {}", data.size(), filePath);

        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, 
                     CSVFormat.DEFAULT.builder().setHeader(STOCK_DATA_HEADERS).build())) {
            
            for (StockData stock : data) {
                printer.printRecord(
                    stock.getDate().format(DATE_FORMATTER),
                    stock.getSymbol(),
                    stock.getOpen(),
                    stock.getHigh(),
                    stock.getLow(),
                    stock.getClose(),
                    stock.getVolume(),
                    stock.getMovingAverage() != null ? stock.getMovingAverage() : "",
                    stock.getRsi() != null ? stock.getRsi() : ""
                );
            }
        }

        logger.info("Export complete: {}", filePath);
    }

    /**
     * Exports prediction results to a CSV file.
     *
     * @param predictions the predictions to export
     * @param filePath    destination file path
     * @param metadata    optional metadata to include as comments
     * @throws IOException if writing fails
     */
    public void exportPredictions(List<PredictionResult> predictions, Path filePath, 
                                   String metadata) throws IOException {
        logger.info("Exporting {} predictions to: {}", predictions.size(), filePath);

        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            
            // Write metadata as comment
            if (metadata != null && !metadata.isEmpty()) {
                printer.printComment("Metadata: " + metadata);
                printer.printComment("Generated: " + LocalDateTime.now().format(DATETIME_FORMATTER));
            }
            
            // Write headers
            printer.printRecord((Object[]) PREDICTION_HEADERS);

            for (PredictionResult prediction : predictions) {
                printer.printRecord(
                    prediction.getDate().format(DATE_FORMATTER),
                    String.format("%.4f", prediction.getPredictedValue()),
                    prediction.getActualValue() != null ? 
                            String.format("%.4f", prediction.getActualValue()) : "",
                    prediction.getError() != null ? 
                            String.format("%.4f", prediction.getError()) : "",
                    prediction.getPercentageError() != null ? 
                            String.format("%.2f%%", prediction.getPercentageError()) : "",
                    prediction.getModelName(),
                    prediction.getPredictionTimestamp().format(DATETIME_FORMATTER)
                );
            }
        }

        logger.info("Prediction export complete: {}", filePath);
    }

    /**
     * Exports combined stock data and predictions.
     *
     * @param stockData   the historical stock data
     * @param predictions the predictions
     * @param filePath    destination file path
     * @throws IOException if writing fails
     */
    public void exportCombined(List<StockData> stockData, List<PredictionResult> predictions,
                                Path filePath) throws IOException {
        logger.info("Exporting combined data to: {}", filePath);

        String[] headers = {"Date", "Symbol", "ActualClose", "PredictedClose", 
                           "Error", "Open", "High", "Low", "Volume"};

        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, 
                     CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

            for (int i = 0; i < stockData.size(); i++) {
                StockData stock = stockData.get(i);
                PredictionResult prediction = i < predictions.size() ? predictions.get(i) : null;

                printer.printRecord(
                    stock.getDate().format(DATE_FORMATTER),
                    stock.getSymbol(),
                    String.format("%.4f", stock.getClose()),
                    prediction != null ? String.format("%.4f", prediction.getPredictedValue()) : "",
                    prediction != null && prediction.getError() != null ? 
                            String.format("%.4f", prediction.getError()) : "",
                    String.format("%.4f", stock.getOpen()),
                    String.format("%.4f", stock.getHigh()),
                    String.format("%.4f", stock.getLow()),
                    stock.getVolume()
                );
            }
        }

        logger.info("Combined export complete: {}", filePath);
    }

    /**
     * Parses a date string with multiple format attempts.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Try common date formats
        String[] formats = {"yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy/MM/dd"};
        
        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        logger.warn("Could not parse date: {}", dateStr);
        return null;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
