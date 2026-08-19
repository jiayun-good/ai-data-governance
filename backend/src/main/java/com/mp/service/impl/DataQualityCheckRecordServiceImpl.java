package com.mp.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mp.domain.po.DataQualityCheckRecord;
import com.mp.domain.po.DataQualityRule;
import com.mp.domain.vo.QualityCheckResultVO;
import com.mp.mapper.DataQualityCheckRecordMapper;
import com.mp.service.IDataQualityCheckRecordService;
import org.springframework.stereotype.Service;

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

}
