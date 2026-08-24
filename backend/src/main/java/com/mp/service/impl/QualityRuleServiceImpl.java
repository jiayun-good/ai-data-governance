package com.mp.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mp.common.Result;
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

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@Slf4j
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

    @Override
    public void executeEnabledRules() {
        // 1. 查询所有启用的质量规则
        List<DataQualityRule> rules = lambdaQuery()
                .eq(DataQualityRule::getStatus, 1)
                .list();
        if (rules == null || rules.isEmpty()) {
            return;
        }
        // 2. 一个一个执行
        for (DataQualityRule rule : rules) {
            try {
                log.info(
                        "开始执行数据质量规则，ruleId={}, ruleName={}",
                        rule.getId(),
                        rule.getRuleName()
                );
                checkRule(rule.getId());
                log.info(
                        "数据质量规则执行完成，ruleId={}",
                        rule.getId()
                );

            } catch (Exception e) {
                log.error(
                        "数据质量规则执行失败，ruleId={}",
                        rule.getId(),
                        e
                );
                // 不要因为一个规则失败导致后面的规则全部不执行
            }
        }
    }
}
