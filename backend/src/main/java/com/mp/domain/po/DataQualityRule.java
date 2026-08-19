package com.mp.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("data_quality_rule")
@Schema(description = "数据质量规则对象")
public class DataQualityRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "字段名")
    private String columnName;

    @Schema(description = "规则类型，如非空校验、唯一性校验、长度校验")
    private String ruleType;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "规则配置(JSON格式)")
    private String ruleConfig;

    @Schema(description = "是否启用：1启用，0禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}