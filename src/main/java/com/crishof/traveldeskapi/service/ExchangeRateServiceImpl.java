package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.CurrencyLatestResponse;
import com.crishof.traveldeskapi.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final WebClient webClient;
    private final String apiKey;
    private final Duration timeout;

    public ExchangeRateServiceImpl(
            WebClient.Builder webClientBuilder,
            @Value("${app.free-currency.base-url:https://api.freecurrencyapi.com/v1}") String baseUrl,
            @Value("${app.free-currency.api-key:}") String apiKey,
            @Value("${app.free-currency.timeout:5s}") Duration timeout
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.timeout = timeout;
    }

    @Override
    public Mono<BigDecimal> getExchangeRate(String from, String to) {
        String baseCurrency = normalizeCurrency(from);
        String targetCurrency = normalizeCurrency(to);

        log.info("Fetching exchange rate from {} to {}", baseCurrency, targetCurrency);

        if (!StringUtils.hasText(apiKey)) {
            throw new ExternalServiceException("Exchange rate API key is not configured");
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/latest")
                        .queryParam("apikey", apiKey)
                        .queryParam("base_currency", baseCurrency)
                        .queryParam("currencies", targetCurrency)
                        .build())
                .retrieve()
                .bodyToMono(CurrencyLatestResponse.class)
                .timeout(timeout)
                .map(response -> extractRate(response, targetCurrency))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("Exchange rate API responded with status {} for {} -> {}", ex.getStatusCode(), baseCurrency, targetCurrency);
                    return new ExternalServiceException("Exchange rate provider returned an error response");
                })
                .onErrorMap(WebClientRequestException.class, ex -> {
                    log.error("Exchange rate API request failed for {} -> {}", baseCurrency, targetCurrency, ex);
                    return new ExternalServiceException("Exchange rate provider is unavailable");
                })
                .onErrorMap(java.util.concurrent.TimeoutException.class,
                        ex -> new ExternalServiceException("Exchange rate provider timed out"));
    }


    private BigDecimal extractRate(CurrencyLatestResponse response, String targetCurrency) {
        Map<String, BigDecimal> data = response.data();

        if (data == null || !data.containsKey(targetCurrency) || data.get(targetCurrency) == null) {
            throw new ExternalServiceException("Exchange rate not available for currency " + targetCurrency);
        }

        return data.get(targetCurrency);
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            throw new IllegalArgumentException("Currency code is required");
        }

        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
