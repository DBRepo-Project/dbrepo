package at.tuwien.service.impl;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public abstract class HibernateConnector {

    public ComboPooledDataSource getPrivilegedDataSource(PrivilegedContainerDto container, String databaseName) {
        final long start = System.currentTimeMillis();
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(url(container, databaseName));
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        log.trace("created pooled data source {} in {} ms, user={}", url(container, databaseName), System.currentTimeMillis() - start, container.getUsername());
        return dataSource;
    }

    public ComboPooledDataSource getPrivilegedDataSource(PrivilegedDatabaseDto database) {
        return getPrivilegedDataSource(database.getContainer(), database.getInternalName());
    }

    public String getSparkUrl(PrivilegedContainerDto container, String databaseName) {
        final StringBuilder sb = new StringBuilder(url(container, databaseName))
                .append("?sessionVariables=sql_mode='ANSI_QUOTES'");
        log.trace("mapped container to spark url: {}", sb.toString());
        return sb.toString();
    }

    private String url(PrivilegedContainerDto container, String databaseName) {
        final StringBuilder stringBuilder = new StringBuilder("jdbc:")
                .append(container.getImage().getJdbcMethod())
                .append("://")
                .append(container.getHost())
                .append(":")
                .append(container.getPort());
        if (databaseName != null) {
            stringBuilder.append("/")
                    .append(databaseName);
        }
        return stringBuilder.toString();
    }

}
