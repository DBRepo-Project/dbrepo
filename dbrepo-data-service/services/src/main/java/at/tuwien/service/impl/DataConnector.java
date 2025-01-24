package at.tuwien.service.impl;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public abstract class DataConnector {

    public ComboPooledDataSource getDataSource(ContainerDto container, String databaseName) {
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

    public ComboPooledDataSource getDataSource(ViewDto view) {
        return getDataSource(view.getDatabase().getContainer(), null);
    }

    public ComboPooledDataSource getDataSource(TableDto table) {
        return getDataSource(table.getDatabase().getContainer(), null);
    }

    public ComboPooledDataSource getDataSource(ContainerDto container) {
        return getDataSource(container, null);
    }

    public ComboPooledDataSource getDataSource(DatabaseDto database) {
        return getDataSource(database.getContainer(), database.getInternalName());
    }

    public String getSparkUrl(ContainerDto container, String databaseName) {
        final StringBuilder sb = new StringBuilder(getJdbcUrl(container, databaseName))
                .append("?sessionVariables=sql_mode='ANSI_QUOTES'");
        log.trace("mapped container to spark url: {}", sb.toString());
        return sb.toString();
    }

    public String getSparkUrl(TableDto table) {
        return getSparkUrl(table.getDatabase().getContainer(), null);
    }

    public String getSparkUrl(DatabaseDto databaseDto) {
        return getSparkUrl(databaseDto.getContainer(), null);
    }

    public String getSparkUrl(ContainerDto container) {
        return getSparkUrl(container, null);
    }

    public String getJdbcUrl(ContainerDto container, String databaseName) {
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
        log.trace("mapped jdbc url: {}", stringBuilder);
        return stringBuilder.toString();
    }

}
