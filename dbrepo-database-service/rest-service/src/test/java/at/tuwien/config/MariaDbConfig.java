package at.tuwien.config;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.mapper.DatabaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Configuration
public class MariaDbConfig {

    private final DatabaseMapper databaseMapper;

    @Autowired
    public MariaDbConfig(DatabaseMapper databaseMapper) {
        this.databaseMapper = databaseMapper;
    }

    /**
     * Inserts a query into a created database with given hostname and database name. The method uses the JDBC in-out
     * notation <a href="#{@link}">{@link https://learn.microsoft.com/en-us/sql/connect/jdbc/using-sql-escape-sequences?view=sql-server-ver16#stored-procedure-calls}</a>
     *
     * @param hostname The hostname.
     * @param database The database name.
     * @param query    The query.
     * @param username The connection username.
     * @param password The connection password.
     * @return The generated or retrieved query id.
     * @throws SQLException The procedure did not succeed.
     */
    public static Long mockSystemQueryInsert(String hostname, String database, String query, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
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

    public static void dropDatabase(String hostname, String database, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, username, password)) {
            final String sql = "DROP DATABASE IF EXISTS `" + database + "`;";
            log.trace("prepare statement '{}'", sql);
            final PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
            statement.close();
        }
    }

    public void mockGrantUserPermissions(String hostname, Database database, User user) throws SQLException,
            QueryMalformedException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database.getInternalName();
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, "root", "mariadb")) {
            final DatabaseGiveAccessDto access = DatabaseGiveAccessDto.builder()
                    .username(user.getUsername())
                    .type(AccessTypeDto.WRITE_ALL)
                    .build();
            final PreparedStatement statement1 = databaseMapper.rawGrantUserAccessQuery(connection, access);
            statement1.executeUpdate();
            final PreparedStatement statement2 = databaseMapper.rawGrantUserProcedure(connection, user);
            statement2.executeUpdate();
            final PreparedStatement statement3 = databaseMapper.rawFlushPrivileges(connection);
            statement3.executeUpdate();
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
     * @param hostname The hostname.
     * @param database The database name.
     * @param query    The query.
     * @param username The connection username.
     * @param password The connection password.
     * @return The generated or retrieved query id.
     * @throws SQLException The procedure did not succeed.
     */
    public static Long mockUserQueryInsert(String hostname, String database, String query, String username, String password)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
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
     * @param hostname The hostname.
     * @param database The database name.
     * @param query    The query.
     * @return The generated or retrieved query id.
     * @throws SQLException The procedure did not succeed.
     */
    public static Long mockSystemQueryInsert(String hostname, String database, String query) throws SQLException {
        return mockSystemQueryInsert(hostname, database, query, "root", "mariadb");
    }
}
