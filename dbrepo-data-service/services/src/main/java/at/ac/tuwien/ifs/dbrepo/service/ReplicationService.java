package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;
import org.springframework.http.HttpMethod;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ReplicationService {

    boolean isEnabled(Database database, Table table);

    void prepare(Connection connection, Database database, Table table) throws SQLException;

    void enqueue(Connection connection, TupleWithTimestampsDto tuple, Database database, Table table,
                 HttpMethod method) throws SQLException;

    List<TupleReplicationOutboxEntry> findOutboxEntries(Database database) throws SQLException;

    int retryDueOutboxEntries(Database database);

    boolean retryOutboxEntry(Database database, UUID id);
}
