package com.asiancuisine.asiancuisine.job.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class UpdatePostLikeEvent extends BaseEvent<UpdatePostLikeEvent.LikeMessage> {

    @Value("${spring.rabbitmq.topic.post_update}")
    private String topic;

    @Override
    public EventMessage<LikeMessage> buildEventMessage(LikeMessage data) {
        return EventMessage.<UpdatePostLikeEvent.LikeMessage>builder()
                .id(RandomStringUtils.randomNumeric(11))
                .timestamp(new Date())
                .data(data)
                .build();
    }

    @Override
    public String topic() {
        return topic;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LikeMessage {
        Map<Long,Integer> likeCountMap;
    }
}
