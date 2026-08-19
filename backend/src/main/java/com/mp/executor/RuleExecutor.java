package com.mp.executor;

import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.QualityCheckResultVO;

import com.mp.domain.po.DataSource;

public interface RuleExecutor {
    /**
     * 当前执行器支持的规则类型
     */
    String getRuleType();


    /**
     * 执行规则
     */
    QualityCheckResultVO execute(
            DataQualityRule rule,
            DataSource dataSource
    );
}
