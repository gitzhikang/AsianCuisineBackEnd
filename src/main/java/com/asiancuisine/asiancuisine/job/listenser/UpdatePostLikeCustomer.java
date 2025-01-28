package com.asiancuisine.asiancuisine.job.listenser;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.asiancuisine.asiancuisine.job.event.UpdatePostLikeEvent;
import com.asiancuisine.asiancuisine.mapper.IPostMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.asiancuisine.asiancuisine.job.event.BaseEvent;

import java.util.List;

@Slf4j
@Component
public class UpdatePostLikeCustomer {

    @Value("${spring.rabbitmq.topic.post_update}")
    private String topic;

    @Autowired
    private IPostMapper postMapper;

    @RabbitListener(queuesToDeclare = @Queue(value = "${spring.rabbitmq.topic.post_update}"))
    public void listener(String message) {
        BaseEvent.EventMessage<UpdatePostLikeEvent.LikeMessage> eventMessage = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<UpdatePostLikeEvent.LikeMessage>>() {
        }.getType());

        UpdatePostLikeEvent.LikeMessage likeMessage = eventMessage.getData();

        //updatePostLike in batch

        postMapper.batchUpdateLikeCount(likeMessage.getLikeCountMap());
    }

}
