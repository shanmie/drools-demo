package com.example.drools;

import com.example.drools.util.KieSessionUtil;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @RequestMapping("/test")
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
