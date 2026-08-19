package com.mp.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Schema(description = "数据质量异常结果VO")
public class DataQualityErrorVO {


    @Schema(description = "异常ID")
    private Long id;


    @Schema(description = "检查记录ID")
    private Long checkId;


    @Schema(description = "质量规则ID")
    private Long ruleId;


    @Schema(description = "规则名称")
    private String ruleName;


    @Schema(description = "异常数据表名")
    private String tableName;


    @Schema(description = "异常字段")
    private String columnName;


    @Schema(description = "异常类型")
    private String errorType;


    @Schema(description = "异常原因")
    private String errorMessage;


    @Schema(description = "异常数据")
    private String errorData;


    @Schema(description = "产生时间")
    private LocalDateTime createTime;


}