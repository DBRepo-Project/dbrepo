package at.ac.tuwien.ifs.dbrepo.service.outbox;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TupleReplicationOutboxServiceMariaDbImplUnitTest {

    @Test
    public void enqueueShouldPersistClaimAndDeleteOnSuccess() throws Exception {
        final TestTupleReplicationOutboxService service = service("success");
        final Database database = database();
        final Table table = table();

        final TupleReplicationOutboxEntry entry = service.enqueue(database, table, HttpMethod.POST,
                DataReplicationDto.builder().build());

        final List<TupleReplicationOutboxEntry> claimed = service.claimDue(database, 10, Duration.ofMinutes(5));
        assertEquals(1, claimed.size());
        assertEquals(entry.getId(), claimed.get(0).getId());
        assertEquals(TupleReplicationOutboxStatus.PROCESSING, claimed.get(0).getStatus());

        assertTrue(service.claim(database, entry.getId(), Duration.ofMinutes(5)).isEmpty());

        service.markSucceeded(database, entry.getId());

        assertTrue(service.claimDue(database, 10, Duration.ZERO).isEmpty());
        assertEquals(0, service.countRows(database));
    }

    @Test
    public void markFailedShouldDelayRetryAndStopAtMaxAttempts() throws Exception {
        final TestTupleReplicationOutboxService service = service("failure");
        final Database database = database();
        final Table table = table();

        final TupleReplicationOutboxEntry entry = service.enqueue(database, table, HttpMethod.PUT,
                DataReplicationDto.builder().build());
        assertTrue(service.claim(database, entry.getId(), Duration.ofMinutes(5)).isPresent());

        service.markFailed(database, entry.getId(), "replication unavailable", Duration.ofMinutes(5), 2);

        assertTrue(service.claimDue(database, 10, Duration.ofMinutes(5)).isEmpty());
        assertEquals(TupleReplicationOutboxStatus.PENDING.name(), service.status(database, entry.getId()));

        service.forceDue(database, entry.getId());
        assertTrue(service.claim(database, entry.getId(), Duration.ofMinutes(5)).isPresent());
        service.markFailed(database, entry.getId(), "replication unavailable", Duration.ofMinutes(5), 2);

        assertEquals(TupleReplicationOutboxStatus.FAILED.name(), service.status(database, entry.getId()));
        assertTrue(service.claimDue(database, 10, Duration.ZERO).isEmpty());
    }

    private TestTupleReplicationOutboxService service(String name) {
        return new TestTupleReplicationOutboxService(new ObjectMapper().findAndRegisterModules(),
                "jdbc:h2:mem:tuple_outbox_" + name + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }

    private Database database() {
        return Database.builder()
                .id(UUID.randomUUID())
                .internalName("test_database")
                .build();
    }

    private Table table() {
        return Table.builder()
                .id(UUID.randomUUID())
                .internalName("test_table")
                .build();
    }

    private static class TestTupleReplicationOutboxService extends TupleReplicationOutboxServiceMariaDbImpl {

        private final String jdbcUrl;

        TestTupleReplicationOutboxService(ObjectMapper objectMapper, String jdbcUrl) {
            super(objectMapper);
            this.jdbcUrl = jdbcUrl;
        }

        @Override
        public ComboPooledDataSource getDataSource(Database database) {
            try {
                final ComboPooledDataSource dataSource = new ComboPooledDataSource();
                dataSource.setDriverClass("org.h2.Driver");
                dataSource.setJdbcUrl(jdbcUrl);
                dataSource.setUser("sa");
                dataSource.setPassword("");
                dataSource.setInitialPoolSize(1);
                dataSource.setMinPoolSize(1);
                dataSource.setMaxPoolSize(2);
                return dataSource;
            } catch (PropertyVetoException e) {
                throw new IllegalStateException(e);
            }
        }

        int countRows(Database database) throws Exception {
            try (ComboPooledDataSource dataSource = getDataSource(database);
                 Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM tuple_replication_notification_outbox")) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }

        String status(Database database, UUID id) throws Exception {
            try (ComboPooledDataSource dataSource = getDataSource(database);
                 Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT status FROM tuple_replication_notification_outbox "
                         + "WHERE id = '" + id + "'")) {
                resultSet.next();
                return resultSet.getString(1);
            }
        }

        void forceDue(Database database, UUID id) throws Exception {
            try (ComboPooledDataSource dataSource = getDataSource(database);
                 Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE tuple_replication_notification_outbox SET next_attempt_at = '"
                        + Instant.now().minus(Duration.ofMinutes(1)) + "' WHERE id = '" + id + "'");
            }
        }
    }
}
