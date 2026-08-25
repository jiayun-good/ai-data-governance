package com.mp.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ChatSessionVO {

    private Long id;

    private String sessionId;

    private String title;

    private Long datasourceId;

    private String datasourceName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 会话详情（从 Redis 加载），包含 history + rules
     */
    private List<Map<String, String>> history;

    private List<Map<String, Object>> rules;
}
