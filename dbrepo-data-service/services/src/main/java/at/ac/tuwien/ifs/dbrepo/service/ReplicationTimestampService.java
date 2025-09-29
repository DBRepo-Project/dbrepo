package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReplicationTimestampService {

    void saveReplicationTimestamp(DatabaseDto database, TupleReplicationTimestamp timestamp);

    void saveReplicationTimestamps(DatabaseDto database, List<TupleReplicationTimestamp> timestamps);

    void updateReplicationTimestampsRowEnd(DatabaseDto database, List<TupleReplicationTimestamp> timestamps);

    List<TupleReplicationTimestamp> findByDatabaseIdAndTableId(DatabaseDto database, UUID databaseId, UUID tableId);

    List<TupleReplicationTimestamp> findBySiteUrl(DatabaseDto database, String siteUrl);

    List<TupleReplicationTimestamp> findByReplicationId(DatabaseDto database, String replicationId);

    List<TupleReplicationTimestamp> findByTimeRange(DatabaseDto database, Instant startTime, Instant endTime);

    List<TupleReplicationTimestamp> findActiveTimestamps(DatabaseDto database, UUID databaseId, UUID tableId);

    List<TupleReplicationTimestamp> findBySiteUrlAndDatabaseIdAndTableId(DatabaseDto database, String siteUrl, UUID databaseId, UUID tableId);

    void ensureTableExists(DatabaseDto database);
}