package com.mp.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建数据质量规则请求参数")
public class QualityRuleDTO {

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;


    @Schema(description = "表名", example = "tb_user")
    private String tableName;


    @Schema(description = "字段名", example = "name")
    private String columnName;


    @Schema(description = "规则类型", example = "NOT_NULL")
    private String ruleType;


    @Schema(description = "规则名称", example = "用户姓名不能为空")
    private String ruleName;


    @Schema(description = "规则配置(JSON格式)")
    private String ruleConfig;


    @Schema(description = "是否启用 1启用 0停用", example = "1")
    private Integer status;
}