package com.mp.service;

import com.mp.common.Result;
import com.mp.domain.dto.KnowledgeAddDTO;
import com.mp.domain.dto.KnowledgeUpdateDTO;
import com.mp.domain.vo.KnowledgeDocVO;
import com.mp.domain.vo.KnowledgeSearchResultVO;

import java.util.List;
import java.util.Map;

/**
 * AI 知识库管理 服务接口
 */
public interface IKnowledgeService {

    /**
     * 列出所有知识文档
     */
    Result<List<KnowledgeDocVO>> listDocuments();

    /**
     * 获取单个知识文档内容（用于编辑回填）
     */
    Result<Map<String, Object>> getDocument(String docId);

    /**
     * 添加知识文档
     */
    Result<Map<String, Object>> addKnowledge(KnowledgeAddDTO dto);

    /**
     * 更新知识文档
     */
    Result<Map<String, Object>> updateKnowledge(String docId, KnowledgeUpdateDTO dto);

    /**
     * 删除知识文档
     */
    Result<Void> deleteKnowledge(String docId);

    /**
     * 搜索知识
     */
    Result<List<KnowledgeSearchResultVO>> searchKnowledge(String query, Integer k);

    /**
     * 批量加载 rag/knowledge/ 目录
     */
    Result<Map<String, Object>> loadDirectory();
}
