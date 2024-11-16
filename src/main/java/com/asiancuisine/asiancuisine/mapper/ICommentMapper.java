package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ICommentMapper {
    // query top level comments
    List<Comment> getTopLevelComments(@Param("postId") Long postId);

    // query child comments
    List<Comment> getChildComments(@Param("parentId") Long parentId);

}
