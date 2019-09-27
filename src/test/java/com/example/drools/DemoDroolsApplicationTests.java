package com.example.drools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import sun.misc.IOUtils;

import java.io.*;
import java.util.List;
import java.util.Map;

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
    public void test2(){
        JSONObject j = JSONObject.parseObject(parseJson());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);

        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        for (Object obj: tags) {
            Map map = (Map) obj;
            String name = MapUtils.getString(map, "name");
            String cond = MapUtils.getString(map, "conditions");
            String value = MapUtils.getString(map, "value");

            if ("AND".equalsIgnoreCase(cond)){
                builder.must(QueryBuilders.matchQuery(name,value).operator(Operator.AND));
            }
            if ("OR".equalsIgnoreCase(cond)){
                builder.should(QueryBuilders.matchQuery(name,value).operator(Operator.OR));
            }
        }
        searchSourceBuilder.query(builder);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        searchSourceBuilder.size(1000);
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("-------------->"+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    @Test
    public void test3(){
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
    public void test4() {

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
                        boolQueryBuilder2.must(QueryBuilders.matchQuery(name, value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.should(QueryBuilders.matchQuery(name, value));
                    }
                }
                if ("NOT".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        System.out.println("name:"+name+":value:"+value);
                        boolQueryBuilder2.mustNot(QueryBuilders.termQuery(name,value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.mustNot(QueryBuilders.matchQuery(name, value) );
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



}
