package com.example.currencyconverter.controller;

import com.example.currencyconverter.dto.CurrencyConversionResponse;
import com.example.currencyconverter.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurrencyConversionController {
    
    private final CurrencyConversionService currencyConversionService;
    
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
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Currency Converter Service is running!");
    }
}