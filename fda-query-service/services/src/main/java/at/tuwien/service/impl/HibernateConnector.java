package at.tuwien.service.impl;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseConnectionException;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.stream.Collectors;

@Log4j2
@Service
public abstract class HibernateConnector {

    protected static Connection getConnection(ContainerImage image, Container container, Database database) throws DatabaseConnectionException {
        final ComboPooledDataSource dataSource = new ComboPooledDataSource();
        final String url = "jdbc:" + image.getJdbcMethod() + "://" + container.getInternalName() + "/" + (database != null ? database.getInternalName() : "");
        dataSource.setJdbcUrl(url);
        final String username = image.getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);
        dataSource.setUser(username);
        final String password = image.getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);
        dataSource.setPassword(password);
        dataSource.setInitialPoolSize(5);
        dataSource.setMinPoolSize(5);
        dataSource.setAcquireIncrement(5);
        dataSource.setMaxPoolSize(20);
        dataSource.setMaxStatements(100);
        final Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            log.error("Failed to connect to the database");
            log.debug("failed to connect to the database {}", database);
            throw new DatabaseConnectionException("Failed to connect to the database");
        }
        return connection;
    }

    protected static Long activeConnection(Connection connection) throws DatabaseConnectionException {
        final ResultSet resultSet = execute(connection, "SHOW STATUS LIKE 'threads_connected'");
        try {
            if (resultSet.next()) {
                return resultSet.getLong(2);
            }
        } catch (SQLException e) {
            log.error("Failed to determine active connections");
            throw new DatabaseConnectionException("Failed to determine active connections", e);
        }
        log.error("Failed to determine active connections");
        throw new DatabaseConnectionException("Failed to determine active connections");
    }

    protected static ResultSet execute(Connection connection, String statement) throws DatabaseConnectionException {
        return execute(connection, statement, null);
    }

    protected static ResultSet execute(Connection connection, String statement, Collection<Object> data) throws DatabaseConnectionException {
        final PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement(statement, data);
            return preparedStatement.executeQuery();
        } catch (SQLException e) {
            log.error("Failed to execute statement");
            log.debug("failed to execute statement {}", statement);
            throw new DatabaseConnectionException("Failed to execute statement", e);
        }
    }

}
