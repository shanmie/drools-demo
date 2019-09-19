package com.example.drools;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author deacon
 * @since 2019/9/19
 */

@Service
public class RestImpl implements Rest {

    @Autowired
    RestHighLevelClient highLevelClient;

    @Override
    public void testRest() {
        GetRequest re = new GetRequest("test","book","北京");
        GetResponse documentFields = null;
        try {
            documentFields = highLevelClient.get(re);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(documentFields.getFields());
    }
}
