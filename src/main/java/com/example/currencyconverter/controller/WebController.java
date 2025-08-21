package com.example.currencyconverter.controller;

import com.example.currencyconverter.dto.CurrencyConversionRequest;
import com.example.currencyconverter.dto.CurrencyConversionResponse;
import com.example.currencyconverter.service.CurrencyConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebController {
    
    private final CurrencyConversionService currencyConversionService;
    
    // List of common currencies for the dropdown
    private final List<String> popularCurrencies = Arrays.asList(
            "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD", "NGN", "PLN",
            "MXN", "SGD", "HKD", "NOK", "KRW", "TRY", "RUB", "INR", "BRL", "ZAR"
    );
    
    @GetMapping("/")
    public String index(Model model) {
        CurrencyConversionRequest request = new CurrencyConversionRequest();
        request.setFromCurrency("USD");
        request.setToCurrency("EUR");
        request.setAmount(new BigDecimal("1.0"));
        model.addAttribute("conversionRequest", request);
        model.addAttribute("currencies", popularCurrencies);
        model.addAttribute("error", null);
        model.addAttribute("conversionResponse", null);
        return "index";
    }
    
    @PostMapping("/convert")
    public String convertCurrency(@Valid @ModelAttribute("conversionRequest") CurrencyConversionRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        
        model.addAttribute("currencies", popularCurrencies);
        model.addAttribute("error", null);
        model.addAttribute("conversionResponse", null);
        
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors in currency conversion request: {}", bindingResult.getAllErrors());
            model.addAttribute("error", "Please check your input values");
            return "index";
        }
        
        try {
            log.info("Processing currency conversion: {} {} to {}", 
                    request.getAmount(), request.getFromCurrency(), request.getToCurrency());
            
            CurrencyConversionResponse response = currencyConversionService.convertCurrency(
                    request.getAmount(), 
                    request.getFromCurrency(), 
                    request.getToCurrency()
            );
            
            model.addAttribute("conversionResponse", response);
            model.addAttribute("success", true);
            
            log.info("Currency conversion successful: {} {} = {} {}", 
                    request.getAmount(), request.getFromCurrency(), 
                    response.getConvertedAmount(), request.getToCurrency());
            
        } catch (Exception e) {
            log.error("Currency conversion failed", e);
            model.addAttribute("error", "Conversion failed: " + e.getMessage());
            model.addAttribute("success", false);
        }
        
        return "index";
    }
    
    @GetMapping("/about")
    public String about() {
        return "about";
    }
}