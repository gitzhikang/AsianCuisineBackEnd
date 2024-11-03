package com.asiancuisine.asiancuisine.service.impl;

import com.asiancuisine.asiancuisine.context.BaseContext;
import com.asiancuisine.asiancuisine.mapper.IPostMapper;
import com.asiancuisine.asiancuisine.service.ICommunityService;
import com.asiancuisine.asiancuisine.util.AwsS3Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommunityServiceImpl implements ICommunityService {

    @Autowired
    AwsS3Util awsS3Util;

    @Value("${ac.aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    IPostMapper postMapper;

    @Override
    public void uploadPost(MultipartFile[] files, String text, String title, String tags) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();
        for(MultipartFile file:files){
            uploadedUrls.add(awsS3Util.uploadFile(file,bucketName));
        }

        Long userId = BaseContext.getCurrentId();
        //save userId, text, title, image to database
        Long postId = postMapper.savaPost(userId,text,title,tags);
        postMapper.savaImage(postId,uploadedUrls);
    }
}
