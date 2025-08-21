package com.example.currencyconverter.client;

import com.example.currencyconverter.model.OpenExchangeRatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Slf4j
@Service
public class OpenExchangeRatesClient {
    
    private final WebClient webClient;
    private final String apiKey;
    
    public OpenExchangeRatesClient(WebClient.Builder webClientBuilder,
                                  @Value("${api.openexchange.base-url}") String baseUrl,
                                  @Value("${api.openexchange.key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }
    
    public Mono<BigDecimal> getExchangeRate(String fromCurrency, String toCurrency) {
        log.info("Fetching exchange rate from OpenExchangeRates: {} to {}", fromCurrency, toCurrency);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/latest.json")
                        .queryParam("app_id", apiKey)
                        .queryParam("base", fromCurrency)
                        .build())
                .retrieve()
                .bodyToMono(OpenExchangeRatesResponse.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    BigDecimal rate = response.getRates().get(toCurrency);
                    if (rate == null) {
                        throw new RuntimeException("Currency not supported: " + toCurrency);
                    }
                    log.info("OpenExchangeRates returned rate: {}", rate);
                    return rate;
                })
                .doOnError(error -> log.error("OpenExchangeRates API call failed", error));
    }
}