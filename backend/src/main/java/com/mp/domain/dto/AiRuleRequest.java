package com.mp.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI生成数据质量规则请求参数")
public class AiRuleRequest {

    @Schema(description = "数据源ID", example = "1")
    private Long datasourceId;

    @Schema(description = "业务描述", example = "用户表的名称列不能为空")
    private String description;
}
