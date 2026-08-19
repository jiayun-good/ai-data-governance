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
 * 数据源信息表
 * </p>
 *
 * @author author
 * @since 2026-08-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("data_source")
@Schema(description = "数据源信息表")
public class DataSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "数据源名称")
    private String name;

    @Schema(description = "数据库类型，如 MYSQL、POSTGRESQL")
    private String type;

    @Schema(description = "数据库主机地址")
    private String host;

    @Schema(description = "数据库端口")
    private Integer port;

    @Schema(description = "数据库名称")
    private String databaseName;

    @Schema(description = "数据库用户名")
    private String username;

    @Schema(description = "数据库密码")
    private String password;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "数据源描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
