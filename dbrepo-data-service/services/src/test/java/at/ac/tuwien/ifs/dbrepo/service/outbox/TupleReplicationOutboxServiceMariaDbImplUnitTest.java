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
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TupleReplicationOutboxServiceMariaDbImplUnitTest {

    @Test
    public void enqueueShouldRetainStableEventIdentityAndHistoryAfterSuccess() throws Exception {
        final TestTupleReplicationOutboxService service = service("success");
        final Database database = database();
        final Table table = table();

        final TupleReplicationOutboxEntry entry = service.enqueue(database, table, HttpMethod.POST,
                DataReplicationDto.builder().build());

        final List<TupleReplicationOutboxEntry> entries = service.findAll(database);
        assertEquals(1, entries.size());
        assertEquals(entry.getId(), entries.get(0).getId());
        assertEquals(TupleReplicationOutboxStatus.PENDING, entries.get(0).getStatus());

        final List<TupleReplicationOutboxEntry> claimed = service.claimDue(database, 10, Duration.ofMinutes(5));
        assertEquals(1, claimed.size());
        assertEquals(entry.getId(), claimed.get(0).getId());
        assertEquals(TupleReplicationOutboxStatus.PROCESSING, claimed.get(0).getStatus());

        assertTrue(service.claim(database, entry.getId(), Duration.ofMinutes(5)).isEmpty());

        service.markSucceeded(database, entry.getId());

        assertTrue(service.claimDue(database, 10, Duration.ZERO).isEmpty());
        assertEquals(1, service.countRows(database));
        final var retained = service.findAll(database).getFirst();
        assertEquals(TupleReplicationOutboxStatus.SUCCEEDED, retained.getStatus());
        final var payload = new ObjectMapper().readTree(retained.getPayloadJson());
        assertEquals(entry.getId().toString(), payload.get("eventId").asText());
        assertTrue(payload.get("eventSequence").asLong() > 0);
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
        final var manual = service.claim(database, entry.getId(), Duration.ofMinutes(5));
        assertTrue(manual.isPresent());
        assertEquals(2, manual.get().getAttempts());
        assertTrue(service.claim(database, entry.getId(), Duration.ofMinutes(5)).isEmpty());
        service.markSucceeded(database, entry.getId());
        assertEquals(1, service.countRows(database));
    }

    @Test
    public void rollbackReusesSequenceWithoutMutatingCallerPayload() throws Exception {
        final var service = service("rollback");
        final var database = database();
        final var table = table();
        final var payload = DataReplicationDto.builder().build();
        try (var dataSource = service.getDataSource(database); var connection = dataSource.getConnection()) {
            service.ensureTableExists(connection);
            assertThrows(SQLException.class, () -> service.enqueue(connection, database, table, HttpMethod.POST, payload));
            connection.setAutoCommit(false);
            service.enqueue(connection, database, table, HttpMethod.POST, payload);
            assertNull(payload.getEventId());
            assertNull(payload.getEventSequence());
            connection.rollback();
            assertEquals(0, service.readJournalState(connection).committedThrough());
            service.enqueue(connection, database, table, HttpMethod.POST, payload);
            connection.commit();
            assertEquals(1, service.readJournalState(connection).committedThrough());
            final var event = service.readRange(connection, 0, 1, 10).getFirst();
            assertEquals(1, event.sequence());
            assertEquals(event.eventId().toString(), new ObjectMapper().readTree(event.payloadJson()).get("eventId").asText());
        }
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
