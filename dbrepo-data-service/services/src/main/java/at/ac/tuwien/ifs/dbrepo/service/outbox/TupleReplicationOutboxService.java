package at.ac.tuwien.ifs.dbrepo.service.outbox;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import org.springframework.http.HttpMethod;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TupleReplicationOutboxService {

    TupleReplicationOutboxEntry enqueue(Database database, Table table, HttpMethod method, DataReplicationDto payload)
            throws SQLException;

    List<TupleReplicationOutboxEntry> findAll(Database database) throws SQLException;

    Optional<TupleReplicationOutboxEntry> claim(Database database, UUID id, Duration processingTimeout)
            throws SQLException;

    List<TupleReplicationOutboxEntry> claimDue(Database database, int limit, Duration processingTimeout)
            throws SQLException;

    void markSucceeded(Database database, UUID id) throws SQLException;

    void markFailed(Database database, UUID id, String error, Duration retryDelay, int maxAttempts)
            throws SQLException;
}
