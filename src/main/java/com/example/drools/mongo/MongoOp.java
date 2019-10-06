package com.example.drools.mongo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MongoOp {

    @Autowired
    private MongoTemplate mongoTemplate;


    public  void createCollection(String name, List<Map<String,Object>> voList) {
        //循环添加
        mongoTemplate.insert(voList,name);
    }
}
