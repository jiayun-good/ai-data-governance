package com.mp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mp.domain.po.DataQualityError;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.DataQualityErrorVO;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 数据质量异常数据表 服务类
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
public interface IDataQualityErrorService extends IService<DataQualityError> {
    /**
     * 保存异常数据
     *
     * @param checkId 检查记录ID
     * @param rule 质量规则
     * @param errorData 异常数据
     */
    void saveErrorData(
            Long checkId,
            DataQualityRule rule,
            List<Map<String,Object>> errorData
    );

    IPage<DataQualityErrorVO> queryErrorPage(Long checkId, Integer page, Integer size);
}
