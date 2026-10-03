package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationJournalDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ReplicationJournalService extends DataConnector {
    private final TupleReplicationOutboxServiceMariaDbImpl journal;
    private final ObjectMapper json;

    public ReplicationJournalDto read(Database database, long after, Long requestedThrough, int limit) throws SQLException {
        if (after < 0 || limit < 1 || limit > 1000 || (requestedThrough != null && requestedThrough < after)) {
            throw new IllegalArgumentException("Invalid replication journal range");
        }
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection()) {
            journal.ensureTableExists(connection);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                final var state = journal.readJournalState(connection);
                final long through = requestedThrough == null ? state.committedThrough() : requestedThrough;
                if (through > state.committedThrough() || after > through) {
                    throw new IllegalArgumentException("Requested journal boundary has not committed");
                }
                final var entries = journal.readRange(connection, after, through, limit);
                final var events = new ArrayList<ReplicationJournalDto.Event>();
                for (var entry : entries) {
                    events.add(new ReplicationJournalDto.Event(entry.method().name(),
                            json.readValue(entry.payloadJson(), DataReplicationDto.class)));
                }
                final long next = entries.isEmpty() ? through : entries.getLast().sequence();
                connection.commit();
                return new ReplicationJournalDto(through, state.legacyThrough(), next, events);
            } catch (JsonProcessingException failure) {
                connection.rollback();
                throw new SQLException("Stored source journal payload is invalid", failure);
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        } finally {
            pool.close();
        }
    }
}
