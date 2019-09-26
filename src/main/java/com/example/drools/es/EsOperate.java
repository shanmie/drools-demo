package com.example.drools.es;

import com.example.drools.UserIndex;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author deacon
 * @since 2019/9/25
 */
@Service
@Slf4j
public class EsOperate {

    private final RestHighLevelClient client;

    @Autowired
    public EsOperate(RestHighLevelClient client) {
        this.client = client;
    }

    public void createIndex(String index, String type, List<Map<String,Object>> voList) {
        try {
            for (Map<String,Object> map : voList) {
                IndexRequest indexRequest = new IndexRequest(index, type);
                ObjectMapper mapper = new ObjectMapper();
                byte[] json = mapper.writeValueAsBytes(map);
                indexRequest.source(json, XContentType.JSON);
                client.index(indexRequest, RequestOptions.DEFAULT);
            }
        } catch (IOException e) {
            log.error("create index error is {}", e);
        }
    }
}


