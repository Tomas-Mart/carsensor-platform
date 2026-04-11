package com.carsensor.gateway.presentation.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<String> authFallback() {
        return Mono.just("Auth service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/fallback/cars")
    public Mono<String> carsFallback() {
        return Mono.just("Car service is temporarily unavailable. Please try again later.");
    }

    @RequestMapping("/fallback/scheduler")
    public Mono<String> schedulerFallback() {
        return Mono.just("Scheduler service is temporarily unavailable. Please try again later.");
    }
}