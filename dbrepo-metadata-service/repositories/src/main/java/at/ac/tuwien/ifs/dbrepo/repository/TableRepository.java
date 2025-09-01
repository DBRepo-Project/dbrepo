package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * TO BE USED READONLY
 */
@Repository
public interface TableRepository extends JpaRepository<Table, UUID> {

    /**
     * Find a table by its replica table ID from the mdb_tables_replica_urls table.
     *
     * @param replicaTableId The replica table ID to search for
     * @return Optional containing the table if found
     */
    @Query("SELECT t FROM Table t JOIN t.replicaUrls r WHERE r.replicaTableId = :replicaTableId")
    Optional<Table> findByReplicaTableId(@Param("replicaTableId") UUID replicaTableId);
}
