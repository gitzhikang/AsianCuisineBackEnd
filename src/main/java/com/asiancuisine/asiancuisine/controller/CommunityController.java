package com.asiancuisine.asiancuisine.controller;

import com.amazonaws.services.s3.transfer.Upload;
import com.asiancuisine.asiancuisine.Result.Result;
import com.asiancuisine.asiancuisine.context.BaseContext;
import com.asiancuisine.asiancuisine.dto.SendCommentDTO;
import com.asiancuisine.asiancuisine.dto.UploadWithUrlDTO;
import com.asiancuisine.asiancuisine.service.ICommunityService;
import com.asiancuisine.asiancuisine.util.AwsS3Util;
import com.asiancuisine.asiancuisine.vo.ArticleVO;
import com.asiancuisine.asiancuisine.vo.PostReviewReturnVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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
    public Result<String> upload(@RequestParam("files") MultipartFile[] files,@RequestParam("text") String text,@RequestParam("title") String title,@RequestParam("tags") String tags){
        try {
            communityService.uploadPost(files,text,title,tags);
            return Result.success();
        } catch (IOException e) {
            log.error("File Upload Failed:{}",e);
        }
        return Result.error("File Upload Failed");
    }

    @ApiOperation("Upload image to Aws s3, get the image urls")
    @PostMapping("/uploadImage")
    public Result uploadImage(@RequestParam("files") MultipartFile[] files){
        List<String> uploadedUrls = new ArrayList<>();
        try {
            uploadedUrls = communityService.uploadImage(files);
            return Result.success(uploadedUrls);
        } catch (IOException e) {
            log.error("File Upload Failed:{}",e);
        }
        return Result.error("File Upload Failed");
    }

    @ApiOperation("Upload post with uploaded image urls")
    @PostMapping("/uploadWithUrls")
    public Result uploadWithUrls(@RequestBody UploadWithUrlDTO uploadWithUrlDTO){
        List<String> uploadedUrls = new ArrayList<>();
        try {
            communityService.uploadPostWithUrls(uploadWithUrlDTO.getImageUrls(),uploadWithUrlDTO.getText(),uploadWithUrlDTO.getTitle(),uploadWithUrlDTO.getTags());
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

    @ApiOperation("Get Post Review List")
    @GetMapping("/getPostPreview")
    public Result<PostReviewReturnVO> getPostPreview(@RequestParam("scrollId") String scrollId, @RequestParam("cacheTime") Long cacheTime) throws IOException {
        Long currentUserId = BaseContext.getCurrentId();
        PostReviewReturnVO postReviewReturnVO= communityService.getRecommendPostPreviewByUserId(currentUserId, scrollId, cacheTime);
        return Result.success(postReviewReturnVO);
    }

    @ApiOperation("Get Post Detail")
    @GetMapping("/getPostDetail/{postId}")
    public Result<ArticleVO> getPostDetail(@PathVariable Long postId) throws IOException {
        return success(communityService.getPostDetail(postId));
    }

    @ApiOperation("Send Comment")
    @PostMapping("/sendCommentPost")
    public Result sendCommentPost(@RequestBody SendCommentDTO sendCommentDTO) {
        Long currentUserId = BaseContext.getCurrentId();
        sendCommentDTO.setUserId(currentUserId);
        communityService.sendCommentPost(sendCommentDTO);
        return Result.success();
    }

    @ApiOperation("Like Post")
    @GetMapping("/likePost/{postId}")
    public Result likePost(@PathVariable Long postId) {
        Long currentUserId = BaseContext.getCurrentId();
        communityService.likePost(postId, currentUserId);
        return Result.success();
    }

    @ApiOperation("Unlike Post")
    @GetMapping("/unLikePost/{postId}")
    public Result UnLikePost(@PathVariable Long postId) {
        Long currentUserId = BaseContext.getCurrentId();
        communityService.unLikePost(postId, currentUserId);
        return Result.success();
    }

    @ApiOperation("Like Comment")
    @GetMapping("/likeComment/{commentId}")
    public Result likeComment(@PathVariable Long commentId) {
        Long currentUserId = BaseContext.getCurrentId();
        communityService.likeComment(commentId, currentUserId);
        return Result.success();
    }

    @ApiOperation("UnLike Comment")
    @GetMapping("/unLikeComment/{commentId}")
    public Result unLikeComment(@PathVariable Long commentId) {
        Long currentUserId = BaseContext.getCurrentId();
        communityService.unLikeComment(commentId, currentUserId);
        return Result.success();
    }









}
