package com.mp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mp.common.Result;
import com.mp.domain.po.DataQualityCheckRecord;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.QualityCheckHistoryVO;
import com.mp.domain.vo.QualityCheckResultVO;

import java.util.List;


/**
 * <p>
 * 数据质量检查记录表 服务类
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
public interface IDataQualityCheckRecordService extends IService<DataQualityCheckRecord> {
    /**
     * 保存质量检查记录
     *
     * @param rule 质量规则
     * @param result 检查结果
     * @return 检查记录ID
     */
    Long saveCheckRecord(
            DataQualityRule rule,
            QualityCheckResultVO result
    );

    IPage<QualityCheckHistoryVO> queryHistoryPage(Integer page, Integer size);

    IPage<QualityCheckHistoryVO> queryByRuleId(Long ruleId, Integer page, Integer size);
}
