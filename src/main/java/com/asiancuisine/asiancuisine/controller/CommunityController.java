package com.asiancuisine.asiancuisine.controller;

import com.asiancuisine.asiancuisine.Result.Result;
import com.asiancuisine.asiancuisine.service.ICommunityService;
import com.asiancuisine.asiancuisine.util.AwsS3Util;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.asiancuisine.asiancuisine.Result.Result.success;

@RestController
@RequestMapping("community")
@Api(tags = "community Api")
@Slf4j
public class CommunityController {
    @Autowired
    AwsS3Util awsS3Util;

    @Value("${ac.aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    private ICommunityService communityService;

    @ApiOperation("Upload file to Aws s3")
    @PostMapping("/upload")
    public Result upload(@RequestParam("files") MultipartFile[] files,@RequestParam("text") String text,@RequestParam("title") String title,@RequestParam("tags") String tags) {
        List<String> uploadedUrls = new ArrayList<>();
        try {
            communityService.uploadPost(files,text,title,tags);
            return Result.success();
        } catch (IOException e) {
            log.error("File Upload Failed:{}",e);
        }
        return Result.error("File Upload Failed");
    }

    @ApiOperation("Tags Hint in tags selection page")
    @GetMapping("/suggestion/{text}")
    public Result<List<String>> getSuggestion(@PathVariable String text) throws IOException {
        if(text.equals("") || text == null){
            return Result.success(new ArrayList<>());
        }
       return Result.success(communityService.getSuggestion(text));
    }
}
