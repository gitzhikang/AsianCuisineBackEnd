package com.asiancuisine.asiancuisine.service;


import com.asiancuisine.asiancuisine.dto.SendCommentDTO;
import com.asiancuisine.asiancuisine.vo.ArticleVO;
import com.asiancuisine.asiancuisine.vo.PostReviewReturnVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ICommunityService {

    void uploadPost(MultipartFile[] files, String text, String title, String tags) throws IOException;

    List<String> getSuggestion(String text) throws IOException;

    PostReviewReturnVO getRecommendPostPreviewByUserId(Long currentUserId, String scrollId, Long cacheTime) throws IOException;

    ArticleVO getPostDetail(Long postId);

    void sendCommentPost(SendCommentDTO sendCommentDTO);

    void likePost(Long postId, Long currentUserId);

    void likeComment(Long commentId, Long currentUserId);

    void unLikePost(Long postId, Long currentUserId);

    void unLikeComment(Long commentId, Long currentUserId);

    List<String> uploadImage(MultipartFile[] files) throws IOException;

    void uploadPostWithUrls(String[] imageUrls, String text, String title, String tags) throws IOException;
}
