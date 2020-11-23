package cn.coderblue.mongodb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author coderblue
 */
@Controller
@RequestMapping("/")
public class BaseController {

    @GetMapping("/index")
    public String home() {
        System.out.println("通过Controller控制器层跳转访问的资源，记得添加 thymeleaf 解析视图..");
        return "/index";
    }
}
