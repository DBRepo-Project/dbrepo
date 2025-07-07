package at.ac.tuwien.ifs.dbrepo.utils;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
public class MariaDbUtil {

    /**
     * https://mariadb.com/kb/en/string-data-types/
     */
    final public static List<ColumnTypeDto> stringDataTypes = List.of(
            ColumnTypeDto.BINARY,
            ColumnTypeDto.VARBINARY,
            ColumnTypeDto.TINYBLOB,
            ColumnTypeDto.MEDIUMBLOB,
            ColumnTypeDto.LONGBLOB,
            ColumnTypeDto.BLOB,
            ColumnTypeDto.CHAR,
            ColumnTypeDto.VARCHAR,
            ColumnTypeDto.ENUM,
            ColumnTypeDto.SET,
            ColumnTypeDto.TINYTEXT,
            ColumnTypeDto.MEDIUMTEXT,
            ColumnTypeDto.LONGTEXT,
            ColumnTypeDto.TEXT);

    /**
     * https://mariadb.com/kb/en/numeric-data-type-overview/
     */
    final public static List<ColumnTypeDto> numericDataTypes = List.of(
            ColumnTypeDto.TINYINT,
            ColumnTypeDto.BOOL,
            ColumnTypeDto.SMALLINT,
            ColumnTypeDto.MEDIUMINT,
            ColumnTypeDto.INT,
            ColumnTypeDto.BIGINT,
            ColumnTypeDto.DECIMAL,
            ColumnTypeDto.FLOAT,
            ColumnTypeDto.DOUBLE,
            ColumnTypeDto.BIT);

    /**
     * https://mariadb.com/kb/en/date-and-time-data-types/
     */
    final static List<ColumnTypeDto> dateDataTypes = List.of(ColumnTypeDto.DATE,
            ColumnTypeDto.DATETIME,
            ColumnTypeDto.TIME,
            ColumnTypeDto.TIMESTAMP,
            ColumnTypeDto.YEAR);

    public static boolean needValueQuotes(ColumnTypeDto columnType) {
        return stringDataTypes.contains(columnType) || dateDataTypes.contains(columnType);
    }


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

    public static void revokeAccess(DatabaseDto database, String username) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            connection.prepareStatement("REVOKE ALL PRIVILEGES, GRANT OPTION FROM `" + username + "`@`%`;")
                    .executeUpdate();
            connection.prepareStatement("FLUSH PRIVILEGES;")
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to revoke access", e);
            throw new SQLException("Failed to revoke access", e);
        }
        log.debug("revoked access from user {} in database {}", username, database.getInternalName());
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

    public static void grantAccess(DatabaseDto database, String grants, String username) {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            connection.prepareStatement("GRANT " + grants + " ON `" + database.getInternalName() + "`.* TO `" + username + "`@`%`;")
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

    public static Set<String> getPrivileges(DatabaseDto database, String username) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final String query = "SHOW GRANTS FOR ?@`%`;";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            final ResultSet resultSet = statement.executeQuery();
            statement.close();
            final Set<String> privileges = new HashSet<>();
            while (resultSet.next()) {
                final Matcher matcher = Pattern.compile("GRANT (.*) ON.*").matcher(resultSet.getString(1));
                if (matcher.find()) {
                    privileges.addAll(Arrays.stream(matcher.group(1).split(",")).map(String::trim).toList());
                    log.trace("found privileges: {}", privileges);
                }
            }
            privileges.remove("USAGE");
            return privileges;
        }
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

    public static boolean tableExists(DatabaseDto database, String table) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final String query = "SELECT 1 FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA = '" + database.getInternalName() + "' AND t.TABLE_NAME = '" + table + "';";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                statement.close();
                return true;
            }
            statement.close();
        }
        return false;
    }

    public static String tableDescription(DatabaseDto database, String table) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, database.getContainer().getUsername(), database.getContainer().getPassword())) {
            final String query = "SELECT t.TABLE_COMMENT FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA = '" + database.getInternalName() + "' AND t.TABLE_NAME = '" + table + "';";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            final ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                final String comment = resultSet.getString(1);
                statement.close();
                return comment;
            }
            statement.close();
        }
        throw new SQLException("Failed to get ResultSet");
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
            statement.execute(query);
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
