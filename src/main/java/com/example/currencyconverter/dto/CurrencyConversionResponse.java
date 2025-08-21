package com.example.currencyconverter.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversionResponse {
    private BigDecimal amount;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private String provider;
    private LocalDateTime timestamp;
}
