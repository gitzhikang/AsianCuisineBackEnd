package com.asiancuisine.asiancuisine;

import com.asiancuisine.asiancuisine.constant.PostConstants;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class AsianCuisineApplicationTests {


    private RestHighLevelClient restHighLevelClient;

    @BeforeEach
    void setUp() {
        restHighLevelClient = new RestHighLevelClient(RestClient.builder(
                new HttpHost("vcm-44123.vm.duke.edu", 9200, "http")));
    }

    @Test
    void createIndex() throws IOException {
        CreateIndexRequest requst = new CreateIndexRequest("post");
        requst.source(PostConstants.MAPPING_TEMPLATE, XContentType.JSON);
        restHighLevelClient.indices().create(requst, RequestOptions.DEFAULT);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.restHighLevelClient.close();
    }

}
