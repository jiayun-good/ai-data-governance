package com.mp.controller;


import com.mp.common.Result;
import com.mp.domain.dto.QualityRuleDTO;
import com.mp.domain.vo.QualityCheckResultVO;
import com.mp.domain.vo.QualityRuleVO;
import com.mp.service.QualityRuleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@RestController
@RequestMapping("/quality")
public class QualityRuleController {
    @Resource
    private QualityRuleService dataQualityRuleService;
    /**创建规则*/
    @PostMapping("/rule")
    public Result<Void> createRule(@RequestBody QualityRuleDTO ruleDTO) {

        return dataQualityRuleService.createRule(ruleDTO);
    }

    /**查询表规则*/
    @GetMapping("/rule/list")
    public Result<List<QualityRuleVO>> listRules(
            @RequestParam String tableName) {

        return dataQualityRuleService.listRules(tableName);
    }

    /**执行规则*/
    @PostMapping("/check/{ruleId}")
    public Result<QualityCheckResultVO> checkRule(
            @PathVariable Long ruleId) {

        return dataQualityRuleService.checkRule(ruleId);
    }
}
