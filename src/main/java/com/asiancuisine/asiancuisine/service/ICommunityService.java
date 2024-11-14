package com.asiancuisine.asiancuisine.service;


import com.asiancuisine.asiancuisine.vo.PostReviewReturnVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ICommunityService {

    void uploadPost(MultipartFile[] files, String text, String title, String tags) throws IOException;

    List<String> getSuggestion(String text) throws IOException;

    PostReviewReturnVO getRecommendPostPreviewByUserId(Long currentUserId, String scrollId, Long cacheTime) throws IOException;
}
