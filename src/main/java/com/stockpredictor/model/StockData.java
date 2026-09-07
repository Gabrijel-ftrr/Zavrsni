package com.stockpredictor.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a single stock data point with OHLCV (Open, High, Low, Close, Volume) data.
 * This is the fundamental data structure for stock price information.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class StockData {
    
    private final LocalDate date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;
    private final String symbol;
    
    // Optional computed features
    private Double movingAverage;
    private Double rsi;
    private Double normalizedClose;

    /**
     * Constructs a new StockData instance.
     *
     * @param date   the date of the stock data
     * @param symbol the stock ticker symbol
     * @param open   the opening price
     * @param high   the highest price of the day
     * @param low    the lowest price of the day
     * @param close  the closing price
     * @param volume the trading volume
     */
    public StockData(LocalDate date, String symbol, double open, double high, 
                     double low, double close, long volume) {
        this.date = Objects.requireNonNull(date, "Date cannot be null");
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    /**
     * Creates a copy of this StockData with computed features.
     *
     * @param movingAverage  the computed moving average
     * @param rsi            the computed RSI value
     * @param normalizedClose the normalized close price
     * @return a new StockData instance with computed features
     */
    public StockData withFeatures(Double movingAverage, Double rsi, Double normalizedClose) {
        StockData copy = new StockData(date, symbol, open, high, low, close, volume);
        copy.movingAverage = movingAverage;
        copy.rsi = rsi;
        copy.normalizedClose = normalizedClose;
        return copy;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public long getVolume() { return volume; }
    public String getSymbol() { return symbol; }
    public Double getMovingAverage() { return movingAverage; }
    public Double getRsi() { return rsi; }
    public Double getNormalizedClose() { return normalizedClose; }

    /**
     * Sets the moving average value.
     * @param movingAverage the moving average to set
     */
    public void setMovingAverage(Double movingAverage) {
        this.movingAverage = movingAverage;
    }

    /**
     * Sets the RSI value.
     * @param rsi the RSI value to set
     */
    public void setRsi(Double rsi) {
        this.rsi = rsi;
    }

    /**
     * Sets the normalized close price.
     * @param normalizedClose the normalized close price to set
     */
    public void setNormalizedClose(Double normalizedClose) {
        this.normalizedClose = normalizedClose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockData stockData = (StockData) o;
        return Double.compare(stockData.close, close) == 0 &&
               date.equals(stockData.date) &&
               symbol.equals(stockData.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, symbol, close);
    }

    @Override
    public String toString() {
        return String.format("StockData{date=%s, symbol='%s', close=%.2f, volume=%d}",
                date, symbol, close, volume);
    }
}
