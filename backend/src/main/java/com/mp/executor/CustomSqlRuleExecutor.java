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

import java.util.List;
import java.util.Map;

@Component
public class CustomSqlRuleExecutor implements RuleExecutor {

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Override
    public String getRuleType() {
        return "CUSTOM_SQL";
    }

    @Override
    public QualityCheckResultVO execute(DataQualityRule rule, DataSource dataSource) {

        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());

        RuleConfig ruleConfig = JSON.parseObject(rule.getRuleConfig(), RuleConfig.class);
        if (ruleConfig == null) {
            throw new IllegalArgumentException("质量规则配置解析失败，ruleId=" + rule.getId());
        }

        if (ruleConfig.getCustomSql() == null || ruleConfig.getCustomSql().isEmpty()) {
            throw new RuntimeException("规则配置错误，CUSTOM_SQL规则必须配置customSql");
        }

        String customSql = ruleConfig.getCustomSql();
        String table = rule.getTableName();

        // 1. 查询总数据量
        String totalSql = "select count(*) from " + table;
        Long total = connector.count(dataSource, totalSql);

        // 2. 用自定义SQL查询异常数据量（用户提供的SQL应返回异常数据集合）
        Long error = connector.count(dataSource,
                "select count(*) from (" + customSql + ") t");

        // 3. 查询异常数据明细
        List<Map<String, Object>> errorData = connector.query(dataSource, customSql);

        Long success = total - error;

        QualityCheckResultVO resultVO = new QualityCheckResultVO();
        resultVO.setErrorCount(error);
        resultVO.setSuccessCount(success);
        resultVO.setTotal(total);
        resultVO.setErrorData(errorData);

        return resultVO;
    }
}
