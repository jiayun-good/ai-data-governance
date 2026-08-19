package com.mp.domain.vo;

import lombok.Data;

@Data
public class ColumnVO {


    /**
     * 字段名称
     */
    private String columnName;


    /**
     * 字段类型
     */
    private String dataType;


    /**
     * 字段长度
     */
    private Integer length;


    /**
     * 是否允许为空
     */
    private Boolean nullable;


    /**
     * 字段备注
     */
    private String comment;
}