package at.tuwien.service.impl;

import at.tuwien.api.CacheableDto;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public abstract class DataConnector<T extends CacheableDto> {

    public ComboPooledDataSource getDataSource(T entity) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(getJdbcUrl(entity.getJdbcMethod(), entity.getHost(), entity.getPort(),
                entity.getDatabase()));
        dataSource.setUser(entity.getUsername());
        dataSource.setPassword(entity.getPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        return dataSource;
    }

    public ComboPooledDataSource getDataSource(T entity, String databaseName) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(getJdbcUrl(entity.getJdbcMethod(), entity.getHost(), entity.getPort(), databaseName));
        dataSource.setUser(entity.getUsername());
        dataSource.setPassword(entity.getPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        return dataSource;
    }

    public String getSparkUrl(String jdbcMethod, String host, Integer port, String databaseName) {
        final StringBuilder sb = new StringBuilder(getJdbcUrl(jdbcMethod, host, port, databaseName))
                .append("?sessionVariables=sql_mode='ANSI_QUOTES'");
        log.trace("mapped container to spark url: {}", sb.toString());
        return sb.toString();
    }

    public String getSparkUrl(T entity) {
        return getSparkUrl(entity.getJdbcMethod(), entity.getHost(), entity.getPort(), entity.getDatabase());
    }

    public String getJdbcUrl(String jdbcMethod, String host, Integer port, String databaseName) {
        final StringBuilder stringBuilder = new StringBuilder("jdbc:")
                .append(jdbcMethod)
                .append("://")
                .append(host)
                .append(":")
                .append(port);
        if (databaseName != null) {
            stringBuilder.append("/")
                    .append(databaseName);
        }
        log.trace("mapped jdbc url: {}", stringBuilder);
        return stringBuilder.toString();
    }

}
