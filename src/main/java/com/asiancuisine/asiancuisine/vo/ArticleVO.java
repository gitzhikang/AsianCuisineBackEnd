package com.asiancuisine.asiancuisine.vo;


import com.asiancuisine.asiancuisine.entity.Comment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
public class ArticleVO {
    private Long id;
    private String title;
    private String desc;
    private String[] tag;
    private String dateTime;
    private String location;
    private Long userId;
    private String userName;
    private String avatarUrl;
    private String[] images;
    private int favoriteCount;
    @JsonProperty("isFavorite")
    private boolean isFavorite;
    private List<CommentVO> comments;
}
