package at.tuwien.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.sql.*;

@Slf4j
@Configuration
public class MariaDbConfig {

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
            final String call = "{call _store_query(?,?,?)}";
            log.trace("prepare procedure '{}'", call);
            final CallableStatement statement = connection.prepareCall(call);
            statement.setString("_username", username);
            statement.setString("query", query);
            statement.registerOutParameter("queryId", Types.BIGINT);
            statement.executeUpdate();
            final Long queryId = statement.getLong("queryId");
            statement.close();
            log.debug("received queryId={}", queryId);
            return queryId;
        }
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
            final String call = "{call store_query(?,?)}";
            log.trace("prepare procedure '{}'", call);
            final CallableStatement statement = connection.prepareCall(call);
            statement.setString(1, query);
            statement.registerOutParameter(2, Types.BIGINT);
            statement.executeUpdate();
            final Long queryId = statement.getLong(2);
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
