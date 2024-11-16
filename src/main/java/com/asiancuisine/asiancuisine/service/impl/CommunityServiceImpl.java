package com.asiancuisine.asiancuisine.service.impl;

import com.asiancuisine.asiancuisine.constant.PostConstants;
import com.asiancuisine.asiancuisine.constant.RedisConstants;
import com.asiancuisine.asiancuisine.context.BaseContext;
import com.asiancuisine.asiancuisine.entity.Comment;
import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.mapper.ICommentMapper;
import com.asiancuisine.asiancuisine.mapper.IPostMapper;
import com.asiancuisine.asiancuisine.mapper.IUserMapper;
import com.asiancuisine.asiancuisine.po.Post;
import com.asiancuisine.asiancuisine.service.ICommunityService;
import com.asiancuisine.asiancuisine.util.AwsS3Util;
import com.asiancuisine.asiancuisine.util.RedisUtil;
import com.asiancuisine.asiancuisine.vo.ArticleVO;
import com.asiancuisine.asiancuisine.vo.PostReviewReturnVO;
import com.asiancuisine.asiancuisine.vo.PostReviewVO;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommunityServiceImpl implements ICommunityService {

    @Autowired
    AwsS3Util awsS3Util;

    @Value("${ac.aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    IPostMapper postMapper;

    @Autowired
    IUserMapper userMapper;
    
    @Autowired
    RestHighLevelClient highLevelClient;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ICommentMapper commentMapper;

    @Override
    public void uploadPost(MultipartFile[] files, String text, String title, String tags) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();
        for(MultipartFile file:files){
            uploadedUrls.add(awsS3Util.uploadFile(file,bucketName));
        }
        String[] tagsArray = tags.split(",");

        //get new tags
        List<Boolean> tagsIsExist = redisUtil.findTagsIsExist(Arrays.asList(tagsArray));
        List<String> newTags = new ArrayList<>();
        for(int i = 0; i < tagsIsExist.size(); i++) {
            if (!tagsIsExist.get(i)) {
                newTags.add(tagsArray[i]);
            }
        }

        if(newTags!=null && newTags.size()>0){
            //save new tags to redis
            stringRedisTemplate.opsForSet().add(RedisConstants.ALL_TAGS, newTags.toArray(new String[0]));

            //save new tags to database
            postMapper.saveTags(newTags.toArray(new String[0]));
            //save new tags to elasticsearch
            for (String tag : newTags.toArray(new String[0])) {
                try {
                    highLevelClient.index(new IndexRequest("tags").id(tag).source("tag", tag), RequestOptions.DEFAULT);
                }catch (IOException e){
                    log.info("Error while saving tags to elasticsearch:{}",e);
                }
            }
        }


        Long userId = BaseContext.getCurrentId();
        String firstImageUrl = uploadedUrls.get(0);
        //save userId, text, title, image to database
        Post post = new Post();
        post.setUserId(userId);
        post.setText(text);
        post.setTitle(title);
        post.setTags(tags);
        post.setFirstImageUrl(firstImageUrl);
        postMapper.savaPost(post);
        Long postId = post.getId();
        postMapper.savaImage(postId,uploadedUrls);

//        Get user details from Redis
        String[] userInfoPreview = redisUtil.getUserInfoPreview(userId);
        String userIcon = userInfoPreview[0];
        String userNickName = userInfoPreview[1];

        //save tags to redis
        if(tags != null && !tags.equals("")){
            List<String> tagsList = new ArrayList<>(Arrays.asList(tags.split(",")));
            redisUtil.saveTagsByUserId(userId, tagsList);
            redisUtil.saveTagsToHotTags(tagsList);
        }

        //save likes to redis
        stringRedisTemplate.opsForValue().set(RedisConstants.POST_LIKES+postId, "0");


        //add post preview info to ES
        Map<String, Object> postPreview = new HashMap<>();
        postPreview.put("id", postId);
        postPreview.put("user_id", userId);
        postPreview.put("title", title);
        postPreview.put("tags", tags);
        postPreview.put("text", text);
        postPreview.put("userIconUrl", userIcon);
        postPreview.put("firstImageUrl", firstImageUrl);

        highLevelClient.index(new IndexRequest("post_preview").id(postId.toString()).source(postPreview), RequestOptions.DEFAULT);
    }

    @Override
    public List<String> getSuggestion(String text) throws IOException {
        SearchRequest request = new SearchRequest("tags");
        request.source().suggest(new SuggestBuilder().addSuggestion("suggestions",
                SuggestBuilders.completionSuggestion("tag").prefix(text).skipDuplicates(true).size(10)));
        SearchResponse response = highLevelClient.search(request, RequestOptions.DEFAULT);
        //parse response
        Suggest suggest = response.getSuggest();
        CompletionSuggestion suggestions = suggest.getSuggestion("suggestions");
        List<CompletionSuggestion.Entry.Option> options = suggestions.getOptions();
        if(options.size() == 0 || options == null){
            return Collections.emptyList();
        }
        List<String> suggestionList = new ArrayList<>();
        for (CompletionSuggestion.Entry.Option option : options) {
            String sugText = option.getText().toString();
            suggestionList.add(sugText);
        }
        return suggestionList;
    }

    @Override
    public PostReviewReturnVO getRecommendPostPreviewByUserId(Long currentUserId, String scrollId, Long cacheExpireTime) throws IOException {
        final Scroll scroll = new Scroll(TimeValue.timeValueMillis(PostConstants.CACHE_SCROLL_ID_EXPIRE_TIME));  // 设置 scroll 的生存时间
        SearchResponse searchResponse;
        Long cacheNewExpireTime = 0L;
        // the refresh page request, will not use scrollId
        // if the cacheTime in the ES has been expired, then we need to search again
        if(scrollId == null || scrollId.equals("") || cacheExpireTime == 0 || cacheExpireTime - System.currentTimeMillis() > PostConstants.CACHE_SCROLL_ID_EXPIRE_TIME){
            //1.get user's tags in redis
            Set<String> tagsByUserId = redisUtil.getTagsByUserId(currentUserId);

            //2.get top hot tags
            Set<String> top3HotTags = redisUtil.getTop3HotTags();

            //Combine 2 set

            tagsByUserId.addAll(top3HotTags);



            //3.using tags(from user and 1 random hot tags) to search in elastic search(should generate different page result)
            //search post_content in index "post_preview" with tags in tagsByUserId
            SearchRequest searchRequest = new SearchRequest("post_preview");
            searchRequest.scroll(scroll);
            searchRequest.source().query(QueryBuilders.matchQuery("post_content", tagsByUserId.toString())).size(10);

            searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            log.info("Fresh Page Search Response: {}", searchResponse);
            scrollId = searchResponse.getScrollId();
            cacheNewExpireTime = System.currentTimeMillis() + PostConstants.CACHE_SCROLL_ID_EXPIRE_TIME;

        }else {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.timeValueMinutes(1));
            searchResponse = highLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            cacheNewExpireTime = cacheExpireTime;
            log.info("scroll down to get more post previews: {}", searchResponse);
        }

        //parse search response and handle results
        List<PostReviewVO> postPreviews = new ArrayList<>();
        searchResponse.getHits().forEach(hit -> postPreviews.add(convertToPostReviewVO(hit)));
        log.info("Recommended post previews: {}", postPreviews);
        return PostReviewReturnVO.builder().cacheExpireTime(cacheNewExpireTime).scrollId(scrollId).postReviewVO(postPreviews).build();

    }

    @Override
    public ArticleVO getPostDetail(Long postId) {
        //query post detail from database
        Post post = postMapper.queryPostById(postId);
        //get user details from Redis
        String[] userInfoPreview = redisUtil.getUserInfoPreview(post.getUserId());
        String userIcon = userInfoPreview[0];
        String userNickName = userInfoPreview[1];
        //Phrase tags into array
        String[] tagsArray = new String[0];
        if (post.getTags() != null && !post.getTags().equals("")) {
            tagsArray = post.getTags().split(",");
        }
        //get images from database
        List<String> images = postMapper.queryImagesByPostId(postId);
        //get likes for post from redis
        Integer likes = Integer.valueOf(stringRedisTemplate.opsForValue().get(RedisConstants.POST_LIKES+postId)==null?"0":stringRedisTemplate.opsForValue().get(RedisConstants.POST_LIKES+postId));
        //get if the user like the post before
        Boolean userPostLiked = stringRedisTemplate.opsForSet().isMember(RedisConstants.USER_POST_LIKED + BaseContext.getCurrentId(), postId.toString());
        //get comments from database
        List<Comment> comments = commentMapper.getTopLevelComments(postId);

        //build ArticleVO
        return ArticleVO.builder()
                .id(postId)
                .title(post.getTitle())
                .desc(post.getText())
                .tag(tagsArray)
                .userName(userNickName)
                .avatarUrl(userIcon)
                .images(images.toArray(new String[0]))
                .favoriteCount(likes)
                .isFavorite(userPostLiked)
                .comments(comments).build();
    }

    private PostReviewVO convertToPostReviewVO(SearchHit hit) {
        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
        Long userId = ((Number) sourceAsMap.get("user_id")).longValue();

        // Try to get user details from Redis
        String userIcon = (String) stringRedisTemplate.opsForHash().get(RedisConstants.USER_PREVIEW_KEY + userId, "icon");
        String userNickName = (String) stringRedisTemplate.opsForHash().get(RedisConstants.USER_PREVIEW_KEY + userId, "nickName");

        // If not found in Redis, get from database
        if (userIcon == null || userNickName == null) {
            User user = userMapper.queryById(userId);
            userIcon = user.getIcon();
            userNickName = user.getNickName();

            // Store user details in Redis
            stringRedisTemplate.opsForHash().put(RedisConstants.USER_PREVIEW_KEY + userId, "icon", userIcon);
            stringRedisTemplate.opsForHash().put(RedisConstants.USER_PREVIEW_KEY + userId, "nickName", userNickName);
            stringRedisTemplate.expire(RedisConstants.USER_PREVIEW_KEY + userId, 1, TimeUnit.DAYS);

        }

        //get likes for post from redis
        //get current post id
        Long id = ((Number) sourceAsMap.get("id")).longValue();
        Integer likes = Integer.valueOf(stringRedisTemplate.opsForValue().get(RedisConstants.POST_LIKES+id)==null?"0":stringRedisTemplate.opsForValue().get(RedisConstants.POST_LIKES+id));
        //get if the user like the post before
        Boolean userPostLiked = stringRedisTemplate.opsForSet().isMember(RedisConstants.USER_POST_LIKED + BaseContext.getCurrentId(), id.toString());

        return new PostReviewVO()
                .setId(id)
                .setUserName(userNickName)
                .setAvatarUrl(userIcon)
                .setTitle((String) sourceAsMap.get("title"))
                .setImage((String) sourceAsMap.get("firstImageUrl"))
                .setFavoriteCount(likes)
                .setFavorite(userPostLiked);
}
}
