package com.crishof.traveldeskapi.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ExchangeRateService {
    Mono<BigDecimal> getExchangeRate(String from, String to);
}
