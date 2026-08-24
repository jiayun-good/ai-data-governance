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
import java.util.stream.Collectors;

@Component
public class EnumRuleExecutor implements RuleExecutor {

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Override
    public String getRuleType() {
        return "ENUM";
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

        if (ruleConfig.getValues() == null || ruleConfig.getValues().isEmpty()) {
            throw new RuntimeException("规则配置错误，ENUM规则必须配置values枚举值列表");
        }

        // 1. 总数据量
        Long total = connector.count(dataSource,
                "select count(*) from " + table);

        // 2. 构建 NOT IN 条件：值不在枚举列表中的为异常
        String inValues = ruleConfig.getValues().stream()
                .map(v -> "'" + v.replace("'", "\\'") + "'")
                .collect(Collectors.joining(", "));

        String condition = column + " NOT IN (" + inValues + ")";

        // 3. 异常数据量
        Long error = connector.count(dataSource,
                "select count(*) from " + table + " where " + condition);

        // 4. 异常数据明细
        List<Map<String, Object>> errorData = connector.query(dataSource,
                "select * from " + table + " where " + condition);

        Long success = total - error;

        QualityCheckResultVO resultVO = new QualityCheckResultVO();
        resultVO.setTotal(total);
        resultVO.setErrorCount(error);
        resultVO.setSuccessCount(success);
        resultVO.setErrorData(errorData);

        return resultVO;
    }
}
