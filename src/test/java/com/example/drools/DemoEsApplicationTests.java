package com.example.drools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.fastjson.JSONObject;
import com.example.drools.excel.ExcelListener;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.*;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import sun.misc.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoEsApplicationTests {


    @Autowired
    private RestHighLevelClient client;

    @Autowired
    ExcelListener excelListener;

    @Test
    public void testExcelCreate() throws IOException {
        // 写法1：
        //String fileName = "/Users/admin/Downloads/blood.xlsx";
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        //EasyExcel.read(fileName, Object.class, excelListener).sheet().doRead();
        ClassPathResource resource = new ClassPathResource("blood.xlsx");
        EasyExcel.read(resource.getInputStream(),excelListener).sheet().doRead();

       /* ExcelReader reader = EasyExcelFactory.read(resource.getInputStream(),excelListener).build();
        reader.read();
        reader.finish();*/
    }


    @Test
    public void testExport(){
        String fileName = "/Users/admin/Downloads/1.xlsx";
        List<String> strings = new ArrayList<>();
        strings.add("name");
        strings.add("sex");
        List<String> strings2 = new ArrayList<>();
        strings2.add("sex");

        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        WriteFont contentWriteFont = new WriteFont();
        // 字体大小
        contentWriteFont.setFontHeightInPoints((short)12);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        HorizontalCellStyleStrategy horizontalCellStyleStrategy =
                new HorizontalCellStyleStrategy(headWriteCellStyle,contentWriteCellStyle);
        EasyExcel.write(fileName).sheet("模板").registerWriteHandler(horizontalCellStyleStrategy).doWrite(Arrays.asList(strings));
    }



    @Test
    public void updateIndexBulk() throws IOException {
        UpdateByQueryRequest request = new UpdateByQueryRequest("test");

        // 更新时版本冲突
        request.setConflicts("proceed");
        request.setQuery(QueryBuilders.matchQuery ("sex", "男士"));
        // 更新最大文档数
        //request.setSize(10);
        // 批次大小
        request.setBatchSize(1000);
        //		request.setPipeline("my_pipeline");

        request.setScript(new Script(ScriptType.INLINE, "painless",
                String.format( "ctx._source.sex = '男'"), Collections.emptyMap()));
        // 并行
        request.setSlices(2);
        // 使用滚动参数来控制“搜索上下文”存活的时间
        request.setScroll(TimeValue.timeValueMinutes(10));
        // 如果提供路由，则将路由复制到滚动查询，将流程限制为匹配该路由值的切分
        //		request.setRouting("=cat");

        // 可选参数
        // 超时
        request.setTimeout(TimeValue.timeValueMinutes(2));
        // 刷新索引
        request.setRefresh(true);


        BulkByScrollResponse bulkResponse = client.updateByQuery(request, RequestOptions.DEFAULT);
        TimeValue timeTaken = bulkResponse.getTook();
        boolean timedOut = bulkResponse.isTimedOut();
        long totalDocs = bulkResponse.getTotal();
        long updatedDocs = bulkResponse.getUpdated();
        long deletedDocs = bulkResponse.getDeleted();
        long batches = bulkResponse.getBatches();
        long noops = bulkResponse.getNoops();
        long versionConflicts = bulkResponse.getVersionConflicts();
        System.out.println("花费时间：" + timeTaken + ",是否超时：" + timedOut + ",总文档数：" + totalDocs + ",更新数：" +
                updatedDocs + ",删除数：" + deletedDocs + ",批量次数：" + batches + ",跳过数：" + noops + ",冲突数：" + versionConflicts);
        List<ScrollableHitSource.SearchFailure> searchFailures = bulkResponse.getSearchFailures();  // 搜索期间的故障
        searchFailures.forEach(e -> {
            System.err.println("Cause:" + e.getReason().getMessage() + "Index:" + e.getIndex() + ",NodeId:" + e.getNodeId() + ",ShardId:" + e.getShardId());
        });
        List<BulkItemResponse.Failure> bulkFailures = bulkResponse.getBulkFailures();   // 批量索引期间的故障
        bulkFailures.forEach(e -> {
            System.err.println("Cause:" + e.getCause().getMessage() + "Index:" + e.getIndex() + ",Type:" + e.getType() + ",Id:" + e.getId());
        });


    }

    @Test
    public void updateIndex() throws IOException {

        TermQueryBuilder queryBuilder = QueryBuilders.termQuery("sex", "男士");

        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        searchSourceBuilder.size(1000);
        System.out.println(searchSourceBuilder);
        Map<String,Object> sourceAsMap = new HashMap<>() ;
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            int i = 1;
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("------++---------->第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
                sourceAsMap = hit.getSourceAsMap();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(sourceAsMap);

        /*sourceAsMap.put("location","印度");
        ObjectMapper mapper = new ObjectMapper();
        byte[] json = mapper.writeValueAsBytes(sourceAsMap);
        UpdateRequest updateRequest = new UpdateRequest("test", "tag", "569728")
                .fetchSource(true);
        updateRequest.doc(json,XContentType.JSON);
        client.update(updateRequest,RequestOptions.DEFAULT);*/
    }

    @Test
    public void del() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest("test", "tag","569796");
        client.delete(deleteRequest,RequestOptions.DEFAULT);
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
    public void test1() {

        JSONObject j = JSONObject.parseObject(parseJson());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);
        String outConditions = j.getString("out_conditions");

        SearchRequest searchRequest = new SearchRequest("test");//设置查询索引
        BoolQueryBuilder boolQueryBuilder0 = QueryBuilders.boolQuery();
        BoolQueryBuilder boolQueryBuilder1 = QueryBuilders.boolQuery();

        for (Object obj: tags) {
            Map map = (Map) obj;
            String inConditions = MapUtils.getString(map, "in_conditions");
            Object content = MapUtils.getObject(map, "content");
            List<Map> maps = JSONObject.parseArray(content.toString(), Map.class);
            BoolQueryBuilder boolQueryBuilder2 = QueryBuilders.boolQuery();
            for (Map coMap:maps) {
                String name = MapUtils.getString(coMap, "name");
                String cond = MapUtils.getString(coMap, "conditions");
                String value = MapUtils.getString(coMap, "value");
                //第三层
                if ("IN".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.must(QueryBuilders.matchPhrasePrefixQuery(name, value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.should(QueryBuilders.matchPhrasePrefixQuery(name, value));
                    }
                }
                if ("NOT".equalsIgnoreCase(cond)){
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        System.out.println("name:"+name+":value:"+value);
                        boolQueryBuilder2.mustNot(QueryBuilders.matchPhrasePrefixQuery(name,value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.mustNot(QueryBuilders.matchPhrasePrefixQuery(name, value) );
                    }


                }
            }
            if ("AND".equalsIgnoreCase(outConditions)) {
                boolQueryBuilder1.must(boolQueryBuilder2);
            }
            if ("OR".equalsIgnoreCase(outConditions)) {
                boolQueryBuilder1.should(boolQueryBuilder2);
            }

        }

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder1);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        searchSourceBuilder.size(1000);
        System.out.println(searchSourceBuilder);
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            int i = 1;
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("------++-->第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test2() {

        JSONObject j = JSONObject.parseObject(parseJson());
        List<Map> tags = j.getJSONArray("tags").toJavaList(Map.class);
        String outConditions = j.getString("out_conditions");

        SearchRequest searchRequest = new SearchRequest("30");//设置查询索引
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        for (Object obj: tags) {
            Map map = (Map) obj;
            String inConditions = MapUtils.getString(map, "in_conditions");
            Object content = MapUtils.getObject(map, "content");
            List<Map> maps = JSONObject.parseArray(content.toString(), Map.class);

            BoolQueryBuilder boolQueryBuilder1 = QueryBuilders.boolQuery();


            for (Map coMap:maps) {
                BoolQueryBuilder boolQueryBuilder2 = QueryBuilders.boolQuery();
                BoolQueryBuilder boolQueryBuilderNot = QueryBuilders.boolQuery();
                String name = MapUtils.getString(coMap, "name");
                String cond = MapUtils.getString(coMap, "conditions");
                String value = MapUtils.getString(coMap, "value");
                //第三层
                if ("NOT".equalsIgnoreCase(cond) || "NIN".equalsIgnoreCase(cond)) {

                    if ("AND".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilderNot.should(QueryBuilders.matchPhrasePrefixQuery(name + ".keyword", value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilderNot.must(QueryBuilders.matchPhrasePrefixQuery(name + ".keyword", value));
                    }
                }else {
                    if ("AND".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.must(QueryBuilders.matchPhrasePrefixQuery(name + ".keyword", value));
                    }
                    if ("OR".equalsIgnoreCase(inConditions)) {
                        boolQueryBuilder2.should(QueryBuilders.matchPhrasePrefixQuery(name + ".keyword", value));
                    }
                }
                if (boolQueryBuilderNot.hasClauses()){
                    boolQueryBuilder1.mustNot(boolQueryBuilderNot);
                }
                if (boolQueryBuilder2.hasClauses()) {
                    boolQueryBuilder1.must(boolQueryBuilder2);
                }
            }
            if ("AND".equalsIgnoreCase(outConditions)) {
                boolQueryBuilder.must(boolQueryBuilder1);
            }
            if ("OR".equalsIgnoreCase(outConditions)) {
                boolQueryBuilder.mustNot(boolQueryBuilder1);
            }
        }



        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);//设置查询条件
        searchRequest.source(searchSourceBuilder);
        searchRequest.types("tag");//设置类型
        searchSourceBuilder.size(1000);
        System.out.println("-------"+searchSourceBuilder);
        try {
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            int i = 1;
            for (SearchHit hit : response.getHits().getHits()) {
                System.out.println("------++------>第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test16() throws IOException {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.from(0);

        List<String> list3 = new ArrayList<>();
        list3.add("德国");
        TermsQueryBuilder bool = QueryBuilders.termsQuery("欧洲发达国家.keyword", list3);

        List<String> list = new ArrayList<>();
        list.add("2");
        list.add("3");
        list.add("4");
        TermsQueryBuilder start = QueryBuilders.termsQuery("成单周期.keyword", list);

        MatchPhrasePrefixQueryBuilder matchPhrasePrefixQueryBuilder2 = QueryBuilders.matchPhrasePrefixQuery("性别.keyword", "女");
        BoolQueryBuilder bool2 = QueryBuilders.boolQuery().should(matchPhrasePrefixQueryBuilder2);

        List<String> list2 = new ArrayList<>();
        list2.add("2-19");
        TermsQueryBuilder start2 = QueryBuilders.termsQuery("年龄.keyword", list2);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(bool).should(bool2).mustNot(start).mustNot(start2);
        sourceBuilder.query(boolQuery);
        sourceBuilder.size(50);
        SearchRequest searchRequest = new SearchRequest("30");
        searchRequest.source(sourceBuilder);

        System.out.println(sourceBuilder);
        //聚合条件
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        int i =1;
        for (SearchHit hit : hits) {
            System.out.println("------++-->第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
        }
        client.close();
    }


    /**
     * 只查询country 包含 日本  或  韩国 并且 性别 男 并且 出发口岸 不包含 北京 和 上海 和 香港
     */
    @Test
    public void test5() {

        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.from(0);

            MatchPhrasePrefixQueryBuilder matchPhrasePrefixQueryBuilder = QueryBuilders.matchPhrasePrefixQuery("country", "日本");
            PrefixQueryBuilder prefixQueryBuilder = QueryBuilders.prefixQuery("country", "韩国");

            BoolQueryBuilder bool = QueryBuilders.boolQuery().should(matchPhrasePrefixQueryBuilder).should(prefixQueryBuilder);

            TermQueryBuilder sex = QueryBuilders.termQuery("sex", "男");
            List<String> list = new ArrayList<>();
            list.add("香港");
            list.add("北京市");
            list.add("上海市");
            TermsQueryBuilder start = QueryBuilders.termsQuery("startOfPort", list);

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(bool).must(sex).mustNot(start);

            sourceBuilder.query(boolQuery);
            sourceBuilder.size(50);
            SearchRequest searchRequest = new SearchRequest("test");
            searchRequest.source(sourceBuilder);

            System.out.println(sourceBuilder);
            //聚合条件
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            int i =1;
            for (SearchHit hit : hits) {
                System.out.println("------++-->第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
            }
            client.close();
        } catch (Exception e) {
            e.getStackTrace();
            System.out.println(e.getMessage());
        }
    }



    /**
     * 只查询country 包含 日本  和  韩国
     */
    @Test
    public void test(){
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.from(0);

            //     MatchQueryBuilder sex = QueryBuilders.matchQuery("sex", "未知");
            MatchQueryBuilder country = QueryBuilders.matchQuery("country.keyword", "印度");
           // MatchQueryBuilder country2 = QueryBuilders.matchQuery("country.keyword", "韩国");
            //TermQueryBuilder country2 = QueryBuilders.termQuery("sex.keyword", "男");
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(country);
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(50);
            SearchRequest searchRequest = new SearchRequest("test");
            searchRequest.source(sourceBuilder);
            System.out.println(sourceBuilder);
            //聚合条件
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                //System.out.println(hit.getSourceAsString());
                System.out.println("------++-->第 "+hit.getSourceAsMap());
            }

            client.close();
        } catch (Exception e) {
            e.getStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * 只查询country 包含 日本  或  韩国
     */
    @Test
    public void test3(){
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.from(0);
            MatchPhrasePrefixQueryBuilder matchPhrasePrefixQueryBuilder = QueryBuilders.matchPhrasePrefixQuery("country", "日本");
            PrefixQueryBuilder prefixQueryBuilder = QueryBuilders.prefixQuery("country", "韩国");
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().should(matchPhrasePrefixQueryBuilder).should(prefixQueryBuilder);
            sourceBuilder.query(boolQuery);
            sourceBuilder.size(50);
            SearchRequest searchRequest = new SearchRequest("test");
            searchRequest.source(sourceBuilder);

            System.out.println(sourceBuilder);
            //聚合条件
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            SearchHits hits = response.getHits();
            for (SearchHit hit : hits) {
                System.out.println(hit.getSourceAsString());
            }

            client.close();
        } catch (Exception e) {
            e.getStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * 只查询country 不包含 日本 和 韩国
     */
    @Test
    public void test4(){

        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.from(0);

            MatchPhrasePrefixQueryBuilder matchPhrasePrefixQueryBuilder = QueryBuilders.matchPhrasePrefixQuery("country", "日本");
            PrefixQueryBuilder prefixQueryBuilder = QueryBuilders.prefixQuery("country", "韩国");
            BoolQueryBuilder notAnd = QueryBuilders.boolQuery().must(matchPhrasePrefixQueryBuilder).must(prefixQueryBuilder);


            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().mustNot(notAnd);

            sourceBuilder.query(boolQuery);
            sourceBuilder.size(50);
            SearchRequest searchRequest = new SearchRequest("test");
            searchRequest.source(sourceBuilder);

            System.out.println(sourceBuilder);
            //聚合条件
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            SearchHits hits = response.getHits();
            int i =1;
            for (SearchHit hit : hits) {
                System.out.println("------++-->第 "+ (i ++) +" 条 "+hit.getSourceAsMap());
            }

            client.close();
        } catch (Exception e) {
            e.getStackTrace();
            System.out.println(e.getMessage());
        }
    }



    @Test
    public void test100(){
        String str = "\"2018/09/09\"";

        //String path = "\".*\"";


        String path = "\"\\d{4}[/]\\d{2}[/]\\d{2}\"";

          //String path = "\\d{4}[/]\\d{2}[/]\\d{2}(\\s\\d{2}:\\d{2}:\\d{2})";//定义匹配规则
       // String path = "\\d{4}[/]\\d{1,2}[/]\\d{1,2}(\\s\\d{2}:\\d{2}(:\\d{2})?)?";
        Pattern p= Pattern.compile(path);//实例化Pattern
        Matcher m=p.matcher(str);//验证字符串内容是否合法
        System.out.println(m.matches());

        String s = "NUll";
        String[] split = s.split(",");
        if (split.length >1){
            System.out.println("多个值");
        }
        System.out.println(split[0].toLowerCase());
        if(StringUtils.isBlank(split[0]) || "null".equalsIgnoreCase(split[0].toLowerCase())) System.out.println("没有啊");
    }


    @Test
    public void test102() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        buffer.put("我爱你我的中国  ".getBytes());
        buffer.put("\n".getBytes());
        buffer.put("我和我的祖国".getBytes());
        System.out.println("刚写完数据 " + buffer);
        //buffer.flip();
        System.out.println("flip之后 " + buffer);
        buffer.flip();
        byte[] target = new byte[buffer.limit()];
        /*buffer.get(target);//自动读取target.length个数据
        for(byte b : target){
            System.out.println(b);
        }*/
        System.out.println("读取完数组 " + buffer);
        buffer.get(target);
        Files.write(Paths.get("/Users/admin/Downloads/11.txt"), target);
        buffer.clear();
    }






}
