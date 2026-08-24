package com.mp.executor;

import com.mp.connector.DataSourceConnector;
import com.mp.connector.DataSourceConnectorFactory;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.QualityCheckResultVO;
import com.mp.domain.po.DataSource;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotNullRuleExecutor implements RuleExecutor {

    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Override
    public String getRuleType() {
        return "NOT_NULL";
    }

    @Override
    public QualityCheckResultVO execute(DataQualityRule rule, DataSource dataSource) {

        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());

        String column = rule.getColumnName();
        String table = rule.getTableName();

        // 异常条件：NULL 或 空字符串
        String condition = column + " IS NULL OR " + column + " = ''";

        // 1. 总数据量
        Long total = connector.count(dataSource,
                "select count(*) from " + table);

        // 2. 异常数据量
        Long error = connector.count(dataSource,
                "select count(*) from " + table + " where " + condition);

        // 3. 异常数据明细
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
