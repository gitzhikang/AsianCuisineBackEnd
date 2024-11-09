package com.asiancuisine.asiancuisine.po;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
public class Post {
    private Long id;
    private Long userId;
    private String title;
    private String text;
    private String firstImageUrl;
    private String tags;
    private Date createTime;
    private Integer likedCount;
    private Integer commentCount;
}
