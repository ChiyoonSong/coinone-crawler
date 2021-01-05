package cone.blockchain.crawler.configuration;

import cone.blockchain.crawler.configuration.datasource.DataSourceCreator;
import cone.blockchain.crawler.configuration.datasource.ReplicationRoutingDataSource;
import cone.blockchain.crawler.configuration.datasource.properties.HikariProperties;
import cone.blockchain.crawler.configuration.datasource.properties.ReadHikariProperties;
import cone.blockchain.crawler.configuration.datasource.properties.WriteHikariProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * DataSourceConfig
 * <pre>
 * DataSource(writeDataSource, readDataSource)
 * --> HikariCP wrapping
 *
 * --> ReplicationRoutingDataSource
 * --> LazyConnectionDataSourceProxy
 * --> DataSourceTransactionManager
 * --> SqlSessionFactoryBean
 * --> SqlSessionTemplate
 * </pre>
 */

@Configuration
@MapperScan({"cone.blockchain.*"})
@EnableConfigurationProperties({WriteHikariProperties.class, ReadHikariProperties.class})
@EnableTransactionManagement
public class MultiTransactionConfiguration {

    private final String mybatisConfigLocation = "classpath:mybatis-config.xml";
    private final String mapperClassLocaion = "classpath*:cone/blockchain/crawler/biz/mapper/*.xml";

    private final String WRITE_DATASOURCE = "write";
    private final String READ_DATASOURCE = "read";

    private final DataSourceCreator dataSourceCreator;

    public MultiTransactionConfiguration(DataSourceCreator dataSourceCreator) {
        this.dataSourceCreator = dataSourceCreator;
    }

    @Primary
    @Bean(name = "writeDataSource", destroyMethod = "close")
    public DataSource writeDataSource(WriteHikariProperties writeHikariProperties) {
        return dataSourceCreator.createHikariDataSource(writeHikariProperties, writeHikariProperties);
    }

    @Bean(name = "readDataSource", destroyMethod = "close")
    public DataSource readDataSource(ReadHikariProperties readHikariProperties) {
        HikariProperties hikariProperties = readHikariProperties;
        return dataSourceCreator.createHikariDataSource(readHikariProperties, hikariProperties);
    }

    /**
     * routingDataSource
     * <p>
     * defaultTargetSource ---> readDataSource
     *
     * @param writeDataSource
     * @param readDataSource
     * @return
     */
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(@Qualifier("writeDataSource") DataSource writeDataSource, @Qualifier("readDataSource") DataSource readDataSource) {
        ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(WRITE_DATASOURCE, writeDataSource);
        dataSourceMap.put(READ_DATASOURCE, readDataSource);
        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        return routingDataSource;
    }

    @Bean(name = "dataSource")
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource
            , ApplicationContext applicationContext) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources(mapperClassLocaion));
        sqlSessionFactoryBean.setConfigLocation(applicationContext.getResource(mybatisConfigLocation));

        final Properties sqlSessionFactoryProperties = new Properties();
        sqlSessionFactoryProperties.put("jdbcTypeForNull", "NULL");
        sqlSessionFactoryBean.setConfigurationProperties(sqlSessionFactoryProperties);

        return sqlSessionFactoryBean.getObject();
    }

    @Bean(name = "sqlSessionTemplate", destroyMethod = "clearCache")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
