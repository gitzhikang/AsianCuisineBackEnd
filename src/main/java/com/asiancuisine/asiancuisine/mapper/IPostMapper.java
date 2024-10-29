package com.asiancuisine.asiancuisine.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IPostMapper {
    Long savaPost(Long userId, String text, String title);

    void savaImage(Long postId, List<String> uris);
}
