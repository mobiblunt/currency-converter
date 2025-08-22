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
        log.info("Getting exchange rate with fallback: {} to {}", fromCurrency, toCurrency);
        
        // Try primary API first
        try {
            BigDecimal rate = exchangeRateApiClient.getExchangeRate(fromCurrency, toCurrency)
                    .block();
            return new RateResponse(rate, "ExchangeRate-API");
        } catch (Exception e) {
            log.warn("Primary API (ExchangeRate-API) failed, trying fallback", e);
            
            // Try secondary API
            try {
                BigDecimal rate = openExchangeRatesClient.getExchangeRate(fromCurrency, toCurrency)
                    .block();
                return new RateResponse(rate, "OpenExchangeRates");
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
        CompletableFuture<RateResponse> primaryCall = exchangeRateApiClient
                .getExchangeRate(fromCurrency, toCurrency)
                .map(rate -> new RateResponse(rate, "ExchangeRate-API"))
                .toFuture();
                
        CompletableFuture<RateResponse> fallbackCall = openExchangeRatesClient
                .getExchangeRate(fromCurrency, toCurrency)
                .map(rate -> new RateResponse(rate, "OpenExchangeRates"))
                .toFuture();
        
        // Use the first successful result
        return primaryCall
                .handle((rateResponse, throwable) -> {
                    if (throwable == null) {
                        return CompletableFuture.completedFuture(rateResponse);
                    } else {
                        log.warn("Primary API failed, using fallback", throwable);
                        return fallbackCall;
                    }
                })
                .thenCompose(future -> future)
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