package com.mp.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI生成规则预览结果")
public class AiRulePreviewVO {

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "字段名")
    private String columnName;

    @Schema(description = "规则类型")
    private String ruleType;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则配置(JSON格式)")
    private String ruleConfig;

    @Schema(description = "AI生成的规则描述")
    private String description;
}
