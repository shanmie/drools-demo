package com.example.drools;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RestClientApplicationTest {

    @Autowired
    private RestHighLevelClient client;
    @Test
    public void test2() throws IOException {
        GetRequest re = new GetRequest("test","book","1");
        GetResponse documentFields = client.get(re, RequestOptions.DEFAULT);
        System.out.println(JSONObject.toJSONString(documentFields));
    }


}
