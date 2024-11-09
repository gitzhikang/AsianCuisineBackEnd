package com.asiancuisine.asiancuisine.util;

import com.asiancuisine.asiancuisine.constant.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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


}
