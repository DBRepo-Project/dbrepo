package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.entity.TupleReplicationTimestamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TupleReplicationTimestampRepository extends JpaRepository<TupleReplicationTimestamp, TupleReplicationTimestamp.TupleReplicationTimestampId> {

    /**
     * Find all timestamps for a specific database and table
     */
    List<TupleReplicationTimestamp> findByDatabaseIdAndTableId(UUID databaseId, UUID tableId);

    /**
     * Find all timestamps for a specific site URL
     */
    List<TupleReplicationTimestamp> findBySiteUrl(String siteUrl);

    /**
     * Find all timestamps for a specific replication ID
     */
    List<TupleReplicationTimestamp> findByReplicationId(String replicationId);

    /**
     * Find timestamps within a specific time range
     */
    @Query("SELECT t FROM TupleReplicationTimestamp t WHERE t.rowStart >= :startTime AND (t.rowEnd IS NULL OR t.rowEnd <= :endTime)")
    List<TupleReplicationTimestamp> findByTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * Find active timestamps (where row_end is null) for a specific database and table
     */
    @Query("SELECT t FROM TupleReplicationTimestamp t WHERE t.databaseId = :databaseId AND t.tableId = :tableId AND t.rowEnd IS NULL")
    List<TupleReplicationTimestamp> findActiveTimestamps(@Param("databaseId") UUID databaseId, @Param("tableId") UUID tableId);

    /**
     * Find timestamps for a specific site, database, and table combination
     */
    List<TupleReplicationTimestamp> findBySiteUrlAndDatabaseIdAndTableId(String siteUrl, UUID databaseId, UUID tableId);
}
