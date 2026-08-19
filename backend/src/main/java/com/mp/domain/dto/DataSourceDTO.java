package com.mp.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增数据源请求参数")
public class DataSourceDTO {

    @Schema(description = "数据库主机地址", example = "127.0.0.1")
    private String host;

    @Schema(description = "数据库端口", example = "3306")
    private Integer port;

    @Schema(description = "数据库名称", example = "hmdp")
    private String databaseName;

    @Schema(description = "数据库用户名", example = "root")
    private String username;

    @Schema(description = "数据库密码", example = "123456")
    private String password;

    @Schema(description = "数据源名称", example = "本地hmdp库")
    private String name;

    @Schema(description = "数据库类型", example = "MYSQL")
    private String type;
}