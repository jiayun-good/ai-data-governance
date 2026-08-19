package com.mp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mp.domain.po.DataQualityError;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.DataQualityErrorVO;
import com.mp.mapper.DataQualityErrorMapper;
import com.mp.service.IDataQualityErrorService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 数据质量异常数据表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@Service
public class DataQualityErrorServiceImpl
        extends ServiceImpl<DataQualityErrorMapper, DataQualityError>
        implements IDataQualityErrorService {



    @Override
    public void saveErrorData(
            Long checkId,
            DataQualityRule rule,
            List<Map<String,Object>> errorData
    ){
        if(errorData == null ||
                errorData.isEmpty()){
            return;
        }

        List<DataQualityError> list =
                new ArrayList<>();

        for(Map<String,Object> data:errorData){

            DataQualityError error =
                    new DataQualityError();

            error.setCheckId(checkId);

            error.setRuleId(
                    rule.getId()
            );

            error.setTableName(
                    rule.getTableName()
            );

            error.setColumnName(
                    rule.getColumnName()
            );

            error.setErrorType(
                    rule.getRuleType()
            );

            error.setErrorMessage(
                    rule.getRuleName()
            );

            error.setErrorData(
                    JSON.toJSONString(data)
            );

            list.add(error);

        }

        //批量插入
        this.saveBatch(list);

    }

    @Override
    public IPage<DataQualityErrorVO> queryErrorPage(Long checkId, Integer page, Integer size) {
        Page<DataQualityError> pageParam = new Page<>(page,size);

        IPage<DataQualityError> errorPage = lambdaQuery()
                .eq(DataQualityError::getCheckId, checkId)
                .orderByDesc(DataQualityError::getCreateTime)
                .page(pageParam);

        //PO分页转VO分页
        Page<DataQualityErrorVO> voPage = new Page<>();

        voPage.setCurrent(errorPage.getCurrent());
        voPage.setSize(errorPage.getSize());
        voPage.setTotal(errorPage.getTotal());

        List<DataQualityErrorVO> voList = errorPage
                .getRecords()
                .stream()
                .map(error->{
                    DataQualityErrorVO vo = new DataQualityErrorVO();
                    BeanUtils.copyProperties(error,vo);
                    return vo;
                }).toList();

        voPage.setRecords(voList);
        return voPage;
    }

}
