package com.example.currencyconverter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import com.example.currencyconverter.dto.CurrencyConversionResponse;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.example.currencyconverter.service.CurrencyConversionService;
import com.example.currencyconverter.client.ExchangeRateApiClient;
import com.example.currencyconverter.client.OpenExchangeRatesClient;

@WebMvcTest
public class CurrencyConversionControllerTest {
    
    @MockBean
    private CurrencyConversionService currencyConversionService;
    
    @MockBean
    private ExchangeRateApiClient exchangeRateApiClient;
    
    @MockBean
    private OpenExchangeRatesClient openExchangeRatesClient;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("conversionRequest"))
                .andExpect(model().attributeExists("currencies"));
    }
    
    @Test
    public void testAboutPage() throws Exception {
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(view().name("about"));
    }
    
    @Test
    public void testConversionFormValidation() throws Exception {
        CurrencyConversionResponse mockResponse = new CurrencyConversionResponse(
            new BigDecimal("100.00"),
            "USD",
            "USD",
            BigDecimal.ONE,
            new BigDecimal("100.00"),
            "SAME_CURRENCY",
            null
        );

        org.mockito.Mockito.when(currencyConversionService.convertCurrency(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("USD"),
            org.mockito.ArgumentMatchers.eq("USD")
        )).thenReturn(mockResponse);

        mockMvc.perform(post("/convert")
                .param("amount", "100.00")
                .param("fromCurrency", "USD")
                .param("toCurrency", "USD"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("currencies"))
                .andExpect(model().attributeExists("conversionResponse"));
    }
}