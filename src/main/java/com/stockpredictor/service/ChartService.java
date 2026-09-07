package com.stockpredictor.service;

import com.stockpredictor.model.BacktestResult;
import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for creating and managing JavaFX charts for stock visualization.
 * Supports line charts for prices, predictions, and indicators.
 *
 * @author StockPredictor Team
 * @version 1.0.0
 */
public class ChartService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChartService.class);

    /**
     * Creates a line chart for stock price data.
     *
     * @param data  the stock data to display
     * @param title chart title
     * @return configured LineChart
     */
    public LineChart<Number, Number> createPriceChart(List<StockData> data, String title) {
        logger.debug("Creating price chart with {} data points", data.size());

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dani");
        xAxis.setAutoRanging(true);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Cijena ($)");
        yAxis.setAutoRanging(true);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        XYChart.Series<Number, Number> priceSeries = createPriceSeries(data, "Zaključna cijena");
        chart.getData().add(priceSeries);

        return chart;
    }

    /**
     * Creates a chart comparing predicted vs actual prices.
     *
     * @param predictions list of prediction results
     * @param title       chart title
     * @return configured LineChart with both series
     */
    public LineChart<Number, Number> createPredictionChart(List<PredictionResult> predictions, 
                                                            String title) {
        logger.debug("Creating prediction chart with {} predictions", predictions.size());

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dani");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Cijena ($)");
        yAxis.setAutoRanging(true);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        // Actual prices series
        XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Stvarna cijena");

        // Predicted prices series
        XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("Predviđena cijena");

        for (int i = 0; i < predictions.size(); i++) {
            PredictionResult pred = predictions.get(i);
            predictedSeries.getData().add(new XYChart.Data<>(i, pred.getPredictedValue()));
            
            if (pred.getActualValue() != null) {
                actualSeries.getData().add(new XYChart.Data<>(i, pred.getActualValue()));
            }
        }

        chart.getData().addAll(actualSeries, predictedSeries);
        
        return chart;
    }

    /**
     * Creates a chart with stock price and optional indicators.
     *
     * @param data          stock data
     * @param showMA        whether to show moving average
     * @param title         chart title
     * @return configured LineChart
     */
    public LineChart<Number, Number> createChartWithIndicators(List<StockData> data,
                                                                boolean showMA, String title) {
        logger.debug("Creating chart with indicators (MA: {})", showMA);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dani");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Cijena ($)");
        yAxis.setAutoRanging(true);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        // Price series
        XYChart.Series<Number, Number> priceSeries = createPriceSeries(data, "Zaključna cijena");
        chart.getData().add(priceSeries);

        // Moving Average series
        if (showMA) {
            XYChart.Series<Number, Number> maSeries = new XYChart.Series<>();
            maSeries.setName("Pomični prosjek");

            for (int i = 0; i < data.size(); i++) {
                Double ma = data.get(i).getMovingAverage();
                if (ma != null) {
                    maSeries.getData().add(new XYChart.Data<>(i, ma));
                }
            }

            chart.getData().add(maSeries);
        }

        return chart;
    }

    /**
     * Creates an error metrics chart from backtest results.
     *
     * @param result the backtest result
     * @return chart showing prediction errors over time
     */
    public LineChart<Number, Number> createErrorChart(BacktestResult result) {
        logger.debug("Creating error chart for {}", result.getModelName());

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dani");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Pogreška");
        yAxis.setAutoRanging(true);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Pogreška predikcije kroz vrijeme");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        XYChart.Series<Number, Number> errorSeries = new XYChart.Series<>();
        errorSeries.setName("Apsolutna pogreška");

        List<PredictionResult> predictions = result.getPredictions();
        for (int i = 0; i < predictions.size(); i++) {
            Double error = predictions.get(i).getError();
            if (error != null) {
                errorSeries.getData().add(new XYChart.Data<>(i, error));
            }
        }

        chart.getData().add(errorSeries);
        
        return chart;
    }

    /**
     * Creates a profit/loss chart from backtest results.
     *
     * @param result            the backtest result
     * @param initialInvestment starting capital
     * @return chart showing portfolio value over time
     */
    public LineChart<Number, Number> createProfitChart(BacktestResult result, 
                                                        double initialInvestment) {
        logger.debug("Creating profit chart for {}", result.getModelName());

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dani");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Vrijednost portfelja ($)");
        yAxis.setAutoRanging(true);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Kretanje portfelja");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        XYChart.Series<Number, Number> portfolioSeries = new XYChart.Series<>();
        portfolioSeries.setName("Vrijednost portfelja");

        // Simulate portfolio value from predictions
        double portfolioValue = initialInvestment;
        List<PredictionResult> predictions = result.getPredictions();

        portfolioSeries.getData().add(new XYChart.Data<>(0, portfolioValue));

        for (int i = 0; i < predictions.size(); i++) {
            PredictionResult pred = predictions.get(i);
            if (pred.getActualValue() != null && i > 0) {
                PredictionResult prevPred = predictions.get(i - 1);
                if (prevPred.getActualValue() != null) {
                    double change = pred.getActualValue() / prevPred.getActualValue();
                    portfolioValue *= change;
                }
            }
            portfolioSeries.getData().add(new XYChart.Data<>(i + 1, portfolioValue));
        }

        chart.getData().add(portfolioSeries);

        // Add baseline
        XYChart.Series<Number, Number> baselineSeries = new XYChart.Series<>();
        baselineSeries.setName("Početni ulog");
        baselineSeries.getData().add(new XYChart.Data<>(0, initialInvestment));
        baselineSeries.getData().add(new XYChart.Data<>(predictions.size(), initialInvestment));
        chart.getData().add(baselineSeries);

        return chart;
    }

    /**
     * Creates an RSI chart.
     *
     * @param data  stock data with RSI values
     * @param title chart title
     * @return chart showing RSI over time
     */
    public LineChart<Number, Number> createRSIChart(List<StockData> data, String title) {
        logger.debug("Creating RSI chart with {} data points", data.size());

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dani");

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("RSI");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        XYChart.Series<Number, Number> rsiSeries = new XYChart.Series<>();
        rsiSeries.setName("RSI");

        for (int i = 0; i < data.size(); i++) {
            Double rsi = data.get(i).getRsi();
            if (rsi != null) {
                rsiSeries.getData().add(new XYChart.Data<>(i, rsi));
            }
        }

        chart.getData().add(rsiSeries);

        // Add overbought/oversold lines
        XYChart.Series<Number, Number> overbought = new XYChart.Series<>();
        overbought.setName("Prekupljenost (70)");
        overbought.getData().add(new XYChart.Data<>(0, 70));
        overbought.getData().add(new XYChart.Data<>(data.size() - 1, 70));

        XYChart.Series<Number, Number> oversold = new XYChart.Series<>();
        oversold.setName("Preprodanost (30)");
        oversold.getData().add(new XYChart.Data<>(0, 30));
        oversold.getData().add(new XYChart.Data<>(data.size() - 1, 30));

        chart.getData().addAll(overbought, oversold);

        return chart;
    }

    /**
     * Updates an existing chart with new data.
     *
     * @param chart the chart to update
     * @param data  new stock data
     */
    public void updateChart(LineChart<Number, Number> chart, List<StockData> data) {
        logger.debug("Updating chart with {} new data points", data.size());

        chart.getData().clear();
        XYChart.Series<Number, Number> series = createPriceSeries(data, "Zaključna cijena");
        chart.getData().add(series);
    }

    /**
     * Creates a price series from stock data.
     */
    private XYChart.Series<Number, Number> createPriceSeries(List<StockData> data, String name) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(name);

        for (int i = 0; i < data.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, data.get(i).getClose()));
        }

        return series;
    }

    /**
     * Applies styling to a chart.
     *
     * @param chart the chart to style
     */
    public void applyDefaultStyle(LineChart<Number, Number> chart) {
        chart.setLegendVisible(true);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(true);
    }
}
