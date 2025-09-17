package com.example.currencyconverter.controller;
import com.example.currencyconverter.dto.ExchangeRateHistory;
import com.example.currencyconverter.dto.CurrencyConversionResponse;
import com.example.currencyconverter.dto.CurrenciesResponse;
import com.example.currencyconverter.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurrencyConversionController {
    
    private final CurrencyConversionService currencyConversionService;
    
    // List of supported currencies
    private final List<String> supportedCurrencies = Arrays.asList(
            "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD", "NGN", "PLN",
            "MXN", "SGD", "HKD", "NOK", "KRW", "TRY", "RUB", "INR", "BRL", "ZAR"
    );
    
    @GetMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @RequestParam @DecimalMin(value = "0.01", message = "Amount must be greater than 0") 
            BigDecimal amount,
            
            @RequestParam @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
            String from,
            
            @RequestParam @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")  
            String to) {
        
        log.info("Currency conversion request: {} {} to {}", amount, from, to);
        
        CurrencyConversionResponse response = currencyConversionService.convertCurrency(amount, from, to);
        
        log.info("Currency conversion successful: {} {} = {} {}", 
                amount, from, response.getConvertedAmount(), to);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/convert-async")
    public CompletableFuture<ResponseEntity<CurrencyConversionResponse>> convertCurrencyAsync(
            @RequestParam @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount,
            
            @RequestParam @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
            String from,
            
            @RequestParam @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
            String to) {
        
        log.info("Async currency conversion request: {} {} to {}", amount, from, to);
        
        return currencyConversionService.convertCurrencyAsync(amount, from, to)
                .thenApply(ResponseEntity::ok);
    }
    
    @GetMapping("/rates")
    public ResponseEntity<ExchangeRateHistory> getRateHistory(
            @RequestParam @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency code must be 3 uppercase letters")
            String base) {
        
        log.info("Fetching rate history for base currency: {}", base);
        ExchangeRateHistory history = currencyConversionService.getRateHistory(base);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/currencies")
    public ResponseEntity<CurrenciesResponse> getSupportedCurrencies() {
        log.info("Fetching list of supported currencies");
        CurrenciesResponse response = new CurrenciesResponse(supportedCurrencies, supportedCurrencies.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Currency Converter Service is running!");
    }
}