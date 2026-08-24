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

    // ========== UNIQUE 唯一性校验 ==========

    /**
     * 是否忽略NULL值（唯一性校验时，NULL是否参与去重判断）
     */
    private Boolean ignoreNull = true;

    // ========== RANGE 范围校验 ==========

    /**
     * 最小值（包含）
     */
    private Double min;

    /**
     * 最大值（包含）
     */
    private Double max;

    // ========== FORMAT 格式校验 ==========

    /**
     * 正则表达式（用于FORMAT规则）
     */
    private String pattern;

    // ========== CUSTOM_SQL 自定义SQL校验 ==========

    /**
     * 自定义检测SQL，查询结果中异常数据的数量
     */
    private String customSql;

}