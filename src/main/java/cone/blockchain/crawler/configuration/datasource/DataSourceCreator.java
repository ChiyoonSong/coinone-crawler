package cone.blockchain.crawler.configuration.datasource;

import cone.blockchain.crawler.configuration.datasource.properties.HikariProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DataSourceCreator {

    public DataSource createHikariDataSource(DataSourceProperties properties, HikariProperties connectionProperties) {
        try {
            final HikariConfig dataSourceConfig = new HikariConfig();
            dataSourceConfig.setJdbcUrl(properties.getUrl());

            dataSourceConfig.setUsername(properties.getUsername());
            dataSourceConfig.setPassword(properties.getPassword());
            dataSourceConfig.setDriverClassName(properties.getDriverClassName());
            dataSourceConfig.setConnectionTimeout(connectionProperties.getConnectionTimeout());
            dataSourceConfig.setValidationTimeout(connectionProperties.getValidationTimeout());
            dataSourceConfig.setIdleTimeout(connectionProperties.getIdleTimeout());
            dataSourceConfig.setMaxLifetime(connectionProperties.getMaxLifetime());
            dataSourceConfig.setMaximumPoolSize(connectionProperties.getMaximumPoolSize());
            dataSourceConfig.setMinimumIdle(connectionProperties.getMinimumIdle());
            dataSourceConfig.setConnectionTestQuery(connectionProperties.getConnectionTestQuery());
            dataSourceConfig.setAutoCommit(connectionProperties.isAutoCommit());
            dataSourceConfig.setReadOnly(connectionProperties.isReadOnly());

            dataSourceConfig.addDataSourceProperty("cachePrepStmts", "true");
            dataSourceConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            dataSourceConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            return new HikariDataSource(dataSourceConfig);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}