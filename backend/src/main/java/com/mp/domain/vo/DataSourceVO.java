package com.mp.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class DataSourceVO {

    private Long id;

    private String name;

    private String type;

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    private LocalDateTime createTime;
}