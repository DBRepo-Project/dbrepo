package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mapstruct.factory.Mappers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "REPLICA_SQL_TEST_PORT", matches = "[0-9]+")
class HistoricalSubsetIntegrationTest extends BaseTest {
    private final String url = "jdbc:mariadb://127.0.0.1:" + System.getenv("REPLICA_SQL_TEST_PORT");
    private final String password = System.getenv("REPLICA_SQL_TEST_PASSWORD");
    private final MariaDbMapper mapper = Mappers.getMapper(MariaDbMapper.class);

    @BeforeEach
    void schema() throws Exception {
        DATABASE_1_CACHE.setInternalName("historical_subset_test");
        try (Connection connection = DriverManager.getConnection(url, "root", password)) {
            connection.createStatement().execute("DROP DATABASE IF EXISTS historical_subset_test");
            connection.createStatement().execute("CREATE DATABASE historical_subset_test");
            connection.setCatalog("historical_subset_test");
            connection.createStatement().execute("CREATE TABLE weather_aus (id INT PRIMARY KEY, value INT) WITH SYSTEM VERSIONING");
            connection.createStatement().execute("CREATE TABLE sensor (id INT PRIMARY KEY, linie VARCHAR(64)) WITH SYSTEM VERSIONING");
            connection.createStatement().execute("INSERT INTO weather_aus VALUES (1, 10)");
            connection.createStatement().execute("INSERT INTO sensor VALUES (1, 'original')");
        }
    }

    @Test
    void joinedAndCommaSeparatedRelationsReplayTheirHistoricalVersions() throws Exception {
        try (Connection connection = DriverManager.getConnection(url + "/historical_subset_test", "root", password)) {
            final Instant selection;
            try (var time = connection.createStatement().executeQuery("SELECT UNIX_TIMESTAMP(NOW(6))")) {
                assertTrue(time.next());
                final var value = time.getBigDecimal(1);
                selection = Instant.ofEpochSecond(value.longValue(), value.remainder(java.math.BigDecimal.ONE)
                        .movePointRight(9).longValue());
            }
            connection.createStatement().execute("UPDATE sensor SET linie = 'changed' WHERE id=1");
            connection.createStatement().execute("DELETE FROM weather_aus WHERE id=1");
            final var request = SubsetDto.builder().datasourceIds(Set.of(TABLE_1_ID))
                    .columns(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build(),
                            SubsetColumnDto.builder().id(COLUMN_3_2_ID).build()))
                    .joins(Set.of(JoinDto.builder().datasourceId(TABLE_3_ID)
                            .conditionals(Set.of(ConditionalDto.builder().columnId(COLUMN_1_1_ID)
                                    .foreignColumnId(COLUMN_3_1_ID).build())).build())).build();
            assertHistoricalRow(connection, mapper.subsetDtoToNormalizedTimestampedQuery(
                    DSL.using(SQLDialect.MARIADB), DATABASE_1_CACHE, request, selection));
            request.setJoins(null);
            request.setDatasourceIds(Set.of(TABLE_1_ID, TABLE_3_ID));
            assertHistoricalRow(connection, mapper.subsetDtoToNormalizedTimestampedQuery(
                    DSL.using(SQLDialect.MARIADB), DATABASE_1_CACHE, request, selection));
            connection.createStatement().execute("CREATE VIEW combined AS SELECT sensor.linie FROM sensor JOIN weather_aus USING(id)");
            assertHistoricalRow(connection, "SELECT * FROM combined FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + MariaDbMapper.mariaDbFormatter.format(selection) + "'");
        }
    }

    private void assertHistoricalRow(Connection connection, String query) throws Exception {
        try (var rows = connection.createStatement().executeQuery(query)) {
            assertTrue(rows.next(), query);
            assertEquals("original", rows.getString("linie"));
            assertFalse(rows.next());
        }
    }
}
