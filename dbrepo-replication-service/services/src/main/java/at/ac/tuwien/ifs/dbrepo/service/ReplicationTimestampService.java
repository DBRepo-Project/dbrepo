package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;

import java.util.List;
import java.util.UUID;

public interface ReplicationTimestampService {

    /**
     * Save a replication timestamp to a specific database
     */
    void saveReplicationTimestamp(DatabaseDto database, TupleReplicationTimestamp timestamp);

    /**
     * Save multiple replication timestamps to a specific database
     */
    void saveReplicationTimestamps(DatabaseDto database, List<TupleReplicationTimestamp> timestamps);

    /**
     * Update row_end for existing replication timestamps (no inserts).
     */
    void updateReplicationTimestampsRowEnd(DatabaseDto database, List<TupleReplicationTimestamp> timestamps);

    /**
     * Find all timestamps for a specific database and table
     */
    List<TupleReplicationTimestamp> findByDatabaseIdAndTableId(DatabaseDto database, UUID databaseId, UUID tableId);

    /**
     * Find all timestamps for a specific site URL in a specific database
     */
    List<TupleReplicationTimestamp> findBySiteUrl(DatabaseDto database, String siteUrl);

    /**
     * Find all timestamps for a specific replication ID in a specific database
     */
    List<TupleReplicationTimestamp> findByReplicationId(DatabaseDto database, String replicationId);

    /**
     * Find timestamps within a specific time range in a specific database
     */
    List<TupleReplicationTimestamp> findByTimeRange(DatabaseDto database, java.time.Instant startTime, java.time.Instant endTime);

    /**
     * Find active timestamps (where row_end is null) for a specific database and table
     */
    List<TupleReplicationTimestamp> findActiveTimestamps(DatabaseDto database, UUID databaseId, UUID tableId);

    /**
     * Find timestamps for a specific site, database, and table combination in a specific database
     */
    List<TupleReplicationTimestamp> findBySiteUrlAndDatabaseIdAndTableId(DatabaseDto database, String siteUrl, UUID databaseId, UUID tableId);

    /**
     * Ensure the tuple_replication_timestamps table exists in the specified database
     */
    void ensureTableExists(DatabaseDto database);

    /**
     * Get the latest replication timestamp from the tuple_replication_timestamps table.
     * This method returns the most recent timestamp when the service received updates.
     * 
     * @param database The database to query
     * @return The latest timestamp, or null if no timestamps exist
     */
    java.time.Instant getLatestReplicationTimestamp(DatabaseDto database);
}
