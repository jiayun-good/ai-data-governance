package com.mp.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mp.domain.po.DataQualityCheckRecord;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.QualityCheckHistoryVO;
import com.mp.domain.vo.QualityCheckResultVO;
import com.mp.mapper.DataQualityCheckRecordMapper;
import com.mp.service.IDataQualityCheckRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 数据质量检查记录表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-19
 */
@Service
public class DataQualityCheckRecordServiceImpl
        extends ServiceImpl<DataQualityCheckRecordMapper, DataQualityCheckRecord>
        implements IDataQualityCheckRecordService {


    @Override
    public Long saveCheckRecord(
            DataQualityRule rule,
            QualityCheckResultVO result
    ){


        DataQualityCheckRecord record =
                new DataQualityCheckRecord();

        record.setRuleId(
                rule.getId()
        );


        record.setRuleName(
                rule.getRuleName()
        );


        record.setDatasourceId(
                rule.getDatasourceId()
        );

        record.setTableName(
                rule.getTableName()
        );

        record.setColumnName(
                rule.getColumnName()
        );

        record.setTotalCount(
                result.getTotal()
        );

        record.setSuccessCount(
                result.getSuccessCount()
        );

        record.setErrorCount(
                result.getErrorCount()
        );

        record.setStatus("SUCCESS");

        this.save(record);

        return record.getId();
    }

    @Override
    public IPage<QualityCheckHistoryVO> queryHistoryPage(Integer page, Integer size) {
        Page<DataQualityCheckRecord> pageParam =
                new Page<>(page,size);

        Page<DataQualityCheckRecord> result =
                lambdaQuery()
                        .orderByDesc(DataQualityCheckRecord::getCreateTime)
                        .page(pageParam);

        Page<QualityCheckHistoryVO> voPage = new Page<>();
        BeanUtils.copyProperties(result,voPage,"records");

        List<QualityCheckHistoryVO> records =
                result.getRecords()
                        .stream()
                        .map(item -> {
                            QualityCheckHistoryVO vo = new QualityCheckHistoryVO();
                            BeanUtils.copyProperties(item,vo);
                            return vo;
                        })
                        .toList();
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public IPage<QualityCheckHistoryVO> queryByRuleId(Long ruleId, Integer page, Integer size) {
        Page<DataQualityCheckRecord> pageParam =
                new Page<>(page,size);

        Page<DataQualityCheckRecord> result = lambdaQuery()
                        .eq(DataQualityCheckRecord::getRuleId,ruleId)
                        .orderByDesc(DataQualityCheckRecord::getCreateTime)
                        .page(pageParam);

        Page<QualityCheckHistoryVO> voPage = new Page<>();
        // 复制分页信息
        voPage.setTotal(result.getTotal());
        voPage.setCurrent(result.getCurrent());
        voPage.setSize(result.getSize());
        // 转换记录列表
        List<QualityCheckHistoryVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 实体转 VO
     */
    private QualityCheckHistoryVO convertToVO(DataQualityCheckRecord record) {
        if (record == null) {
            return null;
        }

        QualityCheckHistoryVO vo = new QualityCheckHistoryVO();
        // 手动复制字段（或使用 BeanUtils）
        BeanUtils.copyProperties(record, vo);

        // 如果有字段名不一致或需要特殊处理的，手动设置
        // vo.setXxx(record.getYyy());

        return vo;
    }
}
