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
//        requst.source(PostConstants.MAPPING_TEMPLATE, XContentType.JSON);
        restHighLevelClient.indices().create(requst, RequestOptions.DEFAULT);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.restHighLevelClient.close();
    }

    static final int MOD = 1_000_000_007;

    public static long countGoodArrays(int n,int start,int end) {
        if(n<=0){
            return 1;
        }
        int maxRange = 2 * n + 1;
        int offset = start - n; // 用于将值映射到数组索引
        int[] dpPrev = new int[maxRange];
        int[] dpCurr = new int[maxRange];

        dpPrev[start - offset] = 1;

        for (int i = 1; i <= n; i++) {
            int vMin = Math.max(start - i,end-n+i-1);
            int vMax = Math.min(start + i,end+n-i+1);
            for (int v = vMin; v <= vMax; v++) {
                int k = v - offset;
                dpCurr[k] = 0;

                // 累加从前一个位置的三种可能转移
                if (k - 1 >= 0) {
                    dpCurr[k] = (dpCurr[k] + dpPrev[k - 1]) % MOD;
                }
                dpCurr[k] = (dpCurr[k] + dpPrev[k]) % MOD;
                if (k + 1 < maxRange) {
                    dpCurr[k] = (dpCurr[k] + dpPrev[k + 1]) % MOD;
                }
            }

            // 交换 dpPrev 和 dpCurr
            int[] temp = dpPrev;
            dpPrev = dpCurr;
            dpCurr = temp;
        }

        int resultIndex = end - offset;
        if (resultIndex < 0 || resultIndex >= maxRange) {
            return 0;
        }
        dpCurr[resultIndex] = (dpPrev[resultIndex-1]+dpPrev[resultIndex+1]+dpPrev[resultIndex])%MOD;
        return dpCurr[resultIndex];


    }
    @Test
    public void test() {
        int[] arr = new int[]{0,1,1,0};
        int n = arr.length;
        int beginIndex = -1;
        int endIndex = -1;
        for(int i =0;i<n;i++){
            if(arr[i]!=0){
                beginIndex = i;
                break;
            }
        }
        for(int i =n-1;i>=0;i--){
            if(arr[i]!=0){
                endIndex = i;
                break;
            }
        }
        if(beginIndex == -1 && endIndex == -1){
            System.out.println(modPow(3,n,MOD));
            return;
        }
        if(beginIndex == endIndex){
            System.out.println(modPow(3,n-1,MOD));
            return;
        }
        long ans = 1;
        ans = (ans * modPow(3,beginIndex,MOD))%MOD;
        ans = (ans*modPow(3,n-endIndex-1,MOD))%MOD;
        int index = beginIndex;
        while(index <= endIndex){
            if(arr[index] != 0){
                int start = index;
                index++;
                while(index <= endIndex && arr[index] == 0){
                    index++;
                }
                if(index > endIndex){
                    break;
                }
                ans*=countGoodArrays(index-start-1,Math.min(arr[start],arr[index]),Math.max(arr[start],arr[index]));
                ans%=MOD;
            }else{
                index++;
            }
        }
        System.out.println(ans);
    }

    public static long modPow(long base, long exponent, int mod) {
        long result = 1;
        base %= mod; // 防止初始的 base 超过 mod

        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result = (result * base) % mod;
            }
            base = (base * base) % mod;
            exponent >>= 1; // 相当于 exponent /= 2
        }
        return result;
    }

}
