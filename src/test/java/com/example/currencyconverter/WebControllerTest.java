package com.example.currencyconverter;

import com.example.currencyconverter.controller.WebController;
import com.example.currencyconverter.service.CurrencyConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebController.class)
public class WebControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CurrencyConversionService currencyConversionService;
    
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
        mockMvc.perform(post("/convert")
                .param("amount", "-1")
                .param("fromCurrency", "USD")
                .param("toCurrency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().hasErrors());
    }
}