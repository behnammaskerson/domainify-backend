package com.domainify.controller;

import com.domainify.dto.CurrencyRatesDto;
import com.domainify.service.CurrencyRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/currency")
public class CurrencyController {

    private final CurrencyRateService currencyRateService;

    public CurrencyController(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    /** Cached FX sell rates (IRT per foreign unit). Authenticated users only. */
    @GetMapping("/rates")
    public ResponseEntity<CurrencyRatesDto> rates() {
        return ResponseEntity.ok(currencyRateService.getRates());
    }
}
