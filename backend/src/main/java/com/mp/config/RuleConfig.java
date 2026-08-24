package com.mp.config;

import lombok.Data;

import java.util.List;

@Data
public class RuleConfig {

    // ========== RANGE 范围校验 ==========

    /**
     * 最小值（包含）
     */
    private Double min;

    /**
     * 最大值（包含）
     */
    private Double max;

    // ========== LENGTH 长度校验 ==========

    /**
     * 最小长度（包含）
     */
    private Integer minLength;

    /**
     * 最大长度（包含）
     */
    private Integer maxLength;

    // ========== REGEX 正则校验 ==========

    /**
     * 正则表达式
     */
    private String pattern;

    // ========== ENUM 枚举校验 ==========

    /**
     * 允许的枚举值列表
     */
    private List<String> values;

    // ========== CUSTOM_SQL 自定义SQL（高级用户专用，AI不生成） ==========

    /**
     * 自定义检测SQL，查询结果中异常数据的数量
     */
    private String customSql;
}
