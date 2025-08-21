package com.example.currencyconverter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ExchangeRateApiResponse {
    @JsonProperty("base")
    private String base;
    
    @JsonProperty("date")
    private String date;
    
    @JsonProperty("rates")
    private Map<String, BigDecimal> rates;
}