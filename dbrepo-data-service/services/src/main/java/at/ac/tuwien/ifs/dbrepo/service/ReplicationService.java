package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface ReplicationService {

    void replicateTuple(TupleWithTimestampsDto tuple, Database database, Table table);

    void replicateTupleUpdate(TupleWithTimestampsDto tuple, Database database, Table table);

    void replicateTupleDelete(TupleWithTimestampsDto tuple, Database database, Table table);

    List<TupleReplicationOutboxEntry> findOutboxEntries(Database database) throws SQLException;

    int retryDueOutboxEntries(Database database);

    boolean retryOutboxEntry(Database database, UUID id);
}
