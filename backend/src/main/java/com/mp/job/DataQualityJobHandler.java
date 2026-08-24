package com.mp.job;

import org.springframework.stereotype.Component;

import com.mp.service.QualityRuleService;
import com.xxl.job.core.handler.annotation.XxlJob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataQualityJobHandler {
    private final QualityRuleService qualityRuleService;

    /**
     * 数据质量规则定时检查
     */
    @XxlJob("dataQualityCheckJob")
    public void dataQualityCheckJob() {

        log.info("========== 数据质量定时检查开始 ==========");

        try {

            // 查询需要执行的质量规则
            qualityRuleService.executeEnabledRules();

            log.info("========== 数据质量定时检查完成 ==========");

        } catch (Exception e) {

            log.error("数据质量定时检查失败", e);

            // 抛异常，让 XXL-JOB 判断本次任务失败
            throw e;
        }
    }
}