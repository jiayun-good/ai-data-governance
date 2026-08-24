package com.mp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 向 Spring 容器注册一个 RestTemplate 对象，让 Java 后端可以通过依赖注入的方式调用其他 HTTP 服务。 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
