package com.stockpredictor.config;

import com.stockpredictor.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Upravitelj servisa dependency injection.
 * Upravlja životnim ciklusom svih Aplikacijskih servisa.
 *
 * @author Gabrijel Matanović
 * @version 1.0.0
 */
public class ServiceProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceProvider.class);
    private static ServiceProvider instance;

    private final AlphaVantageDataService alphaVantageDataService;
    private final CSVService csvService;
    private final DataPreparationService dataPreparationService;
    private final ModelTrainerService modelTrainerService;
    private final BacktestingService backtestingService;
    private final ChartService chartService;

    private ServiceProvider() {
        logger.info("Initializing services...");
        
        // Initialize services in dependency order
        this.alphaVantageDataService = new AlphaVantageDataService();
        this.csvService = new CSVService();
        this.dataPreparationService = new DataPreparationService();
        this.modelTrainerService = new ModelTrainerService();
        this.backtestingService = new BacktestingService(modelTrainerService);
        this.chartService = new ChartService();
        
        logger.info("All services initialized successfully");
    }

    /**
     * Uzima singleton instancu.
     *
     * @return  ServiceProvider instancu
     */
    public static synchronized ServiceProvider getInstance() {
        if (instance == null) {
            instance = new ServiceProvider();
        }
        return instance;
    }

    /**
     * uzima Alpha Vantage data servis.
     *
     * @return AlphaVantageDataService instancu
     */
    public AlphaVantageDataService getAlphaVantageDataService() {
        return alphaVantageDataService;
    }

    /**
     * uzima CSV import/export servis.
     *
     * @return CSVService instancu
     */
    public CSVService getCsvService() {
        return csvService;
    }

    /**
     * uzoma data preparation servis.
     *
     * @return DataPreparationService instancu
     */
    public DataPreparationService getDataPreparationService() {
        return dataPreparationService;
    }

    /**
     * uzima model trainer servis.
     *
     * @return ModelTrainerService instancu
     */
    public ModelTrainerService getModelTrainerService() {
        return modelTrainerService;
    }

    /**
     * uzimabacktesting servis.
     *
     * @return BacktestingService instancu
     */
    public BacktestingService getBacktestingService() {
        return backtestingService;
    }

    /**
     * uzima chart servis.
     *
     * @return ChartService instancu
     */
    public ChartService getChartService() {
        return chartService;
    }

    /**
     * Gasi sve servise.
     */
    public void shutdown() {
        logger.info("Shutting down services...");
        
        try {
            alphaVantageDataService.shutdown();
            modelTrainerService.shutdown();
        } catch (Exception e) {
            logger.error("Error during service shutdown: {}", e.getMessage());
        }
        
        logger.info("Services shutdown complete");
    }

    /**
     * resetira singleton instancu (za test).
     */
    public static void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}
