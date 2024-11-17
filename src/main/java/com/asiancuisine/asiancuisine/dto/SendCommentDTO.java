package com.asiancuisine.asiancuisine.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SendCommentDTO {
    private Long id;
    private Long userId;
    private String message;
    private Long postId;
    private Long parentCommentId;
}
