package com.example.currencyconverter.service;

import com.example.currencyconverter.client.ExchangeRateApiClient;
import com.example.currencyconverter.client.OpenExchangeRatesClient;
import com.example.currencyconverter.dto.CurrencyConversionResponse;
import com.example.currencyconverter.exception.CurrencyConversionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;

import com.example.currencyconverter.dto.ExchangeRateHistory;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
    
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final OpenExchangeRatesClient openExchangeRatesClient;
    
    // Store rate history for the last 24 hours: Map<BaseCurrency, Map<Timestamp, Map<TargetCurrency, Rate>>>
    private final Map<String, Map<LocalDateTime, Map<String, BigDecimal>>> rateHistory = new ConcurrentHashMap<>();
    
    private final List<String> popularCurrencies = Arrays.asList(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD",
        "MXN", "SGD", "HKD", "NOK", "KRW", "TRY", "RUB", "INR", "BRL", "ZAR"
    );
    
    public CurrencyConversionResponse convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return createResponse(amount, fromCurrency, toCurrency, BigDecimal.ONE, amount, "SAME_CURRENCY");
        }
        
        try {
            // Try primary API first, fall back to secondary if needed
            RateResponse rateResponse = getExchangeRateWithFallback(fromCurrency, toCurrency);
            BigDecimal convertedAmount = amount.multiply(rateResponse.rate).setScale(2, RoundingMode.HALF_UP);
            
            return createResponse(amount, fromCurrency, toCurrency, rateResponse.rate, convertedAmount, rateResponse.provider);
        } catch (Exception e) {
            log.error("All currency conversion attempts failed", e);
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
        log.info("Getting exchange rates from multiple APIs: {} to {}", fromCurrency, toCurrency);
        
        BigDecimal exchangeRateApiRate = null;
        BigDecimal openExchangeRate = null;
        
        // Try ExchangeRate API
        try {
            exchangeRateApiRate = exchangeRateApiClient.getExchangeRate(fromCurrency, toCurrency)
                    .block();
            log.info("ExchangeRate-API rate: {}", exchangeRateApiRate);
        } catch (Exception e) {
            log.warn("ExchangeRate-API failed", e);
        }
        
        // Try OpenExchange API
        try {
            openExchangeRate = openExchangeRatesClient.getExchangeRate(fromCurrency, toCurrency)
                    .block();
            log.info("OpenExchangeRates rate: {}", openExchangeRate);
        } catch (Exception e) {
            log.warn("OpenExchangeRates failed", e);
        }
        
        // Determine final rate and provider
        if (exchangeRateApiRate != null && openExchangeRate != null) {
            // Calculate average if both APIs returned data
            BigDecimal averageRate = exchangeRateApiRate.add(openExchangeRate)
                    .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            log.info("Using average rate from both APIs: {}", averageRate);
            return new RateResponse(averageRate, "Average (ExchangeRate-API + OpenExchangeRates)");
        } else if (exchangeRateApiRate != null) {
            // Use ExchangeRate API rate
            return new RateResponse(exchangeRateApiRate, "ExchangeRate-API");
        } else if (openExchangeRate != null) {
            // Use OpenExchange rate
            return new RateResponse(openExchangeRate, "OpenExchangeRates");
        } else {
            // Both APIs failed
            throw new CurrencyConversionException("Both APIs failed to provide exchange rates");
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
                        log.warn("ExchangeRate-API failed", error);
                        return null;
                    }
                    return rate;
                });
                
        CompletableFuture<BigDecimal> openExchangeFuture = openExchangeRatesClient
                .getExchangeRate(fromCurrency, toCurrency)
                .toFuture()
                .handle((rate, error) -> {
                    if (error != null) {
                        log.warn("OpenExchangeRates failed", error);
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
                        throw new CurrencyConversionException("Both APIs failed to provide exchange rates");
                    }
                })
                .thenApply(rateResponse -> {
                    BigDecimal convertedAmount = amount.multiply(rateResponse.rate)
                            .setScale(2, RoundingMode.HALF_UP);
                    return createResponse(amount, fromCurrency, toCurrency, rateResponse.rate, convertedAmount, rateResponse.provider);
                })
                .exceptionally(throwable -> {
                    log.error("All async conversion attempts failed", throwable);
                    throw new CurrencyConversionException("Unable to convert currency: " + throwable.getMessage());
                });
    }
    
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void updateRateHistory() {
        log.info("Updating rate history");
        LocalDateTime now = LocalDateTime.now();
        
        for (String baseCurrency : popularCurrencies) {
            try {
                // Get current rates for the base currency
                RateResponse rateResponse = getExchangeRateWithFallback(baseCurrency, "USD"); // Using USD as reference
                Map<String, BigDecimal> currentRates = new HashMap<>();
                
                for (String targetCurrency : popularCurrencies) {
                    if (!targetCurrency.equals(baseCurrency)) {
                        try {
                            RateResponse targetResponse = getExchangeRateWithFallback(baseCurrency, targetCurrency);
                            currentRates.put(targetCurrency, targetResponse.rate);
                        } catch (Exception e) {
                            log.warn("Failed to get rate for {}/{}", baseCurrency, targetCurrency, e);
                        }
                    }
                }
                
                // Update history
                rateHistory.computeIfAbsent(baseCurrency, k -> new ConcurrentHashMap<>())
                          .put(now, currentRates);
                
                // Remove entries older than 24 hours
                removeOldEntries(baseCurrency);
                
            } catch (Exception e) {
                log.error("Failed to update rate history for {}", baseCurrency, e);
            }
        }
    }
    
    private void removeOldEntries(String baseCurrency) {
        LocalDateTime cutoff = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        Map<LocalDateTime, Map<String, BigDecimal>> currencyHistory = rateHistory.get(baseCurrency);
        if (currencyHistory != null) {
            currencyHistory.keySet().removeIf(timestamp -> timestamp.isBefore(cutoff));
        }
    }
    
    public ExchangeRateHistory getRateHistory(String baseCurrency) {
        Map<LocalDateTime, Map<String, BigDecimal>> history = rateHistory.get(baseCurrency);
        if (history == null || history.isEmpty()) {
            throw new CurrencyConversionException("No rate history available for " + baseCurrency);
        }
        
        // Sort by timestamp
        TreeMap<LocalDateTime, Map<String, BigDecimal>> sortedHistory = new TreeMap<>(history);
        
        return new ExchangeRateHistory(
            baseCurrency,
            LocalDateTime.now(),
            sortedHistory
        );
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