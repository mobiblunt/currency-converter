package com.example.currencyconverter.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversionRequest {
    
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount = BigDecimal.valueOf(1.0);
    
    @NotBlank(message = "From currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
    private String fromCurrency = "USD";
    
    @NotBlank(message = "To currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
    private String toCurrency = "EUR";
}