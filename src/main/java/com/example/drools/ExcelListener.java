package com.example.drools;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellData;
import com.example.drools.es.EsOperate;
import com.example.drools.mongo.MongoOp;
import com.google.common.collect.Maps;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class ExcelListener extends AnalysisEventListener<Object> {

    private final EsOperate es;

    @Autowired
    private MongoOp mongoOp;

    /**
     * 每隔5条存储数据库，实际使用中可以3000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 10;
    private List<Map<Integer,Object>> list = new ArrayList<>();
    private int countSize = 1;

    private Map<Integer,String> headMap = new HashMap<>();

    private List<Map<String,Object>> mapList = new ArrayList<>();
    private List<Map<String,Object>> mapList2 = new ArrayList<>();

    private List<Map<String,Object>> mapListBasic = new ArrayList<>();

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
        //createMongoCollection();
        //createEsIndex();
        createEsIndex();
        //createEsBasic();
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", list.size());
        log.info("存储数据库成功！偏移量{}",countSize);
        handler();
        handler2();
    }

    private void handler(){
        for (Map<Integer, Object> objectMap : list) {
            Map<String,Object> map = new HashMap<>();
            Map<String,Object> mapBasic = new HashMap<>();
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

               if (v.equalsIgnoreCase("country")){
                   String s = val.toString();
                   val = s.split(",");
               }

               map.put(v, val);


           });

           mapList.add(map);
        }

    }

    private void handler2(){
        for (Map<Integer, Object> objectMap : list) {
            Map<String,Object> map = new HashMap<>();
            Map<String,Object> mapBasic = new HashMap<>();
            headMap.forEach((k,v)->{
                Object val = objectMap.get(k);
                if (v.equalsIgnoreCase("sex")){
                    if (Integer.parseInt(val.toString()) ==2){
                        val = "男";
                        System.out.println();
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

               /*if (v.equalsIgnoreCase("country")){
                   String s = val.toString();
                   val = s.split(",");
               }*/

                map.put(v, val);


            });

            mapList2.add(map);
        }

    }

    private Map<String, Object> buildBasicMap(Map<Integer,Object> objectMap){
        Map<String, Object> basicMap = Maps.newHashMap();
        ArrayList<String> arrayList = new ArrayList<>(headMap.values());
        int idIndex = arrayList.indexOf("uid");
        int sexIndex = arrayList.indexOf("sex");
        String id = objectMap.get(idIndex).toString();
        String sex = objectMap.get(sexIndex).toString();
        basicMap.put("uid",id);
        basicMap.put("sex",sex.equals("2")? "男" : sex.equalsIgnoreCase("3") ? "女" : "未知");
        return basicMap;
    }

    private void createMongoCollection(){
        log.info("mongo create collection in...");
        mongoOp.createCollection("test",mapList);
    }

    private void createEsBasic(){
        log.info("create es basic index:{},total:{}",mapListBasic,countSize);
        es.createIndex("basic","basic",mapListBasic);
    }

    private void createEsIndex()  {
        log.info("create es index:{},total:{}",mapList,countSize);
        es.createIndex("test","tag",mapList);
    }

    private void createEsIndex2()  {
        log.info("create es index2:{},total:{}",mapList2,countSize);
        es.createIndex("test2","tag",mapList2);
    }


}
