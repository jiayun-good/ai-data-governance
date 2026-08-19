package com.mp.service;

import com.mp.common.Result;
import com.mp.domain.dto.DataSourceDTO;
import com.mp.domain.po.DataSource;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mp.domain.vo.ColumnVO;
import com.mp.domain.vo.DataSourceVO;

import java.util.List;

/**
 * <p>
 * 数据源信息表 服务类
 * </p>
 *
 * @author author
 * @since 2026-08-18
 */
public interface IDataSourceService extends IService<DataSource> {

    Result<Void> addDataSource(DataSourceDTO dataSourceDTO);

    Result<List<DataSourceVO>> listDataSource();

    Result<DataSourceVO> getDataSource(Long id);

    Result<Void> deleteDataSource(Long id);

    Result<Void> updateDataSource(Long id, DataSourceDTO dataSourceDTO);

    Result<Void> testConnection(Long id);

    Result<List<String>> queryTables(Long id);

    Result<List<ColumnVO>> getColumns(Long id, String tableName);
}
