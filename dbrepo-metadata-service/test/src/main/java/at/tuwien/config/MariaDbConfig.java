package at.tuwien.config;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.querystore.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class MariaDbConfig {

    public static void createInitDatabase(Container container, Database database) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getPrivilegedUsername(), container.getPrivilegedPassword())) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("init/" + database.getInternalName() + ".sql"), new ClassPathResource("init/querystore.sql"));
            populator.setSeparator(";\n");
            populator.populate(connection);
        }
    }

    public static void dropAllDatabases(Container container) {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getPrivilegedUsername(), container.getPrivilegedPassword())) {
            final String sql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME NOT IN ('information_schema', 'mysql', 'performance_schema');";
            log.trace("prepare statement '{}'", sql);
            final PreparedStatement statement = connection.prepareStatement(sql);
            final ResultSet resultSet = statement.executeQuery();
            final List<String> databases = new LinkedList<>();
            while (resultSet.next()) {
                databases.add(resultSet.getString(1));
            }
            resultSet.close();
            statement.close();
            for (String database : databases) {
                final String drop = "DROP DATABASE IF EXISTS `" + database + "`;";
                final PreparedStatement dropStatement = connection.prepareStatement(drop);
                dropStatement.executeUpdate();
                dropStatement.close();
            }
        } catch (SQLException e) {
            log.error("could not drop all databases", e);
        }
    }

    public static void insertQueryStore(Database database, Query query, String username) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getPrivilegedUsername(), database.getContainer().getPrivilegedPassword())) {
            final PreparedStatement prepareStatement = connection.prepareStatement(
                    "INSERT INTO qs_queries (created_by, query, query_normalized, is_persisted, query_hash, result_hash, result_number, created, executed) VALUES (?,?,?,?,?,?,?,?,?)");
            prepareStatement.setString(1, username);
            prepareStatement.setString(2, query.getQuery());
            prepareStatement.setString(3, query.getQuery());
            prepareStatement.setBoolean(4, query.getIsPersisted());
            prepareStatement.setString(5, query.getQueryHash());
            prepareStatement.setString(6, query.getResultHash());
            prepareStatement.setLong(7, query.getResultNumber());
            prepareStatement.setTimestamp(8, Timestamp.from(query.getCreated()));
            prepareStatement.setTimestamp(9, Timestamp.from(query.getExecuted()));
            log.trace("prepared statement: {}", prepareStatement);
            prepareStatement.executeUpdate();
        }
    }

    public static List<Map<String, Object>> listQueryStore(Database database) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getPrivilegedUsername(), database.getContainer().getPrivilegedPassword())) {
            final Statement statement = connection.createStatement();
            final ResultSet result = statement.executeQuery(
                    "SELECT created_by, query, query_normalized, is_persisted, query_hash, result_hash, result_number, created, executed FROM qs_queries");
            final List<Map<String, Object>> rows = new LinkedList<>();
            while (result.next()) {
                rows.add(new HashMap<>() {{
                    put("created_by", result.getString(1));
                    put("query", result.getString(2));
                    put("query_normalized", result.getString(3));
                    put("is_persisted", result.getBoolean(4));
                    put("query_hash", result.getString(5));
                    put("result_hash", result.getString(6));
                    put("result_number", result.getLong(7));
                    put("created", result.getTimestamp(8));
                    put("executed", result.getTimestamp(9));
                }});
            }
            return rows;
        }
    }

    public static List<Map<String, String>> selectQuery(Database database, String query, String... columns)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        final List<Map<String, String>> rows = new LinkedList<>();
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getPrivilegedUsername(), database.getContainer().getPrivilegedPassword())) {
            final Statement statement = connection.createStatement();
            final ResultSet result = statement.executeQuery(query);
            while (result.next()) {
                final Map<String, String> row = new HashMap<>();
                for (String column : columns) {
                    row.put(column, result.getString(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public static void execute(Database database, String query)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getPrivilegedUsername(), database.getContainer().getPrivilegedPassword())) {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        }
    }

    public static void execute(Container container, String query)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getPrivilegedUsername(), container.getPrivilegedPassword())) {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        }
    }
}
