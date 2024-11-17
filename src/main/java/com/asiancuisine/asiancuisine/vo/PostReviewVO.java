package com.asiancuisine.asiancuisine.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class PostReviewVO {
    private Long id;
    private String userName;
    private String avatarUrl;
    private String title;
    private String image;

    @JsonProperty("isFavorite")
    private boolean isFavorite;

    private int favoriteCount;
}

