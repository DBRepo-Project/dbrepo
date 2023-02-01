package at.tuwien.config;

import at.tuwien.querystore.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class MariaDbConfig {

    public static void insertQueryStore(String hostname, String database, Query query, String username) throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
        log.trace("connect to database {}", jdbc);
        final Connection connection = DriverManager.getConnection(jdbc, "root", "mariadb");
        final Statement statement = connection.createStatement();
        statement.execute("INSERT INTO qs_queries (created_by, query, query_normalized, is_persisted, query_hash, result_hash, result_number) " +
                "VALUES ('" + username + "', '" + query.getQuery() + "', '" + query.getQuery() + "', true, '" + query.getQueryHash() + "', '" + query.getResultHash() + "', " + query.getResultNumber() + ")");
        connection.close();
    }

    public static List<Map<String, String>> selectQuery(String hostname, String database, String query, String... columns)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
        log.trace("connect to database {}", jdbc);
        final List<Map<String, String>> rows = new LinkedList<>();
        try (Connection connection = DriverManager.getConnection(jdbc, "root", "mariadb")) {
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

    public static void execute(String hostname, String database, String query)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, "root", "mariadb")) {
            final Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        }
    }
}
