package com.mp.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mp.common.Result;
import com.mp.domain.vo.DataQualityErrorVO;
import com.mp.service.IDataQualityErrorService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 数据质量异常数据表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@RestController
@RequestMapping("/check/errors")
public class DataQualityErrorController {
    @Resource
    private IDataQualityErrorService dataQualityErrorService;
    /**
     * 查询某次检查异常数据
     * */
    @GetMapping("/list/{checkId}")
    public Result<IPage<DataQualityErrorVO>> queryErrorPage(
            @PathVariable Long checkId,
            Integer page,
            Integer size
    ){
        return Result.success(dataQualityErrorService.queryErrorPage(checkId,page,size));
    }

}
