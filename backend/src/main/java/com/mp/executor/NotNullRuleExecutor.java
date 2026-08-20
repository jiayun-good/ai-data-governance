package com.mp.executor;

import com.alibaba.fastjson2.JSON;
import com.mp.config.RuleConfig;
import com.mp.connector.DataSourceConnector;
import com.mp.connector.DataSourceConnectorFactory;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.QualityCheckResultVO;
import jakarta.annotation.Resource;

import com.mp.domain.po.DataSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.trim;


@Component
public class NotNullRuleExecutor implements RuleExecutor{
    @Resource
    private DataSourceConnectorFactory connectorFactory;

    @Override
    public String getRuleType() {
        return "NOT_NULL";
    }

    @Override
    public QualityCheckResultVO execute(DataQualityRule rule, DataSource dataSource) {


        //1. 获取数据库连接器
        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());

        RuleConfig ruleConfig =
                JSON.parseObject(
                        rule.getRuleConfig(),
                        RuleConfig.class
                );
        if (ruleConfig == null) {
            throw new IllegalArgumentException(
                    "质量规则配置解析失败，ruleId=" + rule.getId()
            );
        }

        List<String> conditions = new ArrayList<>();
        if(Boolean.TRUE.equals(ruleConfig.getCheckNull())){
            conditions.add(
                    rule.getColumnName() + " is null"
            );
        }


        if(Boolean.TRUE.equals(ruleConfig.getCheckEmpty())){
            if(Boolean.TRUE.equals(ruleConfig.getTrim())){
                conditions.add(
                        "trim("
                                + rule.getColumnName()
                                + ")=''");
            }else{
                conditions.add(rule.getColumnName() + "=''");
            }
        }
        //为空处理
        if(conditions.isEmpty()){
            throw new RuntimeException("规则配置错误，没有检测条件");
        }

        // 生成异常条件
        String condition =
                String.join(
                        " or ",
                        conditions
                );

        //2. 查询总数据量
        String totalSql =
                "select count(*) from "
                        + rule.getTableName();

        Long total =
                connector.count(
                        dataSource,
                        totalSql
                );

        //3. 查询异常数据量
        String errorSql =
                "select count(*) from "
                        + rule.getTableName()
                        +" where "
                        +condition;
        //4.异常数量
        Long error =
                connector.count(
                        dataSource,
                        errorSql
                );

        //4. 查询异常数据
        String detailSql =
                "select * from "
                        + rule.getTableName()
                        +" where "
                        +condition;

        //5. 查询异常数据
        List<Map<String,Object>> errorData =
                connector.query(
                        dataSource,
                        detailSql
                );
        //6. 计算正常数量
        Long success = total - error;

        QualityCheckResultVO resultVO = new QualityCheckResultVO();
        resultVO.setErrorCount(error);
        resultVO.setSuccessCount(success);
        resultVO.setTotal(total);
        resultVO.setErrorData(errorData);

        return resultVO;
    }


}
