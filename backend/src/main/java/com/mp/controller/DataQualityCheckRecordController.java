package com.mp.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mp.common.Result;
import com.mp.domain.vo.QualityCheckHistoryVO;
import com.mp.service.IDataQualityCheckRecordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 数据质量检查记录表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@RestController
@RequestMapping("/quality/check-record")
public class DataQualityCheckRecordController {
    @Resource
    private IDataQualityCheckRecordService checkRecordService;

    /**质量检查历史*/
    @GetMapping("/list")
    public Result<IPage<QualityCheckHistoryVO>> queryHistoryPage(Integer page, Integer size){
        return Result.success(checkRecordService.queryHistoryPage(page,size));
    }

    /**根据某个质量规则ID，查询这个规则历史执行过的所有检查记录。*/
    @GetMapping("/list/{ruleId}")
    public Result<IPage<QualityCheckHistoryVO>> queryByRuleId(@PathVariable Long ruleId, Integer page, Integer size){
        return Result.success(checkRecordService.queryByRuleId(ruleId,page,size));
    }
}
