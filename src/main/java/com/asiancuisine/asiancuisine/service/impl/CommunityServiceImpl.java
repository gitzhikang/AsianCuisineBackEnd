package com.asiancuisine.asiancuisine.service.impl;

import com.asiancuisine.asiancuisine.context.BaseContext;
import com.asiancuisine.asiancuisine.mapper.IPostMapper;
import com.asiancuisine.asiancuisine.service.ICommunityService;
import com.asiancuisine.asiancuisine.util.AwsS3Util;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CommunityServiceImpl implements ICommunityService {

    @Autowired
    AwsS3Util awsS3Util;

    @Value("${ac.aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    IPostMapper postMapper;
    
    @Autowired
    RestHighLevelClient highLevelClient;

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
}
