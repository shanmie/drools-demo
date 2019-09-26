package com.example.drools;

import lombok.Data;

/**
 * @author deacon
 * @since 2019/9/24
 */
@Data
public class ExcelColumnVo {
    int uid;
    int sex;
    String startOfPort;
    int parentChild;
    String country;
    int day;
    String location;

    String sexZH(int val) {
        switch (val) {
            case 1:
                return "男";
            case 2:
                return "男";
            default:
                return "未知";
        }
    }

    String parentChildZH(int val) {
        switch (val) {
            case 1:
                return "亲子";
            default:
                return "非亲子";
        }
    }
}
