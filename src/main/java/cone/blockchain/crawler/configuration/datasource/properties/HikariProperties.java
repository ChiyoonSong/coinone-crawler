package cone.blockchain.crawler.configuration.datasource.properties;

public interface HikariProperties {
    public int getConnectionTimeout();

    public int getValidationTimeout();

    public int getIdleTimeout();

    public int getMaxLifetime();

    public int getMaximumPoolSize();

    public int getMinimumIdle();

    public String getConnectionTestQuery();

    public boolean isAutoCommit();

    public boolean isReadOnly();
}
