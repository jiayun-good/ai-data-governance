package com.mp.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {


    @Resource
    private RedisTemplate<String,Object> redisTemplate;



    public void set(
            String key,
            Object value,
            long time){

        redisTemplate.opsForValue()
                .set(key,value,time, TimeUnit.SECONDS);
    }



    public Object get(String key){

        return redisTemplate
                .opsForValue()
                .get(key);
    }



    public void delete(String key){

        redisTemplate.delete(key);
    }

}