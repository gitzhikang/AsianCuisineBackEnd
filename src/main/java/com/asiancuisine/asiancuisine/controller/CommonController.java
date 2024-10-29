package com.asiancuisine.asiancuisine.controller;

import com.asiancuisine.asiancuisine.Result.Result;
import com.asiancuisine.asiancuisine.util.AwsS3Util;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.asiancuisine.asiancuisine.Result.Result.success;

@RestController
@RequestMapping("common")
@Api(tags = "Common Api")
@Slf4j
public class CommonController {
    @Autowired
    AwsS3Util awsS3Util;

    @Value("${ac.aws.s3.bucketName}")
    private String bucketName;

    @ApiOperation("Upload file to Aws s3")
    @PostMapping("/upload")
    public Result<List<String>> upload(@RequestParam("files") MultipartFile[] files){
        List<String> uploadedUrls = new ArrayList<>();
        try {
            for(MultipartFile file:files){
                uploadedUrls.add(awsS3Util.uploadFile(file,bucketName));
            }
            return Result.success(uploadedUrls);

        } catch (IOException e) {
            log.error("File Upload Failed:{}",e);
        }
        return Result.error("File Upload Failed");
    }
}
