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
public class LengthRuleExecutor implements RuleExecutor {

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Override
    public String getRuleType() {
        return "LENGTH";
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

        if (ruleConfig.getMinLength() == null && ruleConfig.getMaxLength() == null) {
            throw new RuntimeException("规则配置错误，LENGTH规则至少需要配置minLength或maxLength");
        }

        // 1. 总数据量
        Long total = connector.count(dataSource,
                "select count(*) from " + table);

        // 2. 构建异常条件：字符长度不在 [minLength, maxLength] 范围内
        List<String> conditions = new ArrayList<>();
        if (ruleConfig.getMinLength() != null) {
            conditions.add("CHAR_LENGTH(" + column + ") < " + ruleConfig.getMinLength());
        }
        if (ruleConfig.getMaxLength() != null) {
            conditions.add("CHAR_LENGTH(" + column + ") > " + ruleConfig.getMaxLength());
        }

        String condition = String.join(" or ", conditions);

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
