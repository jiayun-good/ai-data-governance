package com.mp.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据质量检查记录表
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("data_quality_check_record")
@Schema(description="数据质量检查记录表")
public class DataQualityCheckRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "质量规则ID")
    private Long ruleId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "数据源ID")
    private Long datasourceId;

    @Schema(description = "检测表名")
    private String tableName;

    @Schema(description = "检测字段")
    private String columnName;

    @Schema(description = "总数据量")
    private Long totalCount;

    @Schema(description = "正常数据数量")
    private Long successCount;

    @Schema(description = "异常数据数量")
    private Long errorCount;

    @Schema(description = "执行状态 SUCCESS成功 FAIL失败")
    private String status;

    @Schema(description = "执行失败原因")
    private String errorMessage;

    @Schema(description = "执行时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
