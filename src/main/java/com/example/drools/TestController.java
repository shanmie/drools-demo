package com.example.drools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.FileUtils;
import com.example.drools.util.KieSessionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Api(tags = "测试接口", description = "测试 Drools Rest API")
@RestController
public class TestController {



    @ApiOperation(value = "drools", notes = "drools测试数字")
    @GetMapping("/test")
    public String test() throws Exception {
        KieSession session = KieSessionUtil.getAllRules();
        session.insert(new Double(2));
        session.fireAllRules();
        session.dispose();
        return "ok";
    }

    @RequestMapping("/test2")
    public String test2() throws Exception {
        KieSession session = KieSessionUtil.getAllRules();
        User u = new User();
        u.setName("张三是个混蛋");
        session.insert(u);
        session.fireAllRules();
        session.dispose();
        return "ok";
    }

    @GetMapping("/down/")
    public ResponseEntity<byte[]> down() throws UnsupportedEncodingException {

        HttpHeaders headers = null;
        ByteArrayOutputStream baos = null;
        //生成Excel，出于篇幅考虑，这里省略掉，小伙伴可以直接在源码中查看
        headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", new String("1.xls".getBytes("UTF-8"), "iso-8859-1"));
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        baos = new ByteArrayOutputStream();
        //workbook.write(baos);

        List<String> aa = Arrays.asList("1","23","nihao");
        EasyExcel.write(baos).sheet("template").doWrite(Collections.singletonList(aa));

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.CREATED);


    }


}
