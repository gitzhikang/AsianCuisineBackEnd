package com.asiancuisine.asiancuisine.service;


import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ICommunityService {

    void uploadPost(MultipartFile[] files, String text,  String title) throws IOException;

}
