package com.example.currencyconverter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class OpenExchangeRatesResponse {
    @JsonProperty("base")
    private String base;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("rates")
    private Map<String, BigDecimal> rates;
}