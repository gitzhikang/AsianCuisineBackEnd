package com.asiancuisine.asiancuisine.util;

import com.asiancuisine.asiancuisine.constant.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //save tags to user record
    public void saveTagsByUserId(Long userId, List<String> tags) {
        stringRedisTemplate.opsForSet().add(RedisConstants.USER_TAGS_KEY+userId.toString(),tags.toArray(new String[0]));
    }

    //get tags from user record
    public Set<String> getTagsByUserId(Long userId) {
        return stringRedisTemplate.opsForSet().randomMembers(RedisConstants.USER_TAGS_KEY+userId.toString(), RedisConstants.TAGS_COUNT_ONE_USER).stream().collect(Collectors.toSet());
    }

    //get top 3 hot tags in community
    public Set<String> getTop3HotTags(){
       return   stringRedisTemplate.opsForZSet().range(RedisConstants.HOT_TAGS_KEY,0L,3L);
    }

    //save tags to hot tags in community
    public void saveTagsToHotTags(List<String> tags) {
        for(int index = 0;index<tags.size();index++){
            stringRedisTemplate.opsForZSet().incrementScore(RedisConstants.HOT_TAGS_KEY, tags.get(index), 1);
        }
    }

    public List<Boolean> findTagsIsExist(List<String> tags) {

       List<Object> results = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
            byte[] rawKey = stringRedisTemplate.getStringSerializer().serialize(RedisConstants.ALL_TAGS);
            byte[][] rawElements = tags.stream()
                    .map(stringRedisTemplate.getStringSerializer()::serialize)
                    .toArray(byte[][]::new);

            // Call the underlying smIsMember method
            connection.openPipeline();
            for (byte[] rawElement : rawElements) {
                connection.setCommands().sIsMember(rawKey, rawElement);
            }
            return null;
        });
        return results.stream().map(result -> (Boolean) result).collect(Collectors.toList());
    }

    // Save verification code to Redis with expiration time
    public void saveVerificationCode(Long userId, String code) {
        stringRedisTemplate.opsForValue().set(RedisConstants.VERIFICATION_KEY+userId, code, RedisConstants.VERIFICATION_CODE_EXPIRED_TIME_SECONDS, TimeUnit.SECONDS);
    }

    // Get verification code from Redis
    public String getVerificationCode(Long userId) {
        return stringRedisTemplate.opsForValue().get(RedisConstants.VERIFICATION_KEY + userId);
    }
}
