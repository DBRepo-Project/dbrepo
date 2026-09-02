package at.ac.tuwien.ifs.dbrepo.metadata;

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

    @Query("select t from Table t join t.replicaUrls r where r.replicaTableId = :replicaTableId")
    Optional<Table> findByReplicaTableId(@Param("replicaTableId") UUID replicaTableId);

}
