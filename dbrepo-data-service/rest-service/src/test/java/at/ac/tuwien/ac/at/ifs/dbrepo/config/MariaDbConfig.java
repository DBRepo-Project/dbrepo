package at.ac.tuwien.ac.at.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Configuration
public class MariaDbConfig {

    public static void createDatabase(ContainerDto container, String database) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getUsername(), container.getPassword())) {
            final String sql = "CREATE DATABASE `" + database + "`;";
            log.trace("prepare statement '{}'", sql);
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        }
        log.debug("created database {}", database);
    }

    public static void createInitDatabase(DatabaseDto database) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("init/" + database.getInternalName() + ".sql"), new ClassPathResource("init/users.sql"), new ClassPathResource("init/querystore.sql"));
            populator.setSeparator(";\n");
            populator.populate(connection);
        }
        log.debug("created init database {}", database.getInternalName());
    }

    public static void grantWriteAccess(DatabaseDto database, String username) {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            connection.prepareStatement("GRANT SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE ON *.* TO `" + username + "`@`%`;")
                    .executeUpdate();
            connection.prepareStatement("FLUSH PRIVILEGES;")
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("could not grant read access", e);
        }
        log.debug("granted read access to user {} in database {}", username, database.getInternalName());
    }

    public static void dropAllDatabases(ContainerDto container) {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getUsername(), container.getPassword())) {
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
        log.debug("dropped all databases");
    }

    public static void dropDatabase(ContainerDto container, String database)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getUsername(), container.getPassword())) {
            final String sql = "DROP DATABASE IF EXISTS `" + database + "`;";
            log.trace("prepare statement '{}'", sql);
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        }
        log.debug("dropped database {}", database);
    }

    public static List<String> getPrivileges(DatabaseDto database, String username) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final String query = "SHOW GRANTS FOR `" + username + "`;";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            final ResultSet set = statement.executeQuery();
            statement.close();
            if (set.next()) {
                final Matcher matcher = Pattern.compile("GRANT (.*) ON.*").matcher(set.getString(1));
                if (matcher.find()) {
                    final List<String> privileges = Arrays.asList(matcher.group(1).split(","));
                    ;
                    log.trace("found privileges: {}", privileges);
                    return privileges;
                }
            }
        }
        throw new SQLException("Failed to get privileges");
    }

    public static void dropTable(DatabaseDto database, String table) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final String query = "DROP TABLE `" + table + "`;";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        }
        log.debug("dropped table {}", table);
    }

    public static void mockQuery(String hostname, Integer port, String database, String query, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + ":" + port + "/" + database;
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, username, password)) {
            final PreparedStatement statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        }
    }

    public static UUID insertQueryStore(DatabaseDto database, QueryDto query, UUID userId) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database: {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            PreparedStatement prepareStatement = connection.prepareStatement(
                    "INSERT INTO qs_queries (created_by, query, query_normalized, is_persisted, query_hash, result_hash, result_number, created, executed) VALUES (?,?,?,?,?,?,?,?,?)");
            prepareStatement.setString(1, String.valueOf(userId));
            prepareStatement.setString(2, query.getQuery());
            prepareStatement.setString(3, query.getQuery());
            prepareStatement.setBoolean(4, query.getIsPersisted());
            prepareStatement.setString(5, query.getQueryHash());
            prepareStatement.setString(6, query.getResultHash());
            prepareStatement.setLong(7, query.getResultNumber());
            prepareStatement.setTimestamp(8, Timestamp.from(query.getExecution()));
            prepareStatement.setTimestamp(9, Timestamp.from(query.getExecution()));
            log.trace("prepared statement: {}", prepareStatement);
            prepareStatement.executeUpdate();
            /* select */
            prepareStatement = connection.prepareStatement("SELECT id FROM qs_queries WHERE query_hash = ? LIMIT 1");
            prepareStatement.setString(1, query.getQueryHash());
            final ResultSet result = prepareStatement.executeQuery();
            UUID queryId;
            result.next();
            return UUID.fromString(result.getString(1));
        }
    }

    public static List<Map<String, Object>> listQueryStore(DatabaseDto database) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
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

    public static List<Map<String, String>> selectQuery(DatabaseDto database, String query, Set<String> columns)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        final List<Map<String, String>> rows = new LinkedList<>();
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final Statement statement = connection.createStatement();
            log.trace("execute query: {}", query);
            final ResultSet result = statement.executeQuery(query);
            log.trace("map result set to columns: {}", columns);
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

    public static List<Map<String, byte[]>> selectQueryByteArr(DatabaseDto database, String query, Set<String> columns)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        final List<Map<String, byte[]>> rows = new LinkedList<>();
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final Statement statement = connection.createStatement();
            log.trace("execute query: {}", query);
            final ResultSet result = statement.executeQuery(query);
            log.trace("map result set to columns: {}", columns);
            while (result.next()) {
                final Map<String, byte[]> row = new HashMap<>();
                for (String column : columns) {
                    row.put(column, result.getBytes(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public static void execute(DatabaseDto database, String query)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        }
    }

    public static void dropQueryStore(DatabaseDto database)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database: {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final Statement statement = connection.createStatement();
            statement.executeUpdate("DROP SEQUENCE IF EXISTS `qs_queries_seq`;");
            statement.executeUpdate("DROP TABLE IF EXISTS `qs_queries`;");
            statement.executeUpdate("DROP PROCEDURE IF EXISTS `hash_table`;");
            statement.executeUpdate("DROP PROCEDURE IF EXISTS `store_query`;");
            statement.executeUpdate("DROP PROCEDURE IF EXISTS `_store_query`;");
        }
    }

}
