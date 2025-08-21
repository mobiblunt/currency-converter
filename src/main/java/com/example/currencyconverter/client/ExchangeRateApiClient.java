package com.example.currencyconverter.client;

import com.example.currencyconverter.model.ExchangeRateApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
public class ExchangeRateApiClient {
    
    private final WebClient webClient;
    private final String apiKey;
    
    public ExchangeRateApiClient(WebClient.Builder webClientBuilder,
                                @Value("${api.exchangerate.base-url}") String baseUrl,
                                @Value("${api.exchangerate.key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }
    
    public Mono<BigDecimal> getExchangeRate(String fromCurrency, String toCurrency) {
        log.info("Fetching exchange rate from ExchangeRate-API: {} to {}", fromCurrency, toCurrency);
        
        return webClient.get()
                .uri("/latest/{from}", fromCurrency)
                .retrieve()
                .bodyToMono(ExchangeRateApiResponse.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    BigDecimal rate = response.getRates().get(toCurrency);
                    if (rate == null) {
                        throw new RuntimeException("Currency not supported: " + toCurrency);
                    }
                    log.info("ExchangeRate-API returned rate: {}", rate);
                    return rate;
                })
                .doOnError(error -> log.error("ExchangeRate-API call failed", error));
    }
}