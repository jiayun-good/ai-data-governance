package com.mp.connector;

import com.mp.domain.po.DataSource;
import com.mp.domain.vo.ColumnVO;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface DataSourceConnector {

    /**
     * 判断当前连接器是否支持指定的数据源类型
     */
    boolean supports(String type);

    /**
     * 测试数据库连接
     */
    boolean testConnection(DataSource dataSource);

    /**
     * 获取数据库连接
     */
    Connection getConnection(DataSource dataSource);

    /**
     * 查询数据库下所有的表名
     * */
    List<String> getTables(DataSource dataSource);

    /**
     * 查指定表下的字段
     * */
    List<ColumnVO> getColumns(DataSource dataSource, String tableName);

    /**
     * 执行统计SQL
     */
    Long count(
            DataSource dataSource,
            String sql
    );

    /**查异常数据*/
    List<Map<String,Object>> query(
            DataSource dataSource,
            String sql
    );
}