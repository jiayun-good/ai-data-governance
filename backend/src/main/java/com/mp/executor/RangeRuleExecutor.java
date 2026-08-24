package com.mp.executor;

import com.alibaba.fastjson2.JSON;
import com.mp.config.RuleConfig;
import com.mp.connector.DataSourceConnector;
import com.mp.connector.DataSourceConnectorFactory;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.po.DataSource;
import com.mp.domain.vo.QualityCheckResultVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RangeRuleExecutor implements RuleExecutor {

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Override
    public String getRuleType() {
        return "RANGE";
    }

    @Override
    public QualityCheckResultVO execute(DataQualityRule rule, DataSource dataSource) {

        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());

        RuleConfig ruleConfig = JSON.parseObject(rule.getRuleConfig(), RuleConfig.class);
        if (ruleConfig == null) {
            throw new IllegalArgumentException("质量规则配置解析失败，ruleId=" + rule.getId());
        }

        String column = rule.getColumnName();
        String table = rule.getTableName();

        if (ruleConfig.getMin() == null && ruleConfig.getMax() == null) {
            throw new RuntimeException("规则配置错误，RANGE规则至少需要配置min或max");
        }

        // 1. 查询总数据量
        String totalSql = "select count(*) from " + table;
        Long total = connector.count(dataSource, totalSql);

        // 2. 构建异常条件：值不在 [min, max] 范围内
        List<String> conditions = new ArrayList<>();
        if (ruleConfig.getMin() != null) {
            conditions.add(column + " < " + ruleConfig.getMin());
        }
        if (ruleConfig.getMax() != null) {
            conditions.add(column + " > " + ruleConfig.getMax());
        }

        String condition = String.join(" or ", conditions);

        // 3. 查询异常数据量
        String errorSql = "select count(*) from " + table + " where " + condition;
        Long error = connector.count(dataSource, errorSql);

        // 4. 查询异常数据明细
        String detailSql = "select * from " + table + " where " + condition;
        List<Map<String, Object>> errorData = connector.query(dataSource, detailSql);

        Long success = total - error;

        QualityCheckResultVO resultVO = new QualityCheckResultVO();
        resultVO.setErrorCount(error);
        resultVO.setSuccessCount(success);
        resultVO.setTotal(total);
        resultVO.setErrorData(errorData);

        return resultVO;
    }
}
