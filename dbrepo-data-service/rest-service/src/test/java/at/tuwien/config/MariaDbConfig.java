package at.tuwien.config;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Configuration
public class MariaDbConfig {

    /**
     * Inserts a query into a created database with given hostname and database name. The method uses the JDBC in-out
     * notation <a href="#{@link}">{@link https://learn.microsoft.com/en-us/sql/connect/jdbc/using-sql-escape-sequences?view=sql-server-ver16#stored-procedure-calls}</a>
     *
     * @param database The database.
     * @param query    The query.
     * @param username The connection username.
     * @param password The connection password.
     * @return The generated or retrieved query id.
     * @throws SQLException The procedure did not succeed.
     */
    public static Long mockSystemQueryInsert(PrivilegedDatabaseDto database, String query, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, username, password)) {
            final String call = "{call _store_query(?,?,?,?)}";
            log.trace("prepare procedure '{}'", call);
            final CallableStatement statement = connection.prepareCall(call);
            statement.setString(1, username);
            statement.setString(2, query);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.registerOutParameter(4, Types.BIGINT);
            statement.executeUpdate();
            final Long queryId = statement.getLong(4);
            statement.close();
            log.debug("received queryId={}", queryId);
            return queryId;
        }
    }

    public static void createDatabase(PrivilegedContainerDto container, String database) throws SQLException {
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

    public static void createInitDatabase(PrivilegedContainerDto container, DatabaseDto database) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getUsername(), container.getPassword())) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("init/" + database.getInternalName() + ".sql"), new ClassPathResource("init/users.sql"), new ClassPathResource("init/querystore.sql"));
            populator.setSeparator(";\n");
            populator.populate(connection);
        }
        log.debug("created init database {}", database.getInternalName());
    }

    public static void grantReadAccess(PrivilegedDatabaseDto database, String username) {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            connection.prepareStatement("GRANT SELECT ON *.* TO `" + username + "`@`%`;")
                    .executeUpdate();
            connection.prepareStatement("FLUSH PRIVILEGES;")
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("could not grant read access", e);
        }
        log.debug("granted read access to user {} in database {}", username, database.getInternalName());
    }

    public static void grantWriteAccess(PrivilegedDatabaseDto database, String username) {
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

    public static void dropAllDatabases(PrivilegedContainerDto container) {
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

    public static void dropDatabase(PrivilegedContainerDto container, String database)
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

    public static List<String> getUsernames(String hostname, String database, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
        log.trace("connect to database {}", jdbc);
        final List<String> usernames = new LinkedList<>();
        try (Connection connection = DriverManager.getConnection(jdbc, username, password)) {
            final String query = "SELECT User FROM mysql.user;";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            final ResultSet set = statement.executeQuery();
            statement.close();
            while (set.next()) {
                usernames.add(set.getString("User"));
            }
            log.debug("received usernames={}", usernames);
            return usernames;
        }
    }

    public static List<String> getPrivileges(PrivilegedDatabaseDto database, String username) throws SQLException {
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

    public static void dropTable(PrivilegedDatabaseDto database, String table) throws SQLException {
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

    /**
     * Inserts a query into a created database with given hostname and database name. The method uses the JDBC in-out
     * notation <a href="#{@link}">{@link https://learn.microsoft.com/en-us/sql/connect/jdbc/using-sql-escape-sequences?view=sql-server-ver16#stored-procedure-calls}</a>
     *
     * @param database The database.
     * @param query    The query.
     * @param username The connection username.
     * @param password The connection password.
     * @return The generated or retrieved query id.
     * @throws SQLException The procedure did not succeed.
     */
    public static Long mockUserQueryInsert(PrivilegedDatabaseDto database, String query, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database: {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, username, password)) {
            final String call = "{call store_query(?,?,?)}";
            log.trace("prepare procedure '{}'", call);
            final CallableStatement statement = connection.prepareCall(call);
            statement.setString(1, query);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.registerOutParameter(3, Types.BIGINT);
            statement.executeUpdate();
            final Long queryId = statement.getLong(3);
            statement.close();
            log.debug("received queryId={}", queryId);
            return queryId;
        }
    }

    /**
     * Inserts a query into a created database with given hostname and database name. The method uses the JDBC in-out
     * notation <a href="#{@link}">{@link https://learn.microsoft.com/en-us/sql/connect/jdbc/using-sql-escape-sequences?view=sql-server-ver16#stored-procedure-calls}</a>
     *
     * @param database The database.
     * @param query    The query.
     * @return The generated or retrieved query id.
     * @throws SQLException The procedure did not succeed.
     */
    public static Long mockSystemQueryInsert(PrivilegedDatabaseDto database, String query) throws SQLException {
        return mockSystemQueryInsert(database, query, database.getContainer().getUsername(), database.getContainer().getPassword());
    }

    public static void insertQueryStore(PrivilegedDatabaseDto database, QueryDto query, UUID userId) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database: {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final PreparedStatement prepareStatement = connection.prepareStatement(
                    "INSERT INTO qs_queries (created_by, query, query_normalized, is_persisted, query_hash, result_hash, result_number, created, executed) VALUES (?,?,?,?,?,?,?,?,?)");
            prepareStatement.setString(1, String.valueOf(userId));
            prepareStatement.setString(2, query.getQuery());
            prepareStatement.setString(3, query.getQuery());
            prepareStatement.setBoolean(4, query.getIsPersisted());
            prepareStatement.setString(5, query.getQueryHash());
            prepareStatement.setString(6, query.getResultHash());
            prepareStatement.setLong(7, query.getResultNumber());
            prepareStatement.setTimestamp(8, Timestamp.from(query.getCreated()));
            prepareStatement.setTimestamp(9, Timestamp.from(query.getExecution()));
            log.trace("prepared statement: {}", prepareStatement);
            prepareStatement.executeUpdate();
        }
    }

    public static List<Map<String, Object>> listQueryStore(PrivilegedDatabaseDto database) throws SQLException {
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

    public static List<Map<String, String>> selectQuery(PrivilegedDatabaseDto database, String query, Set<String> columns)
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

    public static List<Map<String, byte[]>> selectQueryByteArr(PrivilegedDatabaseDto database, String query, Set<String> columns)
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

    public static void execute(PrivilegedDatabaseDto database, String query)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        }
    }

    public static void execute(PrivilegedContainerDto container, String query)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database: {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getUsername(), container.getPassword())) {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        }
    }

    public static void dropQueryStore(PrivilegedDatabaseDto database)
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

    public static Map<String, List<Object>> describeTableSchema(PrivilegedTableDto table, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + table.getDatabase().getContainer().getHost() + ":" + table.getDatabase().getContainer().getPort() + "/" + table.getDatabase().getInternalName();
        log.trace("connect to database {}", jdbc);
        final Map<String, List<Object>> out = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(jdbc, username, password)) {
            final String query = "SHOW COLUMNS FROM `" + table.getInternalName() + "`;";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            final ResultSet resultSet = statement.executeQuery();
            statement.close();
            while (resultSet.next()) {
                if (resultSet.getString("Field").equals("id")) {
                    continue;
                }
                out.put(resultSet.getString("Field"), List.of(resultSet.getString("Type"), resultSet.getString("Null"), resultSet.getString("Key")));
            }
            return out;
        }
    }

    public static ColumnTypeDto typetoColumnTypeDto(String data) throws Exception {
        if (data.toUpperCase().startsWith("TINYINT(1)")) {
            /* boolean in MySQL */
            return ColumnTypeDto.BOOL;
        }
        final Matcher matcher = Pattern.compile("([A-Z]+)")
                .matcher(data.toUpperCase());
        if (!matcher.find()) {
            log.error("Failed to map type: does not match expected format");
            throw new Exception("Failed to map type: does not match expected format");
        }
        final String type = matcher.group(1);
        try {
            return ColumnTypeDto.valueOf(type);
        } catch (IllegalArgumentException e) {
            if (type.startsWith("TINYINT")) {
                /* boolean in MySQL */
                return ColumnTypeDto.BOOL;
            } else if (type.startsWith("BOOL")) {
                /* boolean */
                return ColumnTypeDto.BOOL;
            } else if (type.startsWith("DOUBLE")) {
                /* double precision */
                return ColumnTypeDto.DOUBLE;
            } else if (type.startsWith("INT")) {
                /* integer synonym */
                return ColumnTypeDto.INT;
            } else if (type.startsWith("DEC")) {
                /* decimal synonym */
                return ColumnTypeDto.DECIMAL;
            } else if (type.startsWith("ENUM")) {
                return ColumnTypeDto.ENUM;
            } else if (type.startsWith("SET")) {
                return ColumnTypeDto.SET;
            }
        }
        log.error("Failed to map data {} and type {}", data, type);
        throw new Exception("Failed to map data " + data + " and type " + type);
    }

    public static boolean tableExists(PrivilegedDatabaseDto database, String tableName)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final Statement statement = connection.createStatement();
            final String query = "SHOW TABLES LIKE '" + tableName + "';";
            log.trace("execute query {}", query);
            final ResultSet result = statement.executeQuery(query);
            return result.next();
        }
    }

}
