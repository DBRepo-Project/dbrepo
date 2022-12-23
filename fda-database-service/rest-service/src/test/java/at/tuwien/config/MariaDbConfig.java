package at.tuwien.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.sql.*;

@Slf4j
@Configuration
public class MariaDbConfig {

    public static Long mockQueryInsert(String hostname, String database, String query)
            throws SQLException {
        final String jdbc = "jdbc:mariadb://" + hostname + "/" + database;
        log.trace("connect to database {}", jdbc);
        try (Connection connection = DriverManager.getConnection(jdbc, "root", "mariadb")) {
            final String call = "{ call store_query(?, ?) }";
            log.trace("prepare procedure '{}'", call);
            final CallableStatement statement = connection.prepareCall(call);
            statement.setString(1, query);
            statement.registerOutParameter(2, Types.BIGINT);
            final ResultSet result = statement.executeQuery(call);
            while (result.next()) {
                final Long id = result.getLong(1);
                log.trace("got id {}", id);
                return id;
            }
        }
        return null;
    }
}
