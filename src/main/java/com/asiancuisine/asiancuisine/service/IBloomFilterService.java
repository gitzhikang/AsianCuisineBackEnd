package com.asiancuisine.asiancuisine.service;

import com.google.common.hash.BloomFilter;

public interface IBloomFilterService {
    BloomFilter<String> getBloomFilter(String userId);
    void saveBloomFilter(String userId, BloomFilter<String> filter);
}
