package cone.blockchain.crawler.configuration.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationRoutingDataSource.class);

    private final String WRITE_DATASOURCE = "write";
    private final String READ_DATASOURCE = "read";

    // routing 로직
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceType = READ_DATASOURCE;
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            dataSourceType = WRITE_DATASOURCE;
        }
        logger.trace("current dataSourceType : {}", dataSourceType);
        return dataSourceType;
    }
}
