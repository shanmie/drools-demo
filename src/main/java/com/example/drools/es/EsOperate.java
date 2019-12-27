package com.example.drools.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
            int i = 1;
            for (Map<String,Object> map : voList) {
                IndexRequest indexRequest = new IndexRequest(index, type,map.get("uid").toString());
                ObjectMapper mapper = new ObjectMapper();
                byte[] json = mapper.writeValueAsBytes(map);
                indexRequest.source(json, XContentType.JSON);
                String uid = map.get("uid").toString();
                IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
                log.info("create index seqNo {} uid {} response {}",i++,uid,response.status());
            }
        } catch (IOException e) {
            log.error("create index error is {}", e);
        }
    }

    private void save(String index,String type, List<Map<String,Object>> voList) {
        try {
            BulkRequest request = new BulkRequest();
            request.timeout(TimeValue.timeValueMinutes(1)); // 设置超时，等待所有节点确认索引已打开（使用TimeValue形式）
            for (Map<String, Object> map : voList) {
                IndexRequest indexRequest = new IndexRequest(index, type);
                ObjectMapper mapper = new ObjectMapper();
                byte[] json = mapper.writeValueAsBytes(map);
                indexRequest.source(json, XContentType.JSON);
                request.add(indexRequest);
            }
            // 执行请求
            client.bulk(request, RequestOptions.DEFAULT);
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }


}


