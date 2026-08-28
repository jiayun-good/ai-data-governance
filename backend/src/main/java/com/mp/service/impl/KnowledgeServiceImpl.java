package com.mp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mp.common.Result;
import com.mp.domain.dto.KnowledgeAddDTO;
import com.mp.domain.dto.KnowledgeUpdateDTO;
import com.mp.domain.vo.KnowledgeDocVO;
import com.mp.domain.vo.KnowledgeSearchResultVO;
import com.mp.service.IKnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 知识库管理 服务实现类
 *
 * 核心逻辑：通过 RestTemplate 透传调用 Python FastAPI 的 /ai/knowledge 接口，
 * 用 FastJSON2 解析 Python 返回的 snake_case JSON 并转换为 Java VO。
 */
@Service
public class KnowledgeServiceImpl implements IKnowledgeService {

    @Resource
    private RestTemplate restTemplate;

    private static final String AI_KNOWLEDGE_URL = "http://localhost:8011/ai/knowledge";

    @Override
    public Result<List<KnowledgeDocVO>> listDocuments() {
        try {
            String response = restTemplate.getForObject(AI_KNOWLEDGE_URL, String.class);
            JSONArray arr = JSON.parseArray(response);
            List<KnowledgeDocVO> docs = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                KnowledgeDocVO doc = new KnowledgeDocVO();
                doc.setDocId(obj.getString("doc_id"));
                doc.setTitle(obj.getString("title"));
                doc.setSource(obj.getString("source"));
                doc.setChunkCount(obj.getInteger("chunk_count"));
                docs.add(doc);
            }
            return Result.success(docs);
        } catch (Exception e) {
            return Result.error("获取知识文档列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> getDocument(String docId) {
        try {
            String response = restTemplate.getForObject(
                    AI_KNOWLEDGE_URL + "/" + docId, String.class);
            Map<String, Object> data = JSON.parseObject(response, Map.class);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取知识文档详情失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> addKnowledge(KnowledgeAddDTO dto) {
        try {
            JSONObject request = new JSONObject();
            request.put("title", dto.getTitle());
            request.put("content", dto.getContent());
            if (dto.getSource() != null) {
                request.put("source", dto.getSource());
            }
            String response = restTemplate.postForObject(
                    AI_KNOWLEDGE_URL, request, String.class);
            Map<String, Object> data = JSON.parseObject(response, Map.class);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("添加知识文档失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> updateKnowledge(String docId, KnowledgeUpdateDTO dto) {
        try {
            JSONObject request = new JSONObject();
            request.put("title", dto.getTitle());
            request.put("content", dto.getContent());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(request.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    AI_KNOWLEDGE_URL + "/" + docId,
                    HttpMethod.PUT,
                    entity,
                    String.class);
            Map<String, Object> data = JSON.parseObject(response.getBody(), Map.class);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("更新知识文档失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteKnowledge(String docId) {
        try {
            restTemplate.delete(AI_KNOWLEDGE_URL + "/" + docId);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除知识文档失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<KnowledgeSearchResultVO>> searchKnowledge(String query, Integer k) {
        try {
            String url = AI_KNOWLEDGE_URL + "/search?query=" + query + "&k=" + (k != null ? k : 3);
            String response = restTemplate.getForObject(url, String.class);
            JSONArray arr = JSON.parseArray(response);
            List<KnowledgeSearchResultVO> results = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                KnowledgeSearchResultVO vo = new KnowledgeSearchResultVO();
                vo.setContent(obj.getString("content"));
                vo.setSource(obj.getString("source"));
                vo.setTitle(obj.getString("title"));
                vo.setChunkIndex(obj.getInteger("chunk_index"));
                vo.setScore(obj.getDouble("score"));
                results.add(vo);
            }
            return Result.success(results);
        } catch (Exception e) {
            return Result.error("搜索知识失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> loadDirectory() {
        try {
            String response = restTemplate.postForObject(
                    AI_KNOWLEDGE_URL + "/load-dir", null, String.class);
            Map<String, Object> data = JSON.parseObject(response, Map.class);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("批量加载知识目录失败：" + e.getMessage());
        }
    }
}
