package com.example.currencyconverter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
import org.hamcrest.Matchers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import com.example.currencyconverter.controller.CurrencyConversionController;
import com.example.currencyconverter.service.CurrencyConversionService;
import com.example.currencyconverter.dto.CurrencyConversionResponse;
import com.example.currencyconverter.client.ExchangeRateApiClient;
import com.example.currencyconverter.client.OpenExchangeRatesClient;

@WebMvcTest(CurrencyConversionController.class)
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
    public void testGetCurrencies() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currencies", hasSize(22)))
                .andExpect(jsonPath("$.count").value(22))
                .andExpect(jsonPath("$.currencies", hasItems("USD", "EUR", "GBP", "JPY")));
    }

    // @Test
    // public void testConvertCurrency() throws Exception {
    //     CurrencyConversionResponse mockResponse = new CurrencyConversionResponse(
    //         new BigDecimal("100.00"),
    //         "USD",
    //         "EUR",
    //         new BigDecimal("0.85"),
    //         new BigDecimal("85.00"),
    //         "ExchangeRate-API",
    //         LocalDateTime.now()
    //     );

    //     when(currencyConversionService.convertCurrency(
    //         any(BigDecimal.class),
    //         eq("USD"),
    //         eq("EUR")
    //     )).thenReturn(mockResponse);

    //     mockMvc.perform(get("/api/v1/convert")
    //             .param("amount", "100.00")
    //             .param("from", "USD")
    //             .param("to", "EUR"))
    //             .andExpect(status().isOk())
    //             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(jsonPath("$.amount", Matchers.comparesEqualTo(100.00)))
    //             .andExpect(jsonPath("$.fromCurrency").value("USD"))
    //             .andExpect(jsonPath("$.toCurrency").value("EUR"))
    //             .andExpect(jsonPath("$.convertedAmount", Matchers.comparesEqualTo(85.00)))
    //             .andExpect(jsonPath("$.provider").value("ExchangeRate-API"));
    // }

    // @Test
    // public void testConvertCurrencyAsync() throws Exception {
    //     CurrencyConversionResponse mockResponse = new CurrencyConversionResponse(
    //         new BigDecimal("100.00"),
    //         "USD",
    //         "EUR",
    //         new BigDecimal("0.85"),
    //         new BigDecimal("85.00"),
    //         "ExchangeRate-API",
    //         LocalDateTime.now()
    //     );

    //     when(currencyConversionService.convertCurrencyAsync(
    //         any(BigDecimal.class),
    //         eq("USD"),
    //         eq("EUR")
    //     )).thenReturn(CompletableFuture.completedFuture(mockResponse));

    //     mockMvc.perform(get("/api/v1/convert-async")
    //             .param("amount", "100.00")
    //             .param("from", "USD")
    //             .param("to", "EUR"))
    //             .andExpect(status().isOk())
    //             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(jsonPath("$.amount", Matchers.comparesEqualTo(100.00)))
    //             .andExpect(jsonPath("$.fromCurrency").value("USD"))
    //             .andExpect(jsonPath("$.toCurrency").value("EUR"))
    //             .andExpect(jsonPath("$.convertedAmount", Matchers.comparesEqualTo(85.00)))
    //             .andExpect(jsonPath("$.provider").value("ExchangeRate-API"));
    // }

    // @Test
    // public void testInvalidAmount() throws Exception {
    //     mockMvc.perform(get("/api/v1/convert")
    //             .param("amount", "-100.00")
    //             .param("from", "USD")
    //             .param("to", "EUR"))
    //             .andExpect(status().isBadRequest());
    // }

    // @Test
    // public void testInvalidCurrencyCode() throws Exception {
    //     mockMvc.perform(get("/api/v1/convert")
    //             .param("amount", "100.00")
    //             .param("from", "INVALID")
    //             .param("to", "EUR"))
    //             .andExpect(status().isBadRequest());
    // }

    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Currency Converter Service is running!"));
    }
}