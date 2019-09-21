package com.example.drools;

import com.alibaba.fastjson.JSONObject;
import com.example.drools.config1.ElasticConnection;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoDroolsApplicationTests {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    private RestHighLevelClient client;

    @Test
    public void createIndex2(){
        Book b = new Book();
        b.setId(1);
        b.setBookName("出发口岸北京,常驻城市北京,有钱人,25");
        bookRepository.index( b );

        Book b2 = new Book();
        b2.setId(2);
        b2.setBookName("出发口岸北京,常驻城市上海,有钱人,25");
        bookRepository.index( b2 );

        Book b3 = new Book();
        b3.setId(3);
        b3.setBookName("出发口岸上海,常驻城市上海,没钱人,26");
        bookRepository.index( b3 );

        Book b4 = new Book();
        b4.setId(4);
        b4.setBookName("出发口岸上海,常驻城市北京,没钱人,26");
        bookRepository.index( b4 );
    }

    @Test
    public void find(){
        List<Book> list5 = bookRepository.findByBookName( "北京" );
        System.out.println(list5);
    }


    @Test
    public void useFind() {
        List<Book> list = bookRepository.findByBookName( "出发口岸北京,常驻城市北京" );
        System.out.println(list);

    }

    @Test
    public void test() throws IOException {
        RestHighLevelClient client = ElasticConnection.getConnection(null).getRestHighLevelClient();
        GetRequest re = new GetRequest("test","book","1");
        GetResponse documentFields = client.get(re, RequestOptions.DEFAULT);
        System.out.println(JSONObject.toJSONString(documentFields));
    }

    @Test
    public void test1() {
        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.queryStringQuery("常驻城市北京,出发口岸北京").field("bookName"));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(1000);//每次查询1000条
        searchSourceBuilder.query(queryBuilder);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("book");//设置类型
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println(hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test2() throws IOException {
        GetRequest re = new GetRequest("test","book","1");
        GetResponse documentFields = client.get(re, RequestOptions.DEFAULT);
        System.out.println(JSONObject.toJSONString(documentFields));
    }



}
