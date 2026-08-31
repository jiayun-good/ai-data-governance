-- =====================================================================
-- 数据治理平台 数据库初始化脚本
-- 用途：Docker Compose 首次启动 MySQL 时自动执行（docker-entrypoint-initdb.d）
-- 作用：建库（幂等）+ 建 6 张业务表（幂等）+ 初始化默认登录用户
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `data_governance` DEFAULT CHARACTER SET utf8mb4;

USE `data_governance`;

-- ---------------------------------------------------------------------
-- 1. 系统用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码（明文比对）',
  `nickname`    VARCHAR(50)  NULL COMMENT '昵称',
  `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 默认登录账号：admin / 123456（幂等插入）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`)
SELECT 'admin', '123456', '管理员', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'admin');

-- ---------------------------------------------------------------------
-- 2. 数据源信息表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `data_source` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
  `name`          VARCHAR(100) NOT NULL COMMENT '数据源名称',
  `type`          VARCHAR(20)  NOT NULL COMMENT '数据库类型：MYSQL、POSTGRESQL',
  `host`          VARCHAR(100) NOT NULL COMMENT '数据库主机地址',
  `port`          INT          NOT NULL COMMENT '数据库端口',
  `database_name` VARCHAR(100) NOT NULL COMMENT '数据库名',
  `username`      VARCHAR(100) NOT NULL COMMENT '数据库用户名',
  `password`      VARCHAR(200) NOT NULL COMMENT '数据库密码',
  `status`        INT          NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `description`   VARCHAR(500) NULL COMMENT '数据源描述',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源信息表';

-- ---------------------------------------------------------------------
-- 3. 数据质量规则表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `data_quality_rule` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `datasource_id` BIGINT       NOT NULL COMMENT '数据源ID',
  `table_name`    VARCHAR(100) NOT NULL COMMENT '表名',
  `column_name`   VARCHAR(100) NOT NULL COMMENT '字段名',
  `rule_type`     VARCHAR(30)  NOT NULL COMMENT '规则类型：NOT_NULL、UNIQUE、LENGTH、FORMAT、RANGE、CUSTOM_SQL 等',
  `rule_name`     VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_config`   TEXT         NULL COMMENT '规则配置（JSON格式）',
  `status`        INT          NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_datasource_table` (`datasource_id`, `table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量规则表';

-- ---------------------------------------------------------------------
-- 4. 数据质量检查记录表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `data_quality_check_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_id`       BIGINT       NOT NULL COMMENT '质量规则ID',
  `rule_name`     VARCHAR(100) NULL COMMENT '规则名称',
  `datasource_id` BIGINT       NULL COMMENT '数据源ID',
  `table_name`    VARCHAR(100) NULL COMMENT '检测表名',
  `column_name`   VARCHAR(100) NULL COMMENT '检测字段',
  `total_count`   BIGINT       NULL COMMENT '总数据量',
  `success_count` BIGINT       NULL COMMENT '正常数据数量',
  `error_count`   BIGINT       NULL COMMENT '异常数据数量',
  `status`        VARCHAR(20)  NULL COMMENT '执行状态：SUCCESS成功 FAIL失败',
  `error_message` VARCHAR(1000) NULL COMMENT '执行失败原因',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_datasource_table` (`datasource_id`, `table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量检查记录表';

-- ---------------------------------------------------------------------
-- 5. 数据质量异常数据表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `data_quality_error` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `check_id`      BIGINT       NOT NULL COMMENT '检查记录ID(data_quality_check_record.id)',
  `rule_id`       BIGINT       NULL COMMENT '质量规则ID',
  `table_name`    VARCHAR(100) NULL COMMENT '异常数据表名',
  `column_name`   VARCHAR(100) NULL COMMENT '异常字段',
  `error_type`    VARCHAR(30)  NULL COMMENT '异常类型：NOT_NULL、UNIQUE、REGEX 等',
  `error_message` VARCHAR(1000) NULL COMMENT '异常原因',
  `error_data`    TEXT         NULL COMMENT '异常数据（JSON格式）',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '产生时间',
  PRIMARY KEY (`id`),
  KEY `idx_check_id` (`check_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量异常数据表';

-- ---------------------------------------------------------------------
-- 6. AI 聊天会话索引表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`    VARCHAR(64)  NOT NULL COMMENT 'Redis 会话ID',
  `title`         VARCHAR(100) NOT NULL COMMENT '会话标题（第一条用户消息截断）',
  `datasource_id` BIGINT       NULL COMMENT '关联数据源ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天会话索引';
