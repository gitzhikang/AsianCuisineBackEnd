package com.asiancuisine.asiancuisine.vo;

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
    private int favoriteCount;
    private boolean isFavorite;
}

