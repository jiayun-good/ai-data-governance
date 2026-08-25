package com.mp.service;

import com.mp.common.Result;
import com.mp.domain.dto.AiRuleRequest;
import com.mp.domain.vo.AiRulePreviewVO;

/**
 * AI 智能生成数据质量规则 服务接口
 */
public interface IAiRuleService {

    /**
     * AI 生成规则预览（不入库）
     *
     * 1. 查数据源 → 获取所有表名
     * 2. 调 AI 选出最匹配的表（有会话上下文时走 detect_context_switch）
     * 3. 查该表的字段元数据
     * 4. 调 AI 生成规则
     * 5. 返回预览结果 + sessionId
     */
    Result<AiRulePreviewVO> preview(AiRuleRequest request);

    /**
     * 用户确认后保存规则入库
     */
    Result<Void> save(AiRulePreviewVO preview);
}
