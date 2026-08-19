package com.mp.config;

import lombok.Data;

@Data
public class RuleConfig {


    /**
     * 是否检查NULL
     */
    private Boolean checkNull = true;


    /**
     * 是否检查空字符串
     */
    private Boolean checkEmpty = true;


    /**
     * 是否去除空格
     */
    private Boolean trim = true;

}