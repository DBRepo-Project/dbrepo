package at.tuwien.config;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
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
    public static Long mockSystemQueryInsert(Database database, String query, String username, String password)
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

    public static Map<String, List<Object>> describeTableSchema(Table table, String username, String password)
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

    public static void createDatabase(Container container, String database) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getPrivilegedUsername(), container.getPrivilegedPassword())) {
            final String sql = "CREATE DATABASE `" + database + "`;";
            log.trace("prepare statement '{}'", sql);
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        }
    }

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

    public static void dropDatabase(Container container, String database)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + container.getHost() + ":" + container.getPort();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, container.getPrivilegedUsername(), container.getPrivilegedPassword())) {
            final String sql = "DROP DATABASE IF EXISTS `" + database + "`;";
            log.trace("prepare statement '{}'", sql);
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        }
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

    public static String getPrivileges(String hostname, String database, String user, String username, String password)
            throws Exception {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
        log.trace("connect to database {}", jdbc);
        final List<String> usernames = new LinkedList<>();
        try (Connection connection = DriverManager.getConnection(jdbc, username, password)) {
            final String query = "SHOW GRANTS FOR `" + user + "`;";
            log.trace("prepare statement '{}'", query);
            final PreparedStatement statement = connection.prepareStatement(query);
            final ResultSet set = statement.executeQuery();
            statement.close();
            if (set.next()) {
                return set.getString(1);
            }
        }
        throw new Exception("Failed to get privileges");
    }

    public static void mockQuery(String hostname, String query, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname;
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
    public static Long mockUserQueryInsert(Database database, String query, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + database.getContainer().getHost() + ":" + database.getContainer().getPort() + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
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
    public static Long mockSystemQueryInsert(Database database, String query) throws SQLException {
        return mockSystemQueryInsert(database, query, database.getContainer().getPrivilegedUsername(), database.getContainer().getPrivilegedPassword());
    }
}
