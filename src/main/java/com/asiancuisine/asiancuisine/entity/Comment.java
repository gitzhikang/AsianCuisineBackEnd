package com.asiancuisine.asiancuisine.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Comment {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private int favoriteCount;
    private String createTime;
    private String location;
    private List<Comment> childComments;
}
