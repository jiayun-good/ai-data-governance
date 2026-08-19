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
 * 数据质量异常数据表
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("data_quality_error")
@Schema(description="数据质量异常数据表")
public class DataQualityError implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "检查记录ID(data_quality_check_record.id)")
    private Long checkId;

    @Schema(description = "质量规则ID")
    private Long ruleId;

    @Schema(description = "异常数据表名")
    private String tableName;

    @Schema(description = "异常字段")
    private String columnName;

    @Schema(description = "异常类型，例如NOT_NULL、UNIQUE、REGEX")
    private String errorType;

    @Schema(description = "异常原因")
    private String errorMessage;

    @Schema(description = "异常数据(JSON格式)")
    private String errorData;

    @Schema(description = "产生时间")
    private LocalDateTime createTime;


}
