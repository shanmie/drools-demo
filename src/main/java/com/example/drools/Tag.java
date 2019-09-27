package com.example.drools;

import lombok.Data;

@Data
public class Tag {
    private int uid;
    private String country;
    private String 出发口岸;
    private String sex;
    private String 是否带孩子;
    private int 决策周期;
    private String location;

}
