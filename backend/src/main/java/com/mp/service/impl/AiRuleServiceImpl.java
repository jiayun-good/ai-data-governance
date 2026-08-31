package com.mp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mp.common.Result;
import com.mp.connector.DataSourceConnector;
import com.mp.connector.DataSourceConnectorFactory;
import com.mp.domain.dto.AiRuleRequest;
import com.mp.domain.po.ChatSession;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.po.DataSource;
import com.mp.domain.vo.AiRulePreviewVO;
import com.mp.domain.vo.ChatSessionVO;
import com.mp.domain.vo.ColumnVO;
import com.mp.mapper.ChatSessionMapper;
import com.mp.mapper.DataQualityRuleMapper;
import com.mp.mapper.DataSourceMapper;
import com.mp.service.IAiRuleService;
import com.mp.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 智能生成数据质量规则 服务实现类
 */
@Service
public class AiRuleServiceImpl implements IAiRuleService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private DataSourceMapper dataSourceMapper;

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Resource
    private DataQualityRuleMapper dataQualityRuleMapper;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private RedisUtil redisUtil;

    // AI 服务地址（Docker 部署时由环境变量 AI_SERVICE_URL 覆盖）
    @Value("${ai.service.base-url:http://localhost:8011}")
    private String aiServiceBaseUrl;
    private static final long SESSION_TTL = -1L; // -1 表示永久保存（Redis 不设置过期时间）

    @Override
    public Result<AiRulePreviewVO> preview(AiRuleRequest request) {

        // 1. 查询数据源
        DataSource dataSource = dataSourceMapper.selectById(request.getDatasourceId());
        if (dataSource == null) {
            return Result.error("数据源不存在");
        }

        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());

        // 2. 加载已有会话（如果有）
        JSONObject session = null;
        String sessionId = request.getSessionId();
        if (sessionId != null && !sessionId.isBlank()) {
            session = loadSession(sessionId);
        }

        // 3. 只获取表名列表（不查字段）
        List<String> tables = connector.getTables(dataSource);
        if (tables == null || tables.isEmpty()) {
            return Result.error("该数据源下没有表");
        }

        // 4. 调 AI 选表（有历史则传入上下文 + currentTable）
        String matchedTable = callAnalyzeTable(request, tables, session);
        if (matchedTable == null || matchedTable.isBlank()) {
            return Result.error("AI未能匹配到合适的表");
        }

        // 5. 只查匹配表的字段
        List<ColumnVO> columns = connector.getColumns(dataSource, matchedTable);

        // 6. 调 AI 生成规则（携带对话历史）
        String ruleResponse = callGenerateRule(request, matchedTable, columns, session);
        if (ruleResponse == null || ruleResponse.isBlank()) {
            return Result.error("AI服务未返回结果");
        }

        // 7. 解析 AI 返回，组装预览结果，管理会话
        try {
            JSONObject ruleJson = JSON.parseObject(ruleResponse);

            AiRulePreviewVO preview = new AiRulePreviewVO();
            preview.setDatasourceId(request.getDatasourceId());
            preview.setTableName(matchedTable);
            preview.setColumnName(ruleJson.getString("column"));
            preview.setRuleType(ruleJson.getString("ruleType"));
            preview.setRuleName(ruleJson.getString("ruleName"));
            preview.setRuleConfig(JSON.toJSONString(ruleJson.get("ruleConfig")));
            preview.setDescription(ruleJson.getString("description"));

            // 构建 AI 回复摘要和规则记录
            String assistantReply = "已识别表 " + matchedTable
                    + "，为字段 " + ruleJson.getString("column")
                    + " 生成 " + ruleJson.getString("ruleType") + " 规则："
                    + ruleJson.getString("ruleName");

            Map<String, Object> ruleRecord = buildRuleRecord(matchedTable, ruleJson);

            // 存入 Redis
            String chatSessionId;
            if (session == null) {
                chatSessionId = createChatSession(
                        request.getDatasourceId(),
                        request.getDescription(), assistantReply, ruleRecord);
            } else {
                chatSessionId = sessionId;
                updateChatSession(session, chatSessionId,
                        request.getDescription(), assistantReply, ruleRecord);
            }

            preview.setSessionId(chatSessionId);
            return Result.success(preview);
        } catch (Exception e) {
            return Result.error("解析AI返回结果失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Void> save(AiRulePreviewVO preview) {

        // 参数校验
        Result<Void> validation = validatePreview(preview);
        if (validation != null) {
            return validation;
        }

        // 校验数据源、表、字段是否真实存在
        DataSource dataSource = dataSourceMapper.selectById(preview.getDatasourceId());
        if (dataSource == null) {
            return Result.error("数据源不存在");
        }

        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());
        List<String> tables = connector.getTables(dataSource);
        if (!tables.contains(preview.getTableName())) {
            return Result.error("数据表不存在");
        }

        List<ColumnVO> columns = connector.getColumns(dataSource, preview.getTableName());
        boolean exists = columns.stream()
                .anyMatch(c -> c.getColumnName().equals(preview.getColumnName()));
        if (!exists) {
            return Result.error("字段不存在");
        }

        // 入库
        DataQualityRule rule = new DataQualityRule();
        rule.setDatasourceId(preview.getDatasourceId());
        rule.setTableName(preview.getTableName());
        rule.setColumnName(preview.getColumnName());
        rule.setRuleType(preview.getRuleType());
        rule.setRuleName(preview.getRuleName());
        rule.setRuleConfig(preview.getRuleConfig());
        rule.setStatus(1);

        dataQualityRuleMapper.insert(rule);
        return Result.success();
    }

    // ========================= AI 调用 =========================

    /**
     * 调用 AI /analyze-table，有会话上下文时传入 history + currentTable
     */
    private String callAnalyzeTable(AiRuleRequest request, List<String> tables,
                                     JSONObject session) {
        Map<String, Object> analyzeRequest = new HashMap<>();
        analyzeRequest.put("description", request.getDescription());
        analyzeRequest.put("tables", tables);

        if (session != null) {
            analyzeRequest.put("history", session.getJSONArray("history"));
            JSONArray rules = session.getJSONArray("rules");
            if (rules != null && !rules.isEmpty()) {
                JSONObject lastRule = rules.getJSONObject(rules.size() - 1);
                analyzeRequest.put("currentTable", lastRule.getString("tableName"));
            }
        }

        try {
            String response = restTemplate.postForObject(
                    aiServiceBaseUrl + "/ai/rule/analyze-table", analyzeRequest, String.class);
            if (response == null || response.isBlank()) {
                return null;
            }
            return JSON.parseObject(response).getString("table");
        } catch (Exception e) {
            throw new RuntimeException("调用AI分析表失败：" + e.getMessage(), e);
        }
    }

    /**
     * 调用 AI /generate，携带对话历史
     */
    private String callGenerateRule(AiRuleRequest request, String matchedTable,
                                     List<ColumnVO> columns, JSONObject session) {
        Map<String, Object> generateRequest = new HashMap<>();
        generateRequest.put("description", request.getDescription());
        generateRequest.put("table", matchedTable);
        generateRequest.put("columns", columns);

        if (session != null) {
            generateRequest.put("history", session.getJSONArray("history"));
        }

        try {
            return restTemplate.postForObject(
                    aiServiceBaseUrl + "/ai/rule/generate", generateRequest, String.class);
        } catch (Exception e) {
            throw new RuntimeException("调用AI生成规则失败：" + e.getMessage(), e);
        }
    }

    // ========================= 会话管理 =========================

    /**
     * 从 Redis 加载会话
     */
    private JSONObject loadSession(String sessionId) {
        String key = "ai:chat:session:" + sessionId;
        Object value = redisUtil.get(key);
        if (value == null) return null;
        try {
            return JSON.parseObject(JSON.toJSONString(value));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建新的对话会话，存储 {context, history, rules}，同时写入 DB 索引
     */
    private String createChatSession(Long datasourceId, String userMessage,
                                     String assistantReply, Map<String, Object> ruleRecord) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String key = "ai:chat:session:" + sessionId;

        Map<String, Object> session = buildSession(datasourceId, userMessage, assistantReply, ruleRecord);
        redisUtil.set(key, session, SESSION_TTL);

        // 写入 DB 索引
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setTitle(truncateTitle(userMessage));
        chatSession.setDatasourceId(datasourceId);
        chatSession.setCreateTime(LocalDateTime.now());
        chatSession.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.insert(chatSession);

        return sessionId;
    }

    /**
     * 追加对话记录和规则到已有会话，同时更新 DB 时间
     */
    private void updateChatSession(JSONObject session, String sessionId,
                                   String userMessage, String assistantReply,
                                   Map<String, Object> ruleRecord) {
        String key = "ai:chat:session:" + sessionId;

        // 追加 history
        List<Object> history = new ArrayList<>(session.getJSONArray("history"));
        Map<String, String> message = new LinkedHashMap<>();
        message.put("user", userMessage);
        message.put("assistant", assistantReply);
        history.add(message);
        session.put("history", history);

        // 追加 rules
        List<Object> rules = new ArrayList<>(session.getJSONArray("rules"));
        rules.add(ruleRecord);
        session.put("rules", rules);

        redisUtil.set(key, session, SESSION_TTL);

        // 更新 DB 时间戳
        ChatSession update = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId));
        if (update != null) {
            update.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(update);
        }
    }

    // ========================= 会话列表/详情/删除 =========================

    @Override
    public Result<List<ChatSessionVO>> listSessions() {
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .orderByDesc(ChatSession::getUpdateTime));

        // 批量查数据源名称
        Set<Long> dsIds = sessions.stream()
                .map(ChatSession::getDatasourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> dsNameMap = new HashMap<>();
        if (!dsIds.isEmpty()) {
            List<DataSource> dsList = dataSourceMapper.selectBatchIds(dsIds);
            dsList.forEach(ds -> dsNameMap.put(ds.getId(), ds.getName()));
        }

        List<ChatSessionVO> voList = sessions.stream().map(s -> {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(s.getId());
            vo.setSessionId(s.getSessionId());
            vo.setTitle(s.getTitle());
            vo.setDatasourceId(s.getDatasourceId());
            vo.setDatasourceName(dsNameMap.getOrDefault(s.getDatasourceId(), "未知数据源"));
            vo.setCreateTime(s.getCreateTime());
            vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    @Override
    public Result<ChatSessionVO> getSession(String sessionId) {
        // 从 DB 查索引
        ChatSession chatSession = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId));
        if (chatSession == null) {
            return Result.error("会话不存在");
        }

        // 从 Redis 加载完整对话
        JSONObject session = loadSession(sessionId);
        if (session == null) {
            return Result.error("会话数据已过期，请重新开始对话");
        }

        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(chatSession.getId());
        vo.setSessionId(chatSession.getSessionId());
        vo.setTitle(chatSession.getTitle());
        vo.setDatasourceId(chatSession.getDatasourceId());
        vo.setCreateTime(chatSession.getCreateTime());
        vo.setUpdateTime(chatSession.getUpdateTime());

        // 解析 history
        JSONArray historyArr = session.getJSONArray("history");
        if (historyArr != null) {
            List<Map<String, String>> history = new ArrayList<>();
            for (int i = 0; i < historyArr.size(); i++) {
                JSONObject item = historyArr.getJSONObject(i);
                Map<String, String> msg = new LinkedHashMap<>();
                msg.put("user", item.getString("user"));
                msg.put("assistant", item.getString("assistant"));
                history.add(msg);
            }
            vo.setHistory(history);
        }

        // 解析 rules
        JSONArray rulesArr = session.getJSONArray("rules");
        if (rulesArr != null) {
            List<Map<String, Object>> rules = new ArrayList<>();
            for (int i = 0; i < rulesArr.size(); i++) {
                JSONObject item = rulesArr.getJSONObject(i);
                rules.add(new LinkedHashMap<>(item));
            }
            vo.setRules(rules);
        }

        // 查数据源名称
        DataSource ds = dataSourceMapper.selectById(chatSession.getDatasourceId());
        if (ds != null) {
            vo.setDatasourceName(ds.getName());
        }

        return Result.success(vo);
    }

    @Override
    public Result<Void> deleteSession(String sessionId) {
        // 删 DB
        chatSessionMapper.delete(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId));
        // 删 Redis
        redisUtil.delete("ai:chat:session:" + sessionId);
        return Result.success();
    }

    // ========================= 辅助方法 =========================

    private String truncateTitle(String text) {
        if (text == null) return "新对话";
        return text.length() > 20 ? text.substring(0, 20) + "..." : text;
    }

    /**
     * 构建完整的会话对象 {context, history, rules}
     */
    private Map<String, Object> buildSession(Long datasourceId, String userMessage,
                                              String assistantReply, Map<String, Object> ruleRecord) {
        Map<String, Object> session = new LinkedHashMap<>();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("datasourceId", datasourceId);
        session.put("context", context);

        List<Map<String, String>> history = new ArrayList<>();
        Map<String, String> message = new LinkedHashMap<>();
        message.put("user", userMessage);
        message.put("assistant", assistantReply);
        history.add(message);
        session.put("history", history);

        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(ruleRecord);
        session.put("rules", rules);

        return session;
    }

    /**
     * 构建规则记录（存入 Redis rules 数组的一条）
     */
    private Map<String, Object> buildRuleRecord(String matchedTable, JSONObject ruleJson) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("tableName", matchedTable);
        record.put("columnName", ruleJson.getString("column"));
        record.put("ruleType", ruleJson.getString("ruleType"));
        record.put("ruleName", ruleJson.getString("ruleName"));
        record.put("ruleConfig", ruleJson.get("ruleConfig"));
        return record;
    }

    /**
     * 校验预览参数
     */
    private Result<Void> validatePreview(AiRulePreviewVO preview) {
        if (preview.getDatasourceId() == null) {
            return Result.error("数据源ID不能为空");
        }
        if (preview.getTableName() == null || preview.getTableName().isBlank()) {
            return Result.error("表名不能为空");
        }
        if (preview.getColumnName() == null || preview.getColumnName().isBlank()) {
            return Result.error("字段名不能为空");
        }
        if (preview.getRuleType() == null || preview.getRuleType().isBlank()) {
            return Result.error("规则类型不能为空");
        }
        if (preview.getRuleName() == null || preview.getRuleName().isBlank()) {
            return Result.error("规则名不能为空");
        }
        if (preview.getRuleConfig() == null || preview.getRuleConfig().isBlank()) {
            return Result.error("规则配置不能为空");
        }
        if (preview.getDescription() == null || preview.getDescription().isBlank()) {
            return Result.error("规则描述不能为空");
        }
        return null;
    }
}
