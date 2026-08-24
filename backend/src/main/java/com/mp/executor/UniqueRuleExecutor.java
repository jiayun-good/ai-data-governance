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
public class UniqueRuleExecutor implements RuleExecutor {

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Override
    public String getRuleType() {
        return "UNIQUE";
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
        boolean ignoreNull = Boolean.TRUE.equals(ruleConfig.getIgnoreNull());

        // 1. 查询总数据量
        String totalSql = "select count(*) from " + table;
        Long total = connector.count(dataSource, totalSql);

        // 2. 查询重复数据（子查询找出出现次数>1的值）
        String errorSql;
        if (ignoreNull) {
            errorSql = "select * from " + table
                    + " where " + column + " in ("
                    + "select " + column + " from " + table
                    + " where " + column + " is not null"
                    + " group by " + column + " having count(*) > 1)";
        } else {
            errorSql = "select * from " + table
                    + " where " + column + " in ("
                    + "select " + column + " from " + table
                    + " group by " + column + " having count(*) > 1)";
        }

        Long error = connector.count(dataSource,
                "select count(*) from (" + errorSql + ") t");

        List<Map<String, Object>> errorData = connector.query(dataSource, errorSql);

        Long success = total - error;

        QualityCheckResultVO resultVO = new QualityCheckResultVO();
        resultVO.setErrorCount(error);
        resultVO.setSuccessCount(success);
        resultVO.setTotal(total);
        resultVO.setErrorData(errorData);

        return resultVO;
    }
}
