package com.example.drools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.sun.tools.internal.ws.wsdl.document.soap.SOAPUse;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.util.Strings;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.recycler.Recycler;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
import java.util.Arrays;
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
        JSONObject j = JSONObject.parseObject(parseJson());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);
        String outConditions = j.getString("out_conditions");

        Query query = new Query();

        Criteria criteria = new Criteria();
        Criteria andCri = new Criteria();
        Criteria orCri = new Criteria();

        List<Criteria> andCriList = new ArrayList<>();
        List<Criteria> orCriList = new ArrayList<>();

        List<String> not = new ArrayList<>();

        for (Object obj: tags) {
            Map map = (Map) obj;
            String inConditions = MapUtils.getString(map, "in_conditions");
            Object content = MapUtils.getObject(map, "content");
            List<Map> maps = JSONObject.parseArray(content.toString(), Map.class);


            for (Map coMap : maps) {
                String name = MapUtils.getString(coMap, "name");
                String cond = MapUtils.getString(coMap, "conditions");
                String value = MapUtils.getString(coMap, "value");

                if ("IN".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        andCriList.add(Criteria.where(name).regex(".*" + value + ".*"));

                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        orCriList.add(Criteria.where(name).regex(".*" + value + ".*"));
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

            if ("AND".equalsIgnoreCase(inConditions)) {
                andCri.andOperator(andCriList.toArray(new Criteria[andCriList.size()]));
            }
            if ("OR".equalsIgnoreCase(inConditions)) {
                orCri.orOperator(orCriList.toArray(new Criteria[orCriList.size()]));
            }


        }

        if ("AND".equalsIgnoreCase(outConditions)) {
            criteria.andOperator(andCri,orCri);
        }
        if ("OR".equalsIgnoreCase(outConditions)) {
            criteria.orOperator(andCri,orCri);
        }
        query.addCriteria(criteria);
        System.out.println(query);
        List<Map> test = mongoTemplate.find(query, Map.class, "test");
        System.out.println("size:---"+test.size());
        System.out.println("test -map:\n"+test);
    }
}
