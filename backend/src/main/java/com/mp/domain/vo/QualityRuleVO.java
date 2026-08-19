package com.mp.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "数据质量规则返回对象")
public class QualityRuleVO {

    @Schema(description = "规则ID", example = "1")
    private Long id;

    @Schema(description = "规则名称", example = "用户姓名不能为空")
    private String ruleName;

    @Schema(description = "规则类型", example = "NOT_NULL")
    private String ruleType;
}