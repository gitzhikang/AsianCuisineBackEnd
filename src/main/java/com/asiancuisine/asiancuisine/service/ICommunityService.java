package com.asiancuisine.asiancuisine.service;


import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ICommunityService {

    void uploadPost(MultipartFile[] files, String text, String title, String tags, String additionalTags) throws IOException;

    List<String> getSuggestion(String text) throws IOException;
}
