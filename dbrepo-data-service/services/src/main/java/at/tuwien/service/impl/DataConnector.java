package at.tuwien.service.impl;

import at.tuwien.api.CacheableDto;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public abstract class DataConnector<T extends CacheableDto> {

    public ComboPooledDataSource getDataSource(T entity) {
        final long start = System.currentTimeMillis();
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(getJdbcUrl(entity.getJdbcMethod(), entity.getHost(), entity.getPassword(),
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
        final long start = System.currentTimeMillis();
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(getJdbcUrl(entity.getJdbcMethod(), entity.getHost(), entity.getPassword(), databaseName));
        dataSource.setUser(entity.getUsername());
        dataSource.setPassword(entity.getPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        return dataSource;
    }

    public String getSparkUrl(String jdbcMethod, String host, String password, String databaseName) {
        final StringBuilder sb = new StringBuilder(getJdbcUrl(jdbcMethod, host, password, databaseName))
                .append("?sessionVariables=sql_mode='ANSI_QUOTES'");
        log.trace("mapped container to spark url: {}", sb.toString());
        return sb.toString();
    }

    public String getSparkUrl(T entity) {
        return getSparkUrl(entity.getJdbcMethod(), entity.getHost(), entity.getPassword(), entity.getDatabase());
    }

    public String getJdbcUrl(String jdbcMethod, String host, String password, String databaseName) {
        final StringBuilder stringBuilder = new StringBuilder("jdbc:")
                .append(jdbcMethod)
                .append("://")
                .append(host)
                .append(":")
                .append(password);
        if (databaseName != null) {
            stringBuilder.append("/")
                    .append(databaseName);
        }
        return stringBuilder.toString();
    }

}
