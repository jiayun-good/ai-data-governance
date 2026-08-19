package com.mp.connector;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**把所有数据库连接器收集到一个 List 里，工厂以后根据数据库类型，从这个 List 里面找到对应的连接器。*/
public class DataSourceConnectorFactory {
    private final List<DataSourceConnector> connectors;

    public DataSourceConnectorFactory(List<DataSourceConnector> connectors) {
        this.connectors = connectors;
    }

    public DataSourceConnector getConnector(String type) {

        return connectors.stream()
                .filter(connector -> connector.supports(type))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("不支持的数据源类型：" + type)
                );
    }

}
