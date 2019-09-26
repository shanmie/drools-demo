package com.example.drools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoDroolsApplicationTests {


    @Autowired
    private RestHighLevelClient client;

    @Autowired
    ExcelListener excelListener;

    @Test
    public void testExcelCreate(){
        // 写法1：
        String fileName = "/Users/admin/Downloads/blood.xlsx";
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        //EasyExcel.read(fileName, Object.class, excelListener).sheet().doRead();

        EasyExcel.read(fileName,excelListener).sheet().doRead();



    }

    @Test
    public void createIndex() throws IOException {
        Book book = new Book();
        book.setId(5);
        book.setBookName("出发口岸南京,常驻城市上海,CEO,30");
        IndexRequest indexRequest = new IndexRequest("test", "book");
        ObjectMapper mapper = new ObjectMapper();
        byte[] json = mapper.writeValueAsBytes(book);
        indexRequest.source(json, XContentType.JSON);
        client.index(indexRequest,RequestOptions.DEFAULT);
    }

    @Test
    public void test() {
        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.matchPhraseQuery("bookName","出发口岸北京,有钱人"));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(1000);//每次查询1000条
        searchSourceBuilder.query(queryBuilder);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("book");//设置类型
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("---->"+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test1(){
        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引

        //QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery("出发口岸北京,常驻城市北京").field("bookName").defaultOperator(Operator.AND);

        //TermsQueryBuilder queryBuilder = QueryBuilders.termsQuery("bookName", "出发口岸北京,常驻城市北京");
        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery("country", "日本,新西兰,西班牙,美国").operator(Operator.OR);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(1000);//每次查询1000条
        searchSourceBuilder.query(queryBuilder);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("--->"+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String json(){
        return  "{\n" +
                "\t\"tags\":\n" +
                "\t[\n" +
                "\t\t{\n" +
                "\t\t\t\"name\":\"出发口岸\",\n" +
                "\t\t\t\"value\":\"上海市,成都市,广州市,北京市\",\n" +
                "\t\t\t\"conditions\":\"AND\"\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"name\":\"country\",\n" +
                "\t\t\t\"value\":\"日本,新西兰,西班牙,美国\",\n" +
                "\t\t\t\"conditions\":\"OR\"\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"name\":\"sex\",\n" +
                "\t\t\t\"value\":\"男\",\n" +
                "\t\t\t\"conditions\":\"LIKE\"\n" +
                "\t\t}\n" +
                "\n" +
                "\t],\n" +
                "\t\"out_confitions\":\"AND\"\n" +
                "\n" +
                "}";
    }

    @Test
    public void test2(){

        JSONObject j = JSONObject.parseObject(json());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);

       // Map map1 = JSONObject.toJavaObject(tags, Map.class);

        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        for (Object obj: tags) {
            Map map = (Map) obj;
            String name = MapUtils.getString(map, "name");
            String cond = MapUtils.getString(map, "conditions");
            String value = MapUtils.getString(map, "value");

            /*if ("AND".equalsIgnoreCase(cond)){
                builder.must(QueryBuilders.matchQuery(name,value).operator(Operator.AND));
            }*/
            if ("OR".equalsIgnoreCase(cond)){
                builder.should(QueryBuilders.matchQuery(name,value).operator(Operator.OR));
            }
        }
        searchSourceBuilder.query(builder);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("----->"+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



    }










}
