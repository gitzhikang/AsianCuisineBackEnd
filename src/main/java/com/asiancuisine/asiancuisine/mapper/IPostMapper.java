package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.po.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IPostMapper {

    void savaImage(Long postId, List<String> uris);

    void saveTags(String[] tags);

    Long savaPost(Post post);
}
