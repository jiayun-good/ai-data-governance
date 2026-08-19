package com.mp.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mp.common.Result;
import com.mp.domain.dto.QualityRuleDTO;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.QualityCheckResultVO;
import com.mp.domain.vo.QualityRuleVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
public interface QualityRuleService extends IService<DataQualityRule> {

    Result<Void> createRule(QualityRuleDTO ruleDTO);

    Result<List<QualityRuleVO>> listRules(String tableName);

    Result<QualityCheckResultVO> checkRule(Long ruleId);
}
