package com.example.currencyconverter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateHistory {
    private String base;
    private LocalDateTime timestamp;
    private Map<LocalDateTime, Map<String, BigDecimal>> rates;
}
