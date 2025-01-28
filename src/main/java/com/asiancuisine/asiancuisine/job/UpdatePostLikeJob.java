package com.asiancuisine.asiancuisine.job;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import com.asiancuisine.asiancuisine.job.event.BaseEvent;
import com.asiancuisine.asiancuisine.job.event.EventPublisher;
import com.asiancuisine.asiancuisine.job.event.UpdatePostLikeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.asiancuisine.asiancuisine.constant.RedisConstants;
import com.asiancuisine.asiancuisine.mapper.IPostMapper;
import com.asiancuisine.asiancuisine.util.PostBitmapUtil;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class UpdatePostLikeJob {



    @Autowired
    private IPostMapper postMapper;
    
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private PostBitmapUtil bitmapUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UpdatePostLikeEvent updatePostLikeEvent;

    @Resource
    private EventPublisher eventPublisher;

     // 配置参数
    private static final int SEGMENT_SIZE = 500_000; // 每个段包含100万个帖子
    private static final int MAX_EXECUTE_UPDATE_COUNT = 2000; // 执行次数
    private static final int MAX_THREADS = 4; // 最大线程数
    private static final long LOCK_WAIT_TIME = 0L; // 锁等待时间(ms)
    private static final long LOCK_LEASE_TIME = 30000L; // 锁租约时间(ms)

    //config tread pool
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000));
    
    @Scheduled(cron = "0 0/1 * * * *")
    public void execute() throws InterruptedException {
        log.info("Start post update task");

        //get lock from redis, if not get lock, indicate that another instance is running the schedule, so return
        RLock lock = redissonClient.getLock(RedisConstants.POST_UPDATE_LOCK);
        if(!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.MILLISECONDS)){
            log.warn("Failed to acquire lock for post update");
            return;
        }

        List<Future<?>> futures = new ArrayList<>();
        
        try {
            // 1. 获取需要处理的范围

            //get current chunk from redis
            Integer currentChunk = Integer.valueOf(stringRedisTemplate.opsForValue().get(RedisConstants.POST_UPDATE_CURRENT_CHUNK));
            long maxPostId = postMapper.getMaxPostId();
            int segmentCount = (int) (maxPostId / SEGMENT_SIZE) + 1;
            int totalUpdate = 0;
            
            // 2. 为每个段创建任务,BATCH_SIZE个帖子
            for (int segmentIndex = currentChunk; segmentIndex < segmentCount; segmentIndex++) {
                final int currentSegmentIndex = segmentIndex;  // 创建一个 final 变量
                List<Long> updatePostIds = bitmapUtil.getUpdatePostIds(segmentIndex);

                //query corresponding post like count
                List<String> keys = updatePostIds.stream()
                .map(id -> RedisConstants.POST_LIKES + id)
                .collect(Collectors.toList());
                List<String> likeCounts = stringRedisTemplate.opsForValue().multiGet(keys);

                //send MQ message to update post like count
                totalUpdate += updatePostIds.size();
                Future<?> future = threadPoolExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        //assemble MQ message
                        Map<Long,Integer> likeCountMap = new HashMap<>();
                        for (int i = 0; i < updatePostIds.size(); i++) {
                            likeCountMap.put(updatePostIds.get(i), likeCounts.get(i) == null ? 0 : Integer.valueOf(likeCounts.get(i)));
                        }
                        //build message object
                        UpdatePostLikeEvent.LikeMessage likeMessage = UpdatePostLikeEvent.LikeMessage.builder().likeCountMap(likeCountMap).build();
                        //build event message
                        BaseEvent.EventMessage<UpdatePostLikeEvent.LikeMessage> likeMessageEventMessage = updatePostLikeEvent.buildEventMessage(likeMessage);
                        //send message
                        eventPublisher.publish(updatePostLikeEvent.topic(), likeMessageEventMessage);
                        bitmapUtil.clearBitsInBatch(currentSegmentIndex, updatePostIds);
                    }
                });
                futures.add(future);
                if(totalUpdate > MAX_EXECUTE_UPDATE_COUNT){
                    break;
                }
            }
            
            // 3. 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("Error processing segment", e);
                }
            }

        } finally {
            lock.unlock();
            log.info("Completed post update task");
        }
    }

    

}
