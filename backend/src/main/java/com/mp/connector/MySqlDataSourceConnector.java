package com.mp.connector;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.mp.domain.po.DataSource;
import com.mp.domain.vo.ColumnVO;

@Component
public class MySqlDataSourceConnector implements DataSourceConnector {

    @Override
    public boolean supports(String type) {
        return "mysql".equalsIgnoreCase(type);
    }

    @Override
    public Connection getConnection(DataSource dataSource) {

        String url = String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDatabaseName()
        );

        try {
            return DriverManager.getConnection(
                    url,
                    dataSource.getUsername(),
                    dataSource.getPassword()
            );
        } catch (SQLException e) {
            throw new RuntimeException("MySQL数据库连接失败", e);
        }
    }

    @Override
    public List<String> getTables(DataSource dataSource) {
        List<String> tables = new ArrayList<>();
        Connection conn = getConnection(dataSource);
        try {
            //根据数据库连接 Connection 创建一个 SQL 执行对象
            Statement statement = conn.createStatement();
            //执行查询 SQL，并返回查询结果。
            ResultSet showTables = statement.executeQuery("show tables");
            while (showTables.next()){
                tables.add(showTables.getString(1));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return tables;
    }

    @Override
    public List<ColumnVO> getColumns(DataSource dataSource, String tableName) {
        List<ColumnVO> list =
                new ArrayList<>();
        String sql = "show columns from " + tableName;
        //根据数据库连接 Connection 创建一个 SQL 执行对象
        try {
            // 1. 获取数据库连接
            Connection conn = getConnection(dataSource);
            // 2. 创建SQL执行对象
            DatabaseMetaData metaData = conn.getMetaData();
            // 3. 查询列信息
            ResultSet columns = metaData.getColumns(null, null, tableName, "%");

            while (columns.next()){
                ColumnVO vo = new ColumnVO();
                // 字段名称
                columns.getString("COLUMN_NAME");
                vo.setColumnName(
                        columns.getString("COLUMN_NAME")
                );

                vo.setDataType(
                        columns.getString("TYPE_NAME")
                );

                vo.setLength(
                        columns.getInt("COLUMN_SIZE")
                );

                vo.setNullable(
                        columns.getInt("NULLABLE")
                                ==
                                DatabaseMetaData.columnNullable
                );


                vo.setComment(
                        columns.getString("REMARKS")
                );
                list.add(vo);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Long count(DataSource dataSource, String sql) {
        try(// 这里声明需要自动关闭的资源，用分号分隔
            Connection conn = getConnection(dataSource);
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery() //执行查询语句并返回结果集
        ) {
            if(rs.next()) {
                return rs.getLong(1);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "执行统计SQL失败",
                    e
            );
        }
        return 0L;
    }

    @Override
    public List<Map<String, Object>> query(DataSource dataSource, String sql) {
        try(Connection conn = getConnection(dataSource);
            PreparedStatement ps =
                    conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            List<Map<String,Object>> list =
                    new ArrayList<>();

            ResultSetMetaData metaData =
                    rs.getMetaData();

            int columnCount =
                    metaData.getColumnCount();

            while(rs.next()){

                Map<String,Object> row =
                        new HashMap<>();

                for(int i=1;i<=columnCount;i++){

                    String columnName =
                            metaData.getColumnName(i);

                    row.put(
                            columnName,
                            rs.getObject(i)
                    );

                }
                list.add(row);

            }
            return list;

        }catch(Exception e){
            throw new RuntimeException(
                    "查询异常数据失败",
                    e
            );
        }
    }

    @Override
    public boolean testConnection(DataSource dataSource) {

        try (Connection connection = getConnection(dataSource)) {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}