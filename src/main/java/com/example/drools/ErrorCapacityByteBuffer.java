package com.example.drools;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2019/10/12
 */
public class ErrorCapacityByteBuffer extends ArrayList<String> {
    private List<String> errorCapacityList = new ArrayList<>();

    private ByteBuffer buffer  ;

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

    public void putInt(int val){
        if (buffer == null){
            buffer = ByteBuffer.allocateDirect(1024);
        }
        buffer.putInt(val);
    }

    public List<String> getErrorCapacityList(){
        return errorCapacityList;
    }


}
