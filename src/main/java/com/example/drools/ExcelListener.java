package com.example.drools;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellData;
import com.example.drools.es.EsOperate;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author
 * @since 2019/9/25
 */

@Slf4j
@Service
public class ExcelListener extends AnalysisEventListener<Object> {

    private final EsOperate es;

    /**
     * 每隔5条存储数据库，实际使用中可以3000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 5;
    private List<Map<Integer,Object>> list = new ArrayList<>();
    private int countSize = 1;

    private Map<Integer,String> headMap = new HashMap<>();

    private List<Map<String,Object>> mapList = new ArrayList<>();

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context){
        log.info("table head {}",headMap);
        this.headMap = headMap;
    }

    @Autowired
    public ExcelListener(EsOperate es) {
        this.es = es;
    }

    @Override
    public void invoke(Object data, AnalysisContext context) {
        if (data instanceof Map) {
            list.add((Map<Integer, Object>) data);
            countSize ++;
        }
        if (list.size() >= BATCH_COUNT) {
            saveData();
            list.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        log.info("所有数据解析完成！{}",countSize);
        createEsIndex();
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", list.size());
        log.info("存储数据库成功！偏移量{}",countSize);
        handler();
    }

    private void handler(){
        for (Map<Integer, Object> objectMap : list) {
            Map<String,Object> map = new HashMap<>();
           headMap.forEach((k,v)->{
               Object val = objectMap.get(k);
               if (v.equalsIgnoreCase("sex")){
                   if (Integer.parseInt(val.toString()) ==2){
                       val = "男";
                   }
                   else if (Integer.parseInt(val.toString()) ==3){
                       val = "女";
                   }else{
                       val = "未知";
                   }
               }
               if (v.equalsIgnoreCase("是否带孩子")){
                   if (Integer.parseInt(val.toString()) ==1){
                       val = "亲子";
                   }else {
                       val = "非亲子";
                   }
               }
               map.put(v, val);
           });
           mapList.add(map);
        }



    }

    private void createEsIndex()  {
        log.info("create es index:{},total:{}",mapList,countSize);
        es.createIndex("test","tag",mapList);
    }
}
