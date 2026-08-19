package com.mp.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "质量规则执行结果")
public class QualityCheckResultVO {

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "检测表")
    private String tableName;

    @Schema(description = "检测字段")
    private String columnName;

    @Schema(description = "总数据量")
    private Long  total;

    @Schema(description = "通过数量")
    private Long  successCount;

    @Schema(description = "异常数量")
    private Long  errorCount;

    @Schema(description = "异常数据")
    private List<Map<String,Object>> errorData;
}