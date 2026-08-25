package com.mp.controller;

import com.mp.common.Result;
import com.mp.domain.dto.AiRuleRequest;
import com.mp.domain.vo.AiRulePreviewVO;
import com.mp.domain.vo.ChatSessionVO;
import com.mp.service.IAiRuleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 智能生成数据质量规则控制器
 *
 * 流程：
 * 1. POST /preview  — 调 AI 生成规则，返回预览（不入库）
 * 2. POST /save     — 用户确认后入库
 * 3. GET  /sessions — 获取会话列表
 * 4. GET  /session/{sessionId} — 获取会话详情
 * 5. DELETE /session/{sessionId} — 删除会话
 */
@RestController
@RequestMapping("/rule/ai")
public class AiRuleController {

    @Resource
    private IAiRuleService aiRuleService;

    @PostMapping("/preview")
    public Result<AiRulePreviewVO> preview(@RequestBody AiRuleRequest request) {
        return aiRuleService.preview(request);
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody AiRulePreviewVO preview) {
        return aiRuleService.save(preview);
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> listSessions() {
        return aiRuleService.listSessions();
    }

    @GetMapping("/session/{sessionId}")
    public Result<ChatSessionVO> getSession(@PathVariable String sessionId) {
        return aiRuleService.getSession(sessionId);
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        return aiRuleService.deleteSession(sessionId);
    }
}
