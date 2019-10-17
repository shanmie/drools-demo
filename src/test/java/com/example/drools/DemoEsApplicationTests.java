package com.example.drools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.auth.In;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpHost;
import org.apache.poi.ss.formula.functions.T;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import sun.misc.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoEsApplicationTests {


    @Autowired
    private RestHighLevelClient client;

    @Autowired
    ExcelListener excelListener;

    @Test
    public void testExcelCreate() throws IOException {
        // 写法1：
        //String fileName = "/Users/admin/Downloads/blood.xlsx";
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        //EasyExcel.read(fileName, Object.class, excelListener).sheet().doRead();
        ClassPathResource resource = new ClassPathResource("blood.xlsx");
        EasyExcel.read(resource.getInputStream(),excelListener).sheet().doRead();

    }

    @Test
    public void createIndex() throws IOException {
        Tag tag = new Tag();
        tag.setUid(1);
        tag.setCountry("印度,印度尼西亚");
        tag.set出发口岸("陕西");
        tag.setSex("男");
        tag.set是否带孩子("非亲子");
        tag.set决策周期(43);
        tag.setLocation("南京");
        IndexRequest indexRequest = new IndexRequest("test", "tag");
        ObjectMapper mapper = new ObjectMapper();
        byte[] json = mapper.writeValueAsBytes(tag);
        indexRequest.source(json, XContentType.JSON);
        client.index(indexRequest,RequestOptions.DEFAULT);
    }

    @Test
    public void updateIndex() throws IOException {
        Tag tag = new Tag();
        tag.setUid(1);
        tag.setCountry("印度");
        IndexRequest indexRequest = new IndexRequest("test","tag");
        ObjectMapper mapper = new ObjectMapper();
        byte[] json = mapper.writeValueAsBytes(tag);
        indexRequest.source(json, XContentType.JSON);
        client.index(indexRequest,RequestOptions.DEFAULT);

    }


    public String parseJson(){
       ClassPathResource resource = new ClassPathResource("conditions.json");
       try {
           return new String(IOUtils.readFully(resource.getInputStream(), -1,true));
       } catch (IOException e) {
           e.printStackTrace();
       }
       return null;
   }



    @Test
    public void test1(){
        JSONObject j = JSONObject.parseObject(parseJson());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);
        String outConditions = j.getString("out_conditions");

        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        BoolQueryBuilder boolQueryBuilder0 = QueryBuilders.boolQuery();
        BoolQueryBuilder boolQueryBuilder1 = QueryBuilders.boolQuery();
        for (Object obj: tags) {
            Map map = (Map) obj;
            String inConditions = MapUtils.getString(map, "in_conditions");
            System.out.println(inConditions);
            Object content = MapUtils.getObject(map, "content");
            List<Map> maps = JSONObject.parseArray(content.toString(), Map.class);
            BoolQueryBuilder boolQueryBuilder2 = QueryBuilders.boolQuery();
            for (Map coMap:maps) {
                String name = MapUtils.getString(coMap, "name");
                String cond = MapUtils.getString(coMap, "conditions");
                String value = MapUtils.getString(coMap, "value");
                System.out.println("name:"+name+":cond:"+cond+":value:"+value);
                //第三层
                if ("IN".equalsIgnoreCase(cond)){
                    boolQueryBuilder2.must(QueryBuilders.matchQuery(name, value).operator(Operator.AND));
                }
                if ("NOT".equalsIgnoreCase(cond)){
                    boolQueryBuilder2.mustNot(QueryBuilders.matchQuery(name, value).operator(Operator.OR));
                }

            }
            if ("AND".equalsIgnoreCase(inConditions)) {
                boolQueryBuilder1.must(boolQueryBuilder2);
            }
            if ("OR".equalsIgnoreCase(inConditions)) {
                boolQueryBuilder1.should(boolQueryBuilder2);
            }

        }
        if ("AND".equalsIgnoreCase(outConditions)) {
            boolQueryBuilder0.must(boolQueryBuilder1);
        }
        if ("OR".equalsIgnoreCase(outConditions)) {
            boolQueryBuilder0.should(boolQueryBuilder1);
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder0);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        searchSourceBuilder.size(1000);
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            int i =1;
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("-----+--->第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test2() {

        JSONObject j = JSONObject.parseObject(parseJson());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);
        String outConditions = j.getString("out_conditions");

        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        BoolQueryBuilder boolQueryBuilder0 = QueryBuilders.boolQuery();
        BoolQueryBuilder boolQueryBuilder1 = QueryBuilders.boolQuery();

        for (Object obj: tags) {
            Map map = (Map) obj;
            String inConditions = MapUtils.getString(map, "in_conditions");
            Object content = MapUtils.getObject(map, "content");
            List<Map> maps = JSONObject.parseArray(content.toString(), Map.class);
            BoolQueryBuilder boolQueryBuilder2 = QueryBuilders.boolQuery();
            for (Map coMap:maps) {
                String name = MapUtils.getString(coMap, "name");
                String cond = MapUtils.getString(coMap, "conditions");
                String value = MapUtils.getString(coMap, "value");
                //第三层
                if ("IN".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.must(QueryBuilders.matchPhrasePrefixQuery(name, value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.should(QueryBuilders.matchPhrasePrefixQuery(name, value));
                    }
                }
                if ("NOT".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        System.out.println("name:"+name+":value:"+value);
                        boolQueryBuilder2.mustNot(QueryBuilders.matchPhrasePrefixQuery(name,value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.mustNot(QueryBuilders.matchPhrasePrefixQuery(name, value) );
                    }


                }
            }
            if ("AND".equalsIgnoreCase(outConditions)) {
                boolQueryBuilder1.must(boolQueryBuilder2);
            }
            if ("OR".equalsIgnoreCase(outConditions)) {
                boolQueryBuilder1.should(boolQueryBuilder2);
            }

        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder1);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        searchSourceBuilder.size(1000);
        System.out.println(searchSourceBuilder);
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            int i = 1;
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("------++-->第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 只查询country 包含 日本  和  韩国
     */
    @Test
    public void test(){
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.from(0);

            //     MatchQueryBuilder sex = QueryBuilders.matchQuery("sex", "未知");

            MatchQueryBuilder country = QueryBuilders.matchQuery("country", "日本");
            MatchQueryBuilder country2 = QueryBuilders.matchQuery("country", "韩国");
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(country).must(country2);
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(50);
            SearchRequest searchRequest = new SearchRequest("test");
            searchRequest.source(sourceBuilder);
            System.out.println(sourceBuilder);
            //聚合条件
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                //System.out.println(hit.getSourceAsString());
                System.out.println("------++-->第 "+hit.getSourceAsMap());
            }

            client.close();
        } catch (Exception e) {
            e.getStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * 只查询country 包含 日本  或  韩国
     */
    @Test
    public void test3(){
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.from(0);
            MatchPhrasePrefixQueryBuilder matchPhrasePrefixQueryBuilder = QueryBuilders.matchPhrasePrefixQuery("country", "日本");
            PrefixQueryBuilder prefixQueryBuilder = QueryBuilders.prefixQuery("country", "韩国");
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().should(matchPhrasePrefixQueryBuilder).should(prefixQueryBuilder);
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(50);
            SearchRequest searchRequest = new SearchRequest("test");
            searchRequest.source(sourceBuilder);

            System.out.println(sourceBuilder);
            //聚合条件
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                System.out.println(hit.getSourceAsString());
            }

            client.close();
        } catch (Exception e) {
            e.getStackTrace();
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void update() throws JsonProcessingException {
        Map<String, Object> parameters = new HashMap<>();
        //UpdateRequest updateRequest = new UpdateRequest("test", "tag", "r5jce20BSr006MxniA62");
        //parameters.put("country", "印度");
        UpdateRequest updateRequest = new UpdateRequest("test", "tag", "spjce20BSr006MxniA7A");
        parameters.put("country", "印度尼西亚");
        ObjectMapper mapper = new ObjectMapper();
        byte[] json = mapper.writeValueAsBytes(parameters);
        updateRequest.doc(json, XContentType.JSON);

        try {
            UpdateResponse updateResponse = client.update(updateRequest,RequestOptions.DEFAULT);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



}
