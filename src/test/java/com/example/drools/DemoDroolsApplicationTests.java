package com.example.drools;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
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

    @Test
    public void createIndex2(){
        Book book = new Book();
        book.setId(1);
        book.setBookName("西游记");
        book.setAuthor( "吴承恩" );
        bookRepository.index( book );


        Book b = new Book();
        b.setId(4);
        b.setBookName("北京|有钱人|25");
        bookRepository.index( b );


        Book b2 = new Book();
        b2.setId(5);
        b2.setBookName("上海|有钱人|25");
        bookRepository.index( b2 );
    }

    @Test
    public void find(){
        List<Book> list5 = bookRepository.findByBookName( "北" );
        System.out.println(list5);
    }


    @Test
    public void useFind() {
        List<Book> list = bookRepository.findByBookName( "北京|有钱人" );
        List<Book> list1 = bookRepository.findByBookName( "有钱人" );
        List<Book> list2 = bookRepository.findByBookName( "25" );
        List<Book> list3 = bookRepository.findByBookName( "25|有钱人" );
        List<Book> list4 = bookRepository.findByBookName( "25|有钱人|北京" );
        List<Book> list5 = bookRepository.findByBookName( "北京|有钱人|25" );
        System.out.println(list);
        System.out.println(list1);
        System.out.println(list2);
        System.out.println(list3);
        System.out.println(list4);
        System.out.println(list5);

    }

    @Test
    public void test() throws IOException {
        RestHighLevelClient client = ElasticConnection.getConnection(null).getRestHighLevelClient();
        GetRequest re = new GetRequest("test","book","北京");
        GetResponse documentFields = client.get(re);
        System.out.println(documentFields.getFields());
    }



}
