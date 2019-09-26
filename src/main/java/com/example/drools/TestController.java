package com.example.drools;

import com.example.drools.util.KieSessionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
