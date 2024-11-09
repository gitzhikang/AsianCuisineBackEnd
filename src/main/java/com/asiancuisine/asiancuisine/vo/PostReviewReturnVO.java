package com.asiancuisine.asiancuisine.vo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
public class PostReviewReturnVO {
    Long cacheExpireTime;
    String scrollId;
    List<PostReviewVO> postReviewVO;
}
