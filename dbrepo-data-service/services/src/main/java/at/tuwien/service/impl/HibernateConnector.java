package at.tuwien.service.impl;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryMalformedException;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Log4j2
@Service
public abstract class HibernateConnector {

    public static ComboPooledDataSource getPrivilegedDataSource(ContainerImage image, Container container) {
        return getPrivilegedDataSource(image, container, null);
    }

    public static ComboPooledDataSource getPrivilegedDataSource(ContainerImage image, Container container, Database database) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(url(image, container, database));
        dataSource.setUser(container.getPrivilegedUsername());
        dataSource.setPassword(container.getPrivilegedPassword());
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        log.trace("created pooled data source {}", dataSource);
        return dataSource;
    }

    public static ComboPooledDataSource getDataSource(ContainerImage image, Container container, Database database, String username, String password) {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setJdbcUrl(url(image, container, database));
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        log.trace("created pooled data source {}", dataSource);
        return dataSource;
    }

    private static String url(ContainerImage image, Container container, Database database) {
        final StringBuilder stringBuilder = new StringBuilder("jdbc:")
                .append(image.getJdbcMethod())
                .append("://")
                .append(container.getHost())
                .append(":")
                .append(container.getPort())
                .append("/");
        if (database != null) {
            stringBuilder.append(database.getInternalName())
                    .append("?currentSchema=")
                    .append(database.getInternalName());
        }

        log.debug("connecting via jdbc, url={}", stringBuilder);
        return stringBuilder.toString();
    }

    public PreparedStatement prepareStatement(Connection connection, String statement) throws QueryMalformedException {
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}m reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

}
