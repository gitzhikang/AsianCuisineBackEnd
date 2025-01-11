package com.asiancuisine.asiancuisine.service.impl;


import com.asiancuisine.asiancuisine.config.BloomFilterConfig;
import com.asiancuisine.asiancuisine.constant.RedisConstants;
import com.asiancuisine.asiancuisine.service.IBloomFilterService;
import com.google.common.hash.BloomFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.google.common.hash.Funnels;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


@Service
@Slf4j
public class BloomFilterService implements IBloomFilterService {

    private final StringRedisTemplate stringRedisTemplate;

    private final BloomFilterConfig config;

    @Autowired
    public BloomFilterService(StringRedisTemplate stringRedisTemplate, BloomFilterConfig config) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.config = config;
    }

    public BloomFilter<String> getBloomFilter(String userId) {
        String bloomKey = getBloomFilterKey(userId);
        byte[] existingFilter = stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .get(bloomKey.getBytes());

        if (existingFilter != null) {
            try {
                return deserializeBloomFilter(existingFilter);
            } catch (Exception e) {
                log.error("Error deserializing Bloom filter for user {}", userId, e);
                return createNewBloomFilter(userId);
            }
        }

        return createNewBloomFilter(userId);
    }

    public void saveBloomFilter(String userId, BloomFilter<String> filter) {
        try {
            String bloomKey = getBloomFilterKey(userId);
            byte[] serializedFilter = serializeBloomFilter(filter);

            stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .set(bloomKey.getBytes(), serializedFilter);

            // Set expiration time
            stringRedisTemplate.expire(bloomKey, Duration.ofHours(config.getExpirationHours()));
        } catch (Exception e) {
            log.error("Error saving Bloom filter for user {}", userId, e);
        }
    }

    public BloomFilter<String> createNewBloomFilter(String userId) {
        BloomFilter<String> filter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                config.getExpectedInsertions(),
                config.getFalsePositiveRate()
        );
        saveBloomFilter(userId, filter);
        return filter;
    }

    private String getBloomFilterKey(String userId) {
        return RedisConstants.BLOOM_FILTER + userId;
    }

    private byte[] serializeBloomFilter(BloomFilter<String> filter) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(filter);
            return bos.toByteArray();
        }
    }

    private BloomFilter<String> deserializeBloomFilter(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (BloomFilter<String>) ois.readObject();
        }
    }
}
