package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.duckdb.DuckDBConnection;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
@Service
public abstract class DataConnector {

    public ComboPooledDataSource getDataSource(Container container, String databaseName) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(getJdbcUrl(container, databaseName));
        dataSource.setUser(container.getUsername());
        dataSource.setPassword(container.getPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        return dataSource;
    }

    public ComboPooledDataSource getDataSource(Container container) {
        return getDataSource(container, null);
    }

    public ComboPooledDataSource getDataSource(Database database) {
        return getDataSource(database.getContainer(), database.getInternalName());
    }

    public String getSparkJdbcUrl(Container container, String databaseName) {
        final StringBuilder sb = new StringBuilder(getJdbcUrl(container, databaseName))
                .append("?sessionVariables=sql_mode='ANSI_QUOTES'");
        log.trace("mapped container to spark jdbc url: {}", sb);
        return sb.toString();
    }

    public String getSparkJdbcUrl(Database databaseDto) {
        return getSparkJdbcUrl(databaseDto.getContainer(), databaseDto.getInternalName());
    }

    public String getJdbcUrl(Container container, String databaseName) {
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
        log.trace("mapped container to jdbc url: {}", stringBuilder);
        return stringBuilder.toString();
    }

    public final DuckDBConnection getDuckDbConnection() throws SQLException {
        return (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
    }

}
