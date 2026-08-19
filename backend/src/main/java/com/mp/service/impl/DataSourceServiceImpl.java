package com.mp.service.impl;

import com.mp.common.Result;
import com.mp.connector.DataSourceConnector;
import com.mp.connector.DataSourceConnectorFactory;
import com.mp.domain.dto.DataSourceDTO;
import com.mp.domain.po.DataSource;
import com.mp.domain.vo.ColumnVO;
import com.mp.domain.vo.DataSourceVO;
import com.mp.mapper.DataSourceMapper;
import com.mp.service.IDataSourceService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 数据源信息表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-18
 */
@Service
public class DataSourceServiceImpl extends ServiceImpl<DataSourceMapper, DataSource> implements IDataSourceService {
    @Resource
    private DataSourceConnectorFactory connectorFactory;
    @Resource
    private DataSourceMapper dataSourceMapper;

    @Override
    public Result<Void> addDataSource(DataSourceDTO dataSourceDTO) {
        // 1. 判断数据源名称是否已经存在
        DataSource exist = lambdaQuery().eq(DataSource::getName, dataSourceDTO.getName()).one();
        if(exist !=null){
            return Result.error("数据源名称已存在");
        }
        // 2. DTO 转实体
        DataSource dataSource = new DataSource();
        BeanUtils.copyProperties(dataSourceDTO,dataSource);
        // 3. 保存数据源
        save(dataSource);
        return Result.success();
    }

    @Override
    public Result<List<DataSourceVO>> listDataSource() {
        List<DataSource> list = lambdaQuery().orderByDesc(DataSource::getCreateTime).list();
        List<DataSourceVO> voList = list.stream()
                .map(dataSource -> {
                    DataSourceVO dataSourceVO = new DataSourceVO();
                    BeanUtils.copyProperties(dataSource, dataSourceVO);
                    return dataSourceVO;
                }).toList();
        return Result.success(voList);
    }

    @Override
    public Result<DataSourceVO> getDataSource(Long id) {
        DataSource dataSource = lambdaQuery().eq(DataSource::getId, id).one();
        DataSourceVO dataSourceVO = new DataSourceVO();
        BeanUtils.copyProperties(dataSource,dataSourceVO);
        return Result.success(dataSourceVO);
    }

    @Override
    public Result<Void> deleteDataSource(Long id) {

        DataSource dataSource = getById(id);

        if (dataSource == null) {
            return Result.error("数据源不存在");
        }

        removeById(id);

        return Result.success();
    }

    @Override
    public Result<Void> updateDataSource(Long id, DataSourceDTO dataSourceDTO) {
        DataSource exist = getById(id);
        if(exist==null){
            return Result.error("数据源不存在");
        }

        //判断名字是否已存在-数据源名称不能重复
        DataSource dataSource = lambdaQuery()
                .eq(DataSource::getName, dataSourceDTO.getName())
                .ne(DataSource::getId,id) //排除正在修改的自己
                .one();
        if(dataSource!=null){
            return Result.error("数据源名称已存在");
        }
        BeanUtils.copyProperties(dataSourceDTO,exist);
        updateById(exist);
        return Result.success();
    }

    @Override
    public Result<Void> testConnection(Long id) {
        //1. 根据 ID 查询数据源
        DataSource exsit = getById(id);
        if(exsit == null){
            return Result.error("数据源不存在");
        }
        try{
            // 2. 根据 type 获取对应的连接器
            DataSourceConnector connector = connectorFactory.getConnector(exsit.getType());
            //3.测试连接
            boolean success = connector.testConnection(exsit);
            if (!success) {
                return Result.error("数据库连接失败");
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error("数据库连接失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<String>> queryTables(Long id) {
        // 1. 根据id查询数据源
        DataSource dataSource = getById(id);
        if(dataSource==null){
            return Result.error("数据源不存在");
        }
        //2.调用数据库连接策略工厂,找到该用的连接器
        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());
        List<String> tables = connector.getTables(dataSource);

        return Result.success(tables);
    }

    @Override
    public Result<List<ColumnVO>> getColumns(Long id, String tableName) {
        DataSource dataSource = dataSourceMapper.selectById(id);
        if(dataSource == null){
            return Result.error("数据源不存在");
        }
        DataSourceConnector connector = connectorFactory.getConnector(dataSource.getType());
        List<ColumnVO> columns = connector.getColumns(dataSource, tableName);

        return Result.success(columns);
    }
}
