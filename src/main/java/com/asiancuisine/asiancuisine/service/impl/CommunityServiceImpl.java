package com.asiancuisine.asiancuisine.service.impl;

import com.asiancuisine.asiancuisine.config.SearchConfig;
import com.asiancuisine.asiancuisine.constant.PostConstants;
import com.asiancuisine.asiancuisine.constant.RedisConstants;
import com.asiancuisine.asiancuisine.context.BaseContext;
import com.asiancuisine.asiancuisine.dto.SendCommentDTO;
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
import com.asiancuisine.asiancuisine.vo.CommentVO;
import com.asiancuisine.asiancuisine.vo.PostReviewReturnVO;
import com.asiancuisine.asiancuisine.vo.PostReviewVO;
import com.google.common.hash.BloomFilter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
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
import java.util.concurrent.ConcurrentHashMap;
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

    @Autowired
    private BloomFilterService bloomFilterService;

    @Autowired
    private SearchConfig searchConfig;

    private final Map<Long, Integer> userCursors = new ConcurrentHashMap<>();

    private final Map<Long, Long> userRandomSeeds = new ConcurrentHashMap<>();

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
            if (!tagsIsExist.get(i)&&!tagsArray[i].equals("")) {
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
    public PostReviewReturnVO getRecommendPostPreviewByUserId(Long currentUserId, int pageSize,boolean refresh) throws IOException {
        if (refresh) {
            userCursors.put(currentUserId, 0);
            userRandomSeeds.put(currentUserId, System.currentTimeMillis());
        }

        Long randomSeed = userRandomSeeds.computeIfAbsent(currentUserId, k -> System.currentTimeMillis());
        int currentCursor = userCursors.getOrDefault(currentUserId, 0);
        BloomFilter<String> userBloomFilter = bloomFilterService.getBloomFilter(currentUserId.toString());

        //1.get user's all tags in redis
        Set<String> tagsByUserId = redisUtil.getTagsByUserId(currentUserId);

        //2.get top hot 3 tags
        Set<String> top3HotTags = redisUtil.getTop3HotTags();

        //Combine 2 set
        tagsByUserId.addAll(top3HotTags);

        //get weight of tags
        Map<String, Float> tagWeights = new HashMap<>();
        for (String tag : tagsByUserId) {
            Double weight = redisUtil.getTagWeight(tag);
            if (weight != null) {
                tagWeights.put(tag, weight.floatValue());
            }
        }

        //3.using tags(from user and 1 random hot tags) to search in elastic search(should generate different page result)
        //search post_content in index "post_preview" with tags in tagsByUserId

        // Build Elasticsearch query
        SearchSourceBuilder sourceBuilder = buildSearchQuery(tagWeights, currentCursor,randomSeed);

        try {
            List<PostReviewVO> posts = new ArrayList<>();
            int attemptCount = 0;
            int batchSize = searchConfig.getInitialBatchSize();
            Set<String> seenInThisQuery = new HashSet<>();

            while (posts.size() < pageSize && attemptCount < searchConfig.getMaxAttempts()) {
                sourceBuilder.from(currentCursor);
                sourceBuilder.size(batchSize);

                // Execute search with time window
                SearchRequest searchRequest = new SearchRequest("post_preview").source(sourceBuilder);
                SearchResponse response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
                SearchHit[] hits = response.getHits().getHits();

                if (hits.length == 0) {
                    // Try without time window if no results
                    sourceBuilder.query(buildFunctionScoreQuery(tagWeights, currentCursor,randomSeed));
                    searchRequest = new SearchRequest("post_preview").source(sourceBuilder);
                    response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
                    hits = response.getHits().getHits();

                    if (hits.length == 0) {
                        break;
                    }
                }

                // Filter and map posts
                for (SearchHit hit : hits) {
                    String postId = hit.getId();
                    if (!userBloomFilter.mightContain(postId) && !seenInThisQuery.contains(postId)) {
                        PostReviewVO post = convertToPostReviewVO(hit);
                        posts.add(post);
                        seenInThisQuery.add(postId);

                        if (posts.size() >= pageSize) {
                            break;  // Stop once we have enough posts
                        }
                    }
                }

                currentCursor += hits.length;
                attemptCount++;
                batchSize *= 2;
            }

            // Reset Bloom filter if no results
            if (posts.isEmpty()) {
                log.info("Resetting Bloom filter for user {} due to insufficient results", currentUserId);
                userBloomFilter = bloomFilterService.createNewBloomFilter(currentUserId.toString());

                sourceBuilder.from(0);
                sourceBuilder.size(pageSize);
                SearchRequest searchRequest = new SearchRequest("posts").source(sourceBuilder);
                SearchResponse response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);

                SearchHit[] hits = response.getHits().getHits();
                posts = Arrays.stream(hits)
                        .map(this::convertToPostReviewVO)
                        .collect(Collectors.toList());
                currentCursor = hits.length;
            }

            userCursors.put(currentUserId, currentCursor);
            bloomFilterService.saveBloomFilter(currentUserId.toString(), userBloomFilter);

            return PostReviewReturnVO.builder().postReviewVO(posts).build();

        } catch (IOException e) {
            log.error("Error getting recommendations: ", e);
            throw new RuntimeException("Error getting recommendations", e);
        }
    }

    private SearchSourceBuilder buildSearchQuery(Map<String, Float> tagWeights, int currentCursor,Long randomSeed) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder functionScoreQuery = buildFunctionScoreQuery(tagWeights, currentCursor,randomSeed);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(functionScoreQuery)
                .must(QueryBuilders.rangeQuery("timestamp")
                        .gte("now-" + searchConfig.getTimeWindowDays() + "d")
                        .lte("now"));

        sourceBuilder.query(boolQuery);
        return sourceBuilder;
    }

    private QueryBuilder buildFunctionScoreQuery(Map<String, Float> tagWeights, int currentCursor,Long randomSeed) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        for (String tag : tagWeights.keySet()) {
            boolQuery.should(QueryBuilders.termQuery("tag", tag));
        }

        List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functions = new ArrayList<>();

        // Tag weight functions
        for (Map.Entry<String, Float> entry : tagWeights.entrySet()) {
            functions.add(
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            QueryBuilders.termQuery("tag", entry.getKey()),
                            ScoreFunctionBuilders.weightFactorFunction(entry.getValue())
                    )
            );
        }

        // Time decay function
        functions.add(
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                        ScoreFunctionBuilders.exponentialDecayFunction(
                                "timestamp",
                                "now",
                                searchConfig.getDecayScaleDays() + "d",
                                searchConfig.getDecayOffsetDays() + "d",
                                searchConfig.getDecayFactor()
                        )
                )
        );

        // Random score function
        functions.add(
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                        ScoreFunctionBuilders.randomFunction()
                                .seed(randomSeed)
                                .setField("_seq_no")
                                .setWeight(searchConfig.getRandomWeight())
                )
        );

        return QueryBuilders.functionScoreQuery(boolQuery,
                        functions.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[0]))
                .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
                .boostMode(CombineFunction.MULTIPLY);
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
        List<CommentVO> commentVO = convertToCommentVO(comments);

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
                .comments(commentVO).build();
    }

    @Override
    public void sendCommentPost(SendCommentDTO sendCommentDTO) {
        //judge if the comment is a reply
        if(sendCommentDTO.getParentCommentId() == -1){
            // is not a reply
            commentMapper.saveParentComment(sendCommentDTO);
            stringRedisTemplate.opsForValue().increment(RedisConstants.POST_COMMENTS_COUNT+sendCommentDTO.getPostId());
            stringRedisTemplate.opsForValue().set(RedisConstants.POST_COMMENTS_LIKES+sendCommentDTO.getId(), "0");
        }else {
            // is a reply
            commentMapper.saveChildComment(sendCommentDTO);
            stringRedisTemplate.opsForValue().set(RedisConstants.POST_COMMENTS_LIKES+sendCommentDTO.getId(), "0");
        }
    }

    @Override
    public void likePost(Long postId, Long currentUserId) {
        stringRedisTemplate.opsForValue().increment(RedisConstants.POST_LIKES+postId);
        stringRedisTemplate.opsForSet().add(RedisConstants.USER_POST_LIKED+currentUserId, postId.toString());
    }

    @Override
    public void likeComment(Long commentId, Long currentUserId) {
        //increment comment likes in redis
        stringRedisTemplate.opsForValue().increment(RedisConstants.POST_COMMENTS_LIKES+commentId);
        //add comment id to user's liked comment set
        stringRedisTemplate.opsForSet().add(RedisConstants.USER_COMMENT_LIKED+currentUserId, commentId.toString());
    }

    @Override
    public void unLikePost(Long postId, Long currentUserId) {
        stringRedisTemplate.opsForValue().decrement(RedisConstants.POST_LIKES+postId);
        stringRedisTemplate.opsForSet().remove(RedisConstants.USER_POST_LIKED+currentUserId, postId.toString());
    }

    @Override
    public void unLikeComment(Long commentId, Long currentUserId) {
        stringRedisTemplate.opsForValue().decrement(RedisConstants.POST_COMMENTS_LIKES+commentId);
        stringRedisTemplate.opsForSet().remove(RedisConstants.USER_COMMENT_LIKED+currentUserId, commentId.toString());
    }

    @Override
    public List<String> uploadImage(MultipartFile[] files) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();
        for(MultipartFile file:files){
            uploadedUrls.add(awsS3Util.uploadFile(file,bucketName));
        }
        return uploadedUrls;
    }

    @Override
    public void uploadPostWithUrls(String[] imageUrls, String text, String title, String tags) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();
        for(String imageUrl:imageUrls){
            uploadedUrls.add(imageUrl);
        }
        String[] tagsArray = tags.split(",");

        //get new tags
        List<Boolean> tagsIsExist = redisUtil.findTagsIsExist(Arrays.asList(tagsArray));
        List<String> newTags = new ArrayList<>();
        for(int i = 0; i < tagsIsExist.size(); i++) {
            if (!tagsIsExist.get(i)&&!tagsArray[i].equals("")) {
                newTags.add(tagsArray[i]);
            }
        }
        log.info("New tags:{}",newTags);
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

    private List<CommentVO> convertToCommentVO(List<Comment> comments) {
        List<CommentVO> ans = new ArrayList<>();
        for (Comment comment : comments) {
            String[] userInfoPreview = redisUtil.getUserInfoPreview(comment.getUserId());
            String avatarUrl = userInfoPreview[0];
            String userName = userInfoPreview[1];
            boolean isFavorite = stringRedisTemplate.opsForSet().isMember(RedisConstants.USER_COMMENT_LIKED + BaseContext.getCurrentId(), comment.getId().toString());
            int favoriteCount = Integer.valueOf(stringRedisTemplate.opsForValue().get(RedisConstants.POST_COMMENTS_LIKES+comment.getId())==null?"0":stringRedisTemplate.opsForValue().get(RedisConstants.POST_COMMENTS_LIKES+comment.getId()));
            List<CommentVO> children = null;
            if(comment.getChildComments() !=null && comment.getChildComments().size() > 0){
                children = convertToCommentVO(comment.getChildComments());
            }
            ans.add(CommentVO.builder()
                    .id(comment.getId())
                    .postId(comment.getPostId())
                    .avatarUrl(avatarUrl)
                    .userName(userName)
                    .message(comment.getContent())
                    .favoriteCount(favoriteCount)
                    .isFavorite(isFavorite)
                    .dateTime(comment.getCreateTime())
                    .location(comment.getLocation())
                    .children(children)
                    .build());
        }
        return ans;

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
