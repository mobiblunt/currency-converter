package com.example.currencyconverter.service;

import com.example.currencyconverter.client.ExchangeRateApiClient;
import com.example.currencyconverter.client.OpenExchangeRatesClient;
import com.example.currencyconverter.dto.CurrencyConversionResponse;
import com.example.currencyconverter.exception.CurrencyConversionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.example.currencyconverter.dto.ExchangeRateHistory;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
    
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final OpenExchangeRatesClient openExchangeRatesClient;
    
    // Store conversion history: Map<CurrencyPair, Map<Timestamp, Rate>>
    private final Map<String, Map<LocalDateTime, BigDecimal>> conversionHistory = new ConcurrentHashMap<>();

    
    
    public CurrencyConversionResponse convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return createResponse(amount, fromCurrency, toCurrency, BigDecimal.ONE, amount, "SAME_CURRENCY");
        }
        
        try {
            // Get exchange rate with fallback
            RateResponse rateResponse = getExchangeRateWithFallback(fromCurrency, toCurrency);
            BigDecimal convertedAmount = amount.multiply(rateResponse.rate).setScale(2, RoundingMode.HALF_UP);
            
            // Store the rate in history
            storeConversionRate(fromCurrency, toCurrency, rateResponse.rate);
            
            return createResponse(amount, fromCurrency, toCurrency, rateResponse.rate, convertedAmount, rateResponse.provider);
        } catch (Exception e) {
            log.error("Currency conversion failed for {} {} to {}: {}", amount, fromCurrency, toCurrency, e.getMessage());
            throw new CurrencyConversionException("Unable to convert currency: " + e.getMessage());
        }
    }
    
    private static class RateResponse {
        final BigDecimal rate;
        final String provider;
        
        RateResponse(BigDecimal rate, String provider) {
            this.rate = rate;
            this.provider = provider;
        }
    }

    @Cacheable(value = "exchangeRates", key = "#fromCurrency + '-' + #toCurrency")
    public RateResponse getExchangeRateWithFallback(String fromCurrency, String toCurrency) {
        log.info("Getting exchange rates from APIs: {} to {}", fromCurrency, toCurrency);
        
        BigDecimal exchangeRateApiRate = null;
        BigDecimal openExchangeRate = null;
        
        // Try ExchangeRate API
        try {
            exchangeRateApiRate = exchangeRateApiClient.getExchangeRate(fromCurrency, toCurrency)
                    .block();
            log.debug("ExchangeRate-API rate: {}", exchangeRateApiRate);
        } catch (Exception e) {
            log.warn("ExchangeRate-API failed for {}/{}: {}", fromCurrency, toCurrency, e.getMessage());
        }
        
        // Try OpenExchange API
        try {
            openExchangeRate = openExchangeRatesClient.getExchangeRate(fromCurrency, toCurrency)
                    .block();
            log.debug("OpenExchangeRates rate: {}", openExchangeRate);
        } catch (Exception e) {
            log.warn("OpenExchangeRates failed for {}/{}: {}", fromCurrency, toCurrency, e.getMessage());
        }
        
        // Determine final rate and provider
        if (exchangeRateApiRate != null && openExchangeRate != null) {
            // Calculate average if both APIs returned data
            BigDecimal averageRate = exchangeRateApiRate.add(openExchangeRate)
                    .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            log.info("Using average rate for {}/{}: {}", fromCurrency, toCurrency, averageRate);
            return new RateResponse(averageRate, "Average (ExchangeRate-API + OpenExchangeRates)");
        } else if (exchangeRateApiRate != null) {
            return new RateResponse(exchangeRateApiRate, "ExchangeRate-API");
        } else if (openExchangeRate != null) {
            return new RateResponse(openExchangeRate, "OpenExchangeRates");
        } else {
            throw new CurrencyConversionException("Both APIs failed to provide exchange rates for " + fromCurrency + "/" + toCurrency);
        }
    }
    
    // Asynchronous version for better performance
    public CompletableFuture<CurrencyConversionResponse> convertCurrencyAsync(
            BigDecimal amount, String fromCurrency, String toCurrency) {
        
        if (fromCurrency.equals(toCurrency)) {
            return CompletableFuture.completedFuture(
                createResponse(amount, fromCurrency, toCurrency, BigDecimal.ONE, amount, "SAME_CURRENCY")
            );
        }
        
        // Call both APIs in parallel
        CompletableFuture<BigDecimal> exchangeRateApiFuture = exchangeRateApiClient
                .getExchangeRate(fromCurrency, toCurrency)
                .toFuture()
                .handle((rate, error) -> {
                    if (error != null) {
                        log.warn("ExchangeRate-API failed for {}/{}: {}", fromCurrency, toCurrency, error.getMessage());
                        return null;
                    }
                    return rate;
                });
                
        CompletableFuture<BigDecimal> openExchangeFuture = openExchangeRatesClient
                .getExchangeRate(fromCurrency, toCurrency)
                .toFuture()
                .handle((rate, error) -> {
                    if (error != null) {
                        log.warn("OpenExchangeRates failed for {}/{}: {}", fromCurrency, toCurrency, error.getMessage());
                        return null;
                    }
                    return rate;
                });
        
        // Wait for both results
        return CompletableFuture.allOf(exchangeRateApiFuture, openExchangeFuture)
                .thenApply(v -> {
                    BigDecimal exchangeRateApiRate = exchangeRateApiFuture.join();
                    BigDecimal openExchangeRate = openExchangeFuture.join();
                    
                    if (exchangeRateApiRate != null && openExchangeRate != null) {
                        // Calculate average if both APIs returned data
                        BigDecimal averageRate = exchangeRateApiRate.add(openExchangeRate)
                                .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
                        return new RateResponse(averageRate, "Average (ExchangeRate-API + OpenExchangeRates)");
                    } else if (exchangeRateApiRate != null) {
                        return new RateResponse(exchangeRateApiRate, "ExchangeRate-API");
                    } else if (openExchangeRate != null) {
                        return new RateResponse(openExchangeRate, "OpenExchangeRates");
                    } else {
                        throw new CurrencyConversionException("Both APIs failed to provide exchange rates for " + fromCurrency + "/" + toCurrency);
                    }
                })
                .thenApply(rateResponse -> {
                    BigDecimal convertedAmount = amount.multiply(rateResponse.rate)
                            .setScale(2, RoundingMode.HALF_UP);
                    
                    // Store the rate in history
                    storeConversionRate(fromCurrency, toCurrency, rateResponse.rate);
                    
                    return createResponse(amount, fromCurrency, toCurrency, rateResponse.rate, convertedAmount, rateResponse.provider);
                })
                .exceptionally(throwable -> {
                    log.error("Async conversion failed for {} {} to {}: {}", amount, fromCurrency, toCurrency, throwable.getMessage());
                    throw new CurrencyConversionException("Unable to convert currency: " + throwable.getMessage());
                });
    }
    
    /**
     * Store conversion rate in history for the given currency pair
     */
    private void storeConversionRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        String currencyPair = fromCurrency.toUpperCase() + "/" + toCurrency.toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        
        // Store the rate with timestamp
        conversionHistory.computeIfAbsent(currencyPair, k -> new ConcurrentHashMap<>())
                        .put(now, rate);
        
        // Clean up old entries (older than 24 hours)
        removeOldEntries(currencyPair);
        
        log.debug("Stored conversion rate for {}: {} at {}", currencyPair, rate, now);
    }
    
    /**
     * Remove entries older than 24 hours for a currency pair
     */
    private void removeOldEntries(String currencyPair) {
        LocalDateTime cutoff = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        Map<LocalDateTime, BigDecimal> pairHistory = conversionHistory.get(currencyPair);
        
        if (pairHistory != null) {
            int sizeBefore = pairHistory.size();
            pairHistory.keySet().removeIf(timestamp -> timestamp.isBefore(cutoff));
            int sizeAfter = pairHistory.size();
            
            if (sizeBefore > sizeAfter) {
                log.debug("Removed {} old entries for {}", (sizeBefore - sizeAfter), currencyPair);
            }
        }
    }
    
    /**
     * Get rate history for a base currency
     * Returns all conversion pairs where the base currency was used
     */
    public ExchangeRateHistory getRateHistory(String baseCurrency) {
        String upperBaseCurrency = baseCurrency.toUpperCase();
        
        // Find all currency pairs that start with the base currency
        Map<LocalDateTime, Map<String, BigDecimal>> consolidatedHistory = new TreeMap<>();
        
        conversionHistory.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(upperBaseCurrency + "/"))
                .forEach(entry -> {
                    String currencyPair = entry.getKey();
                    String targetCurrency = currencyPair.substring(currencyPair.indexOf("/") + 1);
                    Map<LocalDateTime, BigDecimal> pairHistory = entry.getValue();
                    
                    // Add each rate to the consolidated history
                    pairHistory.forEach((timestamp, rate) -> {
                        consolidatedHistory.computeIfAbsent(timestamp, k -> new ConcurrentHashMap<>())
                                         .put(targetCurrency, rate);
                    });
                });
        
        if (consolidatedHistory.isEmpty()) {
            log.warn("No conversion history available for base currency: {}", upperBaseCurrency);
            throw new CurrencyConversionException("No conversion history available for " + upperBaseCurrency);
        }
        
        log.info("Retrieved conversion history for {} with {} time points", upperBaseCurrency, consolidatedHistory.size());
        
        return new ExchangeRateHistory(
            upperBaseCurrency,
            LocalDateTime.now(),
            new TreeMap<>(consolidatedHistory)
        );
    }
    
    /**
     * Get all currency pairs that have been converted
     */
    public List<String> getAvailableCurrencyPairs() {
        return new ArrayList<>(conversionHistory.keySet());
    }
    
    /**
     * Get all base currencies that have conversion history
     */
    public List<String> getAvailableBaseCurrencies() {
        return conversionHistory.keySet().stream()
                .map(pair -> pair.substring(0, pair.indexOf("/")))
                .distinct()
                .sorted()
                .toList();
    }
    
    /**
     * Get the latest rate for a specific currency pair
     */
    public BigDecimal getLatestRate(String fromCurrency, String toCurrency) {
        String currencyPair = fromCurrency.toUpperCase() + "/" + toCurrency.toUpperCase();
        Map<LocalDateTime, BigDecimal> pairHistory = conversionHistory.get(currencyPair);
        
        if (pairHistory == null || pairHistory.isEmpty()) {
            throw new CurrencyConversionException("No conversion history available for " + currencyPair);
        }
        
        // Get the most recent rate
        LocalDateTime latestTimestamp = pairHistory.keySet().stream()
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new CurrencyConversionException("No rates available for " + currencyPair));
        
        return pairHistory.get(latestTimestamp);
    }
    
    /**
     * Clear all conversion history (useful for testing)
     */
    public void clearHistory() {
        conversionHistory.clear();
        log.info("Cleared all conversion history");
    }
    
    private CurrencyConversionResponse createResponse(BigDecimal amount, String fromCurrency, 
                                                     String toCurrency, BigDecimal exchangeRate, 
                                                     BigDecimal convertedAmount, String provider) {
        return new CurrencyConversionResponse(
                amount,
                fromCurrency,
                toCurrency,
                exchangeRate,
                convertedAmount,
                provider,
                LocalDateTime.now()
        );
    }
}