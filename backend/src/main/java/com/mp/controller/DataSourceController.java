package com.mp.controller;


import com.mp.common.Result;
import com.mp.domain.dto.DataSourceDTO;
import com.mp.domain.vo.ColumnVO;
import com.mp.domain.vo.DataSourceVO;
import com.mp.service.IDataSourceService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import javax.xml.crypto.Data;
import java.util.List;

/**
 * <p>
 * 数据源信息表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-18
 */
@RestController
@RequestMapping("/data-source")
public class DataSourceController {
    @Resource
    private IDataSourceService dataSourceService;
    /*添加数据源*/
    @PostMapping()
    public Result<Void> addDataSource(@RequestBody DataSourceDTO dataSourceDTO){

        return dataSourceService.addDataSource(dataSourceDTO);
    }

    /*查询数据源列表*/
    @GetMapping("/list")
    public Result<List<DataSourceVO>> listDataSource(){
        return dataSourceService.listDataSource();
    }

    /*查询数据源详情*/
    @GetMapping("/{id}")
    public Result<DataSourceVO> getDataSource(@PathVariable Long id){
        return dataSourceService.getDataSource(id);
    }
    /*删除数据源*/
    @DeleteMapping("/{id}")
    public Result<Void> deleteDataSource(@PathVariable Long id) {
        return dataSourceService.deleteDataSource(id);
    }

    /*修改数据源*/
    @PutMapping("/{id}")
    public Result<Void> updateDataSource(
            @PathVariable Long id,
            @RequestBody DataSourceDTO dataSourceDTO) {

        return dataSourceService.updateDataSource(id, dataSourceDTO);
    }
    /*测试连接数据源*/
    @PostMapping("/{id}/test")
    public Result<Void> testConnection(@PathVariable Long id){
        return dataSourceService.testConnection(id);
    }

    /*获取数据表*/
    @GetMapping("/{id}/tables")
    public Result<List<String>> queryTables(@PathVariable Long id){
        return dataSourceService.queryTables(id);
    }

    /*获取字段*/
    @GetMapping("/{id}/tables/{tableName}/columns")
    public Result<List<ColumnVO>> getColumns(@PathVariable Long id,@PathVariable String tableName){
        return dataSourceService.getColumns(id,tableName);
    }
}
