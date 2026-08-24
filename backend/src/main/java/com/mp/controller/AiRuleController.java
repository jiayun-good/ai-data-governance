package com.mp.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mp.common.Result;
import com.mp.connector.DataSourceConnector;
import com.mp.connector.DataSourceConnectorFactory;
import com.mp.domain.dto.AiRuleRequest;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.po.DataSource;
import com.mp.domain.vo.AiRulePreviewVO;
import com.mp.mapper.DataQualityRuleMapper;
import com.mp.mapper.DataSourceMapper;
import com.mp.domain.vo.ColumnVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能生成数据质量规则控制器
 *
 * 流程：
 * 1. POST /preview  — 调 AI 生成规则，返回预览（不入库）
 * 2. POST /save     — 用户确认后入库
 */
@RestController
@RequestMapping("/api/rule/ai")
public class AiRuleController {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private DataSourceMapper dataSourceMapper;

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Resource
    private DataQualityRuleMapper dataQualityRuleMapper;

    private static final String AI_BASE_URL = "http://localhost:8000/ai/rule";

    /**
     * 第一步：AI 生成规则预览（不入库）
     *
     * 1. 查数据源 → 获取所有表名
     * 2. 调 AI 选出最匹配的表
     * 3. 只查该表的字段元数据
     * 4. 调 AI 生成规则
     * 5. 返回预览结果给前端展示
     */
    @PostMapping("/preview")
    public Result<AiRulePreviewVO> preview(@RequestBody AiRuleRequest request) {

        // 1. 查询数据源
        DataSource dataSource = dataSourceMapper.selectById(request.getDatasourceId());
        if (dataSource == null) {
            return Result.error("数据源不存在");
        }

        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());

        // 2. 只获取表名列表（不查字段）
        List<String> tables = connector.getTables(dataSource);
        if (tables == null || tables.isEmpty()) {
            return Result.error("该数据源下没有表");
        }

        // 3. 第一步调 AI：让 AI 从表名中选出最匹配的表
        Map<String, Object> analyzeRequest = new HashMap<>();
        analyzeRequest.put("description", request.getDescription());
        analyzeRequest.put("tables", tables);

        String matchedTable;
        try {
            String analyzeResponse = restTemplate.postForObject(
                    AI_BASE_URL + "/analyze-table", analyzeRequest, String.class);
            if (analyzeResponse == null || analyzeResponse.isBlank()) {
                return Result.error("AI未能匹配到合适的表");
            }
            JSONObject analyzeResult = JSON.parseObject(analyzeResponse);
            matchedTable = analyzeResult.getString("table");
            if (matchedTable == null || matchedTable.isBlank()) {
                return Result.error("AI未能匹配到合适的表");
            }
        } catch (Exception e) {
            return Result.error("调用AI分析表失败：" + e.getMessage());
        }

        // 4. 只查匹配表的字段
        List<ColumnVO> columns = connector.getColumns(dataSource, matchedTable);

        // 5. 第二步调 AI：发送字段元数据，生成规则
        Map<String, Object> generateRequest = new HashMap<>();
        generateRequest.put("description", request.getDescription());
        generateRequest.put("table", matchedTable);
        generateRequest.put("columns", columns);

        String ruleResponse;
        try {
            ruleResponse = restTemplate.postForObject(
                    AI_BASE_URL + "/generate", generateRequest, String.class);
        } catch (Exception e) {
            return Result.error("调用AI生成规则失败：" + e.getMessage());
        }

        if (ruleResponse == null || ruleResponse.isBlank()) {
            return Result.error("AI服务未返回结果");
        }

        // 6. 解析 AI 返回，组装预览结果（不入库）
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

            return Result.success(preview);
        } catch (Exception e) {
            return Result.error("解析AI返回结果失败：" + e.getMessage());
        }
    }

    /**
     * 第二步：用户确认后保存规则入库
     *
     * 前端把预览结果（可修改）发回来，确认入库
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody AiRulePreviewVO preview) {

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

        //校验是否真正存在
        DataSource dataSource =
                dataSourceMapper.selectById(
                        preview.getDatasourceId()
                );


        DataSourceConnector connector =
          connectorFactory.getConnector(
                    dataSource.getType()
        );
        //验证表：
        List<String> tables =
                connector.getTables(dataSource);


        if(!tables.contains(preview.getTableName())){
            return Result.error("数据表不存在");
        }
        //验证字段：
        List<ColumnVO> columns =
                connector.getColumns(
                        dataSource,
                        preview.getTableName()
                );

        boolean exists =
                columns.stream()
                        .anyMatch(
                                c -> c.getColumnName()
                                        .equals(preview.getColumnName())
                        );

        if(!exists){
            return Result.error("字段不存在");
        }

        DataQualityRule rule = new DataQualityRule();
        rule.setDatasourceId(preview.getDatasourceId());
        rule.setTableName(preview.getTableName());
        rule.setColumnName(preview.getColumnName());
        rule.setRuleType(preview.getRuleType());
        rule.setRuleName(preview.getRuleName());
        rule.setRuleConfig(preview.getRuleConfig());
        rule.setStatus(1); // 默认启用

        dataQualityRuleMapper.insert(rule);

        return Result.success();
    }
}
