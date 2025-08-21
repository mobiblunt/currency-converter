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
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {
    
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final OpenExchangeRatesClient openExchangeRatesClient;
    
    public CurrencyConversionResponse convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return createResponse(amount, fromCurrency, toCurrency, BigDecimal.ONE, amount, "SAME_CURRENCY");
        }
        
        try {
            // Try primary API first, fall back to secondary if needed
            BigDecimal exchangeRate = getExchangeRateWithFallback(fromCurrency, toCurrency);
            BigDecimal convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            
            return createResponse(amount, fromCurrency, toCurrency, exchangeRate, convertedAmount, "API");
        } catch (Exception e) {
            log.error("All currency conversion attempts failed", e);
            throw new CurrencyConversionException("Unable to convert currency: " + e.getMessage());
        }
    }
    
    @Cacheable(value = "exchangeRates", key = "#fromCurrency + '-' + #toCurrency")
    public BigDecimal getExchangeRateWithFallback(String fromCurrency, String toCurrency) {
        log.info("Getting exchange rate with fallback: {} to {}", fromCurrency, toCurrency);
        
        // Try primary API first
        try {
            return exchangeRateApiClient.getExchangeRate(fromCurrency, toCurrency)
                    .block();
        } catch (Exception e) {
            log.warn("Primary API (ExchangeRate-API) failed, trying fallback", e);
            
            // Try secondary API
            try {
                return openExchangeRatesClient.getExchangeRate(fromCurrency, toCurrency)
                        .block();
            } catch (Exception fallbackError) {
                log.error("Fallback API (OpenExchangeRates) also failed", fallbackError);
                throw new CurrencyConversionException("Both primary and fallback APIs failed");
            }
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
        CompletableFuture<BigDecimal> primaryCall = exchangeRateApiClient
                .getExchangeRate(fromCurrency, toCurrency)
                .toFuture();
                
        CompletableFuture<BigDecimal> fallbackCall = openExchangeRatesClient
                .getExchangeRate(fromCurrency, toCurrency)
                .toFuture();
        
        // Use the first successful result
        return primaryCall
                .handle((rate, throwable) -> {
                    if (throwable == null) {
                        return CompletableFuture.completedFuture(rate);
                    } else {
                        log.warn("Primary API failed, using fallback", throwable);
                        return fallbackCall;
                    }
                })
                .thenCompose(future -> future)
                .thenApply(exchangeRate -> {
                    BigDecimal convertedAmount = amount.multiply(exchangeRate)
                            .setScale(2, RoundingMode.HALF_UP);
                    return createResponse(amount, fromCurrency, toCurrency, exchangeRate, convertedAmount, "API");
                })
                .exceptionally(throwable -> {
                    log.error("All async conversion attempts failed", throwable);
                    throw new CurrencyConversionException("Unable to convert currency: " + throwable.getMessage());
                });
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