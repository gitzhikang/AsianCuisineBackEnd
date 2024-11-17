package com.asiancuisine.asiancuisine.vo;

import com.asiancuisine.asiancuisine.entity.Comment;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
public class CommentVO {
    private Long id;
    private Long postId;
    private String avatarUrl;
    private String userName;
    private String message;
    private int favoriteCount;
    private boolean isFavorite;
    private String dateTime;
    private String location;
    private List<CommentVO> children;
}
