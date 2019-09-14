package com.example.drools;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class User {
    String name;
    int age;
    List<String> list;
    Map<String,Object> map;
}
