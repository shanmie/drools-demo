package com.example.drools;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoDroolsApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Autowired
    BookRepository bookRepository;

    @Test
    public void createIndex2(){
        Book book = new Book();
        book.setId(1);
        book.setBookName("西游记");
        book.setAuthor( "吴承恩" );

        Book b = new Book();
        b.setId(2);
        b.setBookName("北京，有钱人，25");

        Book b2 = new Book();
        b2.setId(3);
        b2.setBookName("上海，有钱人，25");
        bookRepository.index( book );
        bookRepository.index( b );
        bookRepository.index( b2 );
    }

    @Test
    public void useFind() {
        List<Book> list = bookRepository.findByBookNameLike( "北京，有钱人" );
        List<Book> list1 = bookRepository.findByBookNameLike( "有钱人" );
        List<Book> list2 = bookRepository.findByBookNameLike( "25" );
        List<Book> list3 = bookRepository.findByBookNameLike( "25，有钱人" );
        List<Book> list4 = bookRepository.findByBookNameLike( "25，有钱人，北京" );
        List<Book> list5 = bookRepository.findByBookNameLike( "北京，有钱人，25" );
        System.out.println(list);
        System.out.println(list1);
        System.out.println(list2);
        System.out.println(list3);
        System.out.println(list4);
        System.out.println(list5);

    }

}
