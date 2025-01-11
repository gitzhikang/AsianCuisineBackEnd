package com.asiancuisine.asiancuisine.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "recommendation.search")
@Data
public class SearchConfig {
    private int maxAttempts = 3;
    private int initialBatchSize = 20;
    private int timeWindowDays = 90;
    private int decayScaleDays = 7;
    private int decayOffsetDays = 30;
    private double decayFactor = 0.1;
    private float randomWeight = 0.2F;
}
