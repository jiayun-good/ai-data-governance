CREATE TABLE IF NOT EXISTS `chat_session` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id`    VARCHAR(64)  NOT NULL COMMENT 'Redis 会话ID',
  `title`         VARCHAR(100) NOT NULL COMMENT '会话标题（第一条用户消息截断）',
  `datasource_id` BIGINT       NULL     COMMENT '关联数据源ID',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 聊天会话索引';
