package at.tuwien.service.impl;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public abstract class HibernateConnector {

    public static ComboPooledDataSource getPrivilegedDataSource(PrivilegedContainerDto container, String databaseName) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(url(container, databaseName));
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        log.trace("created pooled data source {} (user={}, password=(hidden))", url(container, databaseName), container.getUsername());
        return dataSource;
    }

    public static ComboPooledDataSource getPrivilegedDataSource(PrivilegedDatabaseDto database) {
        return getPrivilegedDataSource(database.getContainer(), database.getInternalName());
    }

    private static String url(PrivilegedContainerDto container, String databaseName) {
        final StringBuilder stringBuilder = new StringBuilder("jdbc:")
                .append(container.getImage().getJdbcMethod())
                .append("://")
                .append(container.getHost())
                .append(":")
                .append(container.getPort());
        if (databaseName != null) {
            stringBuilder.append("/")
                    .append(databaseName)
                    .append("?currentSchema=")
                    .append(databaseName);
        }
        return stringBuilder.toString();
    }

}
