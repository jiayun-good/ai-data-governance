package com.mp.service.impl;

import com.mp.common.Result;
import com.mp.connector.DataSourceConnector;
import com.mp.connector.DataSourceConnectorFactory;
import com.mp.domain.dto.QualityRuleDTO;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.po.DataSource;
import com.mp.domain.vo.QualityCheckResultVO;
import com.mp.domain.vo.QualityRuleVO;
import com.mp.executor.RuleExecutor;
import com.mp.executor.RuleExecutorFactory;
import com.mp.mapper.DataQualityRuleMapper;
import com.mp.mapper.DataSourceMapper;
import com.mp.service.IDataQualityCheckRecordService;
import com.mp.service.IDataQualityErrorService;
import com.mp.service.QualityRuleService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@Service
public class QualityRuleServiceImpl extends ServiceImpl<DataQualityRuleMapper, DataQualityRule> implements QualityRuleService {
    @Resource
    private DataQualityRuleMapper dataQualityRuleMapper;
    @Resource
    private DataSourceMapper dataSourceMapper;
    @Resource
    private RuleExecutorFactory ruleExecutorFactory;

    @Resource
    private IDataQualityCheckRecordService checkRecordService;
    @Resource
    private IDataQualityErrorService errorService;

    @Override
    public Result<Void> createRule(QualityRuleDTO ruleDTO) {
        DataQualityRule dataQualityRule = new DataQualityRule();
        BeanUtils.copyProperties(ruleDTO,dataQualityRule);
        save(dataQualityRule);
        return Result.success();
    }

    @Override
    public Result<List<QualityRuleVO>> listRules(String tableName) {
        List<DataQualityRule> rules = lambdaQuery()
                .eq(DataQualityRule::getTableName, tableName)
                .eq(DataQualityRule::getStatus, 1) //只查询启用的
                .list();

        List<QualityRuleVO> voList = rules.stream()
                .map(rule -> {
                    QualityRuleVO vo = new QualityRuleVO();
                    vo.setId(rule.getId());
                    vo.setRuleName(rule.getRuleName());
                    vo.setRuleType(rule.getRuleType());
                    return vo;
                }).toList();
        return Result.success(voList);
    }

    @Override
    public Result<QualityCheckResultVO> checkRule(Long ruleId) {
        //1. 查询规则
        DataQualityRule rule = dataQualityRuleMapper.selectById(ruleId);
        if(rule==null){
            return Result.error("该规则不存在");
        }

        //2. 查询数据源
        DataSource dataSource =
                dataSourceMapper.selectById(
                        rule.getDatasourceId()
                );
        if(dataSource == null){
            return Result.error("数据源不存在");
        }

        //3. 根据规则类型获取执行器
        RuleExecutor executor = ruleExecutorFactory.getExecutor(rule.getRuleType());
        //4. 执行规则
        QualityCheckResultVO result = executor.execute(rule, dataSource);

        //5.保存检查记录
        Long checkId =
                checkRecordService.saveCheckRecord(
                        rule,
                        result
                ); //得到了插入data_quality_check_record数据的主键id
        //6.保存异常数据
        errorService.saveErrorData(
                checkId,
                rule,
                result.getErrorData()
        );

        //7. 返回结果
        return Result.success(result);
    }
}
