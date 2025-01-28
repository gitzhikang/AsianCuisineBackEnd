package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.po.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface IPostMapper {

    void savaImage(Long postId, List<String> uris);

    void saveTags(String[] tags);

    Long savaPost(Post post);

    Post queryPostById(Long postId);

    List<String> queryImagesByPostId(Long postId);

    void batchUpdateLikeCount(Map<Long,Integer> postLikeCountMap);

    /**
     * Get the maximum post ID in the database
     * @return the maximum post ID
     */
    long getMaxPostId();
}
