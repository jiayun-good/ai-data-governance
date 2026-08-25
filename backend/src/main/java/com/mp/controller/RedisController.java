package com.mp.controller;

import com.mp.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisController {


    @Resource
    private RedisUtil redisUtil;


    @GetMapping("/test")
    public Object test(){


        redisUtil.set(
                "user:1",
                "张三111",
                300
        );


        return redisUtil.get("user:1");
    }

}