package com.example.drools;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 2019/10/12
 */
public class ErrorCapacity extends ArrayList<String> {
    private List<String> errorCapacityList = new ArrayList<>();

    private static ErrorCapacity errorCapacity;

    public static ErrorCapacity getInstance(){
        if (errorCapacity == null){
            errorCapacity = new ErrorCapacity();
        }
        return errorCapacity;
    }

    @Override
    public boolean add(String obj){
        return errorCapacityList.add(obj);
    }

    public List<String> getErrorCapacityList(){
        return errorCapacityList;
    }


}
