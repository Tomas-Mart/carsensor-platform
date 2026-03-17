package com.carsensor.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "parser")
public class ParserProperties {
    private int maxPages;
    private String userAgent;
    private int timeout;
    private String schedule;
}