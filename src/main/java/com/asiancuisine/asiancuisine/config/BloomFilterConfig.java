package com.asiancuisine.asiancuisine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "recommendation")
@Data
public class BloomFilterConfig {
    private int expectedInsertions = 10000;
    private double falsePositiveRate = 0.01;
    private int expirationHours = 24;  // Bloom filter expiration time
}
