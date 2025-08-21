package com.example.currencyconverter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
public class CurrencyConversionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Currency Converter Service is running!"));
    }
    
    @Test
    public void testSameCurrencyConversion() throws Exception {
        mockMvc.perform(get("/api/v1/convert")
                .param("amount", "100")
                .param("from", "USD")
                .param("to", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedAmount").value(100.00));
    }
}