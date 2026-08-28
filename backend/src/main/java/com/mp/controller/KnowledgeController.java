package com.mp.controller;

import com.mp.common.Result;
import com.mp.domain.dto.KnowledgeAddDTO;
import com.mp.domain.dto.KnowledgeUpdateDTO;
import com.mp.domain.vo.KnowledgeDocVO;
import com.mp.domain.vo.KnowledgeSearchResultVO;
import com.mp.service.IKnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 知识库管理控制器
 *
 * 路由前缀 /knowledge，经 JWT 拦截器保护。
 * 内部透传调用 Python FastAPI 的 /ai/knowledge 接口。
 *
 * 端点概览：
 *   GET    /knowledge              列出所有知识文档
 *   GET    /knowledge/{docId}      获取单个文档内容（用于编辑回填）
 *   POST   /knowledge              添加知识文档
 *   PUT    /knowledge/{docId}      更新知识文档
 *   DELETE /knowledge/{docId}      删除知识文档
 *   GET    /knowledge/search       搜索知识
 *   POST   /knowledge/load-dir     批量加载 rag/knowledge/ 目录
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Resource
    private IKnowledgeService knowledgeService;

    @GetMapping
    public Result<List<KnowledgeDocVO>> list() {
        return knowledgeService.listDocuments();
    }

    @GetMapping("/{docId}")
    public Result<Map<String, Object>> getDocument(@PathVariable String docId) {
        return knowledgeService.getDocument(docId);
    }

    @PostMapping
    public Result<Map<String, Object>> add(@RequestBody KnowledgeAddDTO dto) {
        return knowledgeService.addKnowledge(dto);
    }

    @PutMapping("/{docId}")
    public Result<Map<String, Object>> update(@PathVariable String docId,
                                              @RequestBody KnowledgeUpdateDTO dto) {
        return knowledgeService.updateKnowledge(docId, dto);
    }

    @DeleteMapping("/{docId}")
    public Result<Void> delete(@PathVariable String docId) {
        return knowledgeService.deleteKnowledge(docId);
    }

    @GetMapping("/search")
    public Result<List<KnowledgeSearchResultVO>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") Integer k) {
        return knowledgeService.searchKnowledge(query, k);
    }

    @PostMapping("/load-dir")
    public Result<Map<String, Object>> loadDir() {
        return knowledgeService.loadDirectory();
    }
}
