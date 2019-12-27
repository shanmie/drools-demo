package com.example.drools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.example.drools.excel.ExcelListener;
import org.apache.commons.collections4.MapUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;
import sun.misc.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoMongoApplicationTests {
    @Autowired
    ExcelListener excelListener;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testExcelCreate() throws IOException {
        // 写法1：
        //String fileName = "/Users/admin/Downloads/blood.xlsx";
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        //EasyExcel.read(fileName, Object.class, excelListener).sheet().doRead();
        ClassPathResource resource = new ClassPathResource("blood.xlsx");
        EasyExcel.read(resource.getInputStream(),excelListener).sheet().doRead();

    }

    public String parseJson(){
        ClassPathResource resource = new ClassPathResource("conditions.json");
        try {
            return new String(IOUtils.readFully(resource.getInputStream(), -1,true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Test
    public void test(){
        String s = parseJson();
        String s1 = JSONObject.toJSONString(s);
        System.out.println(s);
        System.out.println(s1);

        JSONObject j = JSONObject.parseObject(parseJson());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);
        String outConditions = j.getString("out_conditions");

        Query query = new Query();
        Criteria criteria = new Criteria();

        List<String> not = new ArrayList<>();
        List<Criteria> inCriList = new ArrayList<>();

        for (Object obj: tags) {
            Map map = (Map) obj;
            String inConditions = MapUtils.getString(map, "in_conditions");
            Object content = MapUtils.getObject(map, "content");
            List<Map> maps = JSONObject.parseArray(content.toString(), Map.class);

            List<Criteria> andCriList = new ArrayList<>();
            List<Criteria> orCriList = new ArrayList<>();

            for (Map coMap : maps) {
                String name = MapUtils.getString(coMap, "name");
                String cond = MapUtils.getString(coMap, "conditions");
                String value = MapUtils.getString(coMap, "value");

                if ("IN".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        andCriList.add(Criteria.where(name).is(value));

                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        orCriList.add(Criteria.where(name).is(value));
                    }
                }
                if ("NOT".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        not.add(value);
                        System.out.println(not);
                        andCriList.add(Criteria.where(name).not().all(not));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        not.add(value);
                        orCriList.add(Criteria.where(name).nin(not));
                    }
                }
            }
            Criteria andCri = new Criteria();
            Criteria orCri = new Criteria();
            if ("AND".equalsIgnoreCase(inConditions)) {
                andCri.andOperator(andCriList.stream().toArray(Criteria[]::new));
                inCriList.add(andCri);
            }
            if ("OR".equalsIgnoreCase(inConditions)) {
                orCri.orOperator(orCriList.toArray(new Criteria[orCriList.size()]));
                inCriList.add(orCri);
            }
        }

        if ("AND".equalsIgnoreCase(outConditions)) {
            System.out.println(criteria.toString());

            criteria.andOperator(inCriList.toArray(new Criteria[inCriList.size()]));
        }
        if ("OR".equalsIgnoreCase(outConditions)) {
            criteria.orOperator(inCriList.toArray(new Criteria[inCriList.size()]));
        }
        query.addCriteria(criteria);
        System.out.println(query);
        List<Map> test = mongoTemplate.find(query, Map.class, "test");
        System.out.println("size:---"+test.size());
        System.out.println("test -map:\n"+test);
    }
}
