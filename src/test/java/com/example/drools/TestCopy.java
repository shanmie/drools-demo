package com.example.drools;

import org.junit.Test;
import org.springframework.beans.BeanUtils;

/**
 * @author deacon
 * @since 2019/11/8
 */
public class TestCopy {

    @Test
    public void test(){
        A a = new A();
        a.setA("1");
        a.setB("2");
        B b = new B();
        BeanUtils.copyProperties(a,b);
        System.out.println(b);
    }
}
