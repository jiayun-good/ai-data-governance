package com.mp.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Schema(description = "质量检查历史VO")
public class QualityCheckHistoryVO {


    @Schema(description = "检查记录ID")
    private Long id;


    @Schema(description = "规则ID")
    private Long ruleId;


    @Schema(description = "规则名称")
    private String ruleName;


    @Schema(description = "数据源ID")
    private Long datasourceId;


    @Schema(description = "检测表")
    private String tableName;


    @Schema(description = "检测字段")
    private String columnName;


    @Schema(description = "总数据量")
    private Long totalCount;


    @Schema(description = "正常数量")
    private Long successCount;


    @Schema(description = "异常数量")
    private Long errorCount;


    @Schema(description = "执行状态")
    private String status;


    @Schema(description = "失败原因")
    private String errorMessage;


    @Schema(description = "执行时间")
    private LocalDateTime createTime;


}