package cone.blockchain.crawler.configuration.datasource.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "spring.write.datasource")
public class WriteHikariProperties extends DataSourceProperties implements HikariProperties {
    private int connectionTimeout;
    private int validationTimeout;
    private int idleTimeout;
    private int maxLifetime;
    private int maximumPoolSize;
    private int minimumIdle;
    private String connectionTestQuery;
    private boolean autoCommit;
    private boolean readOnly = false;
}
