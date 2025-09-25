package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, UUID> {

    List<Database> findAllDesc();

    List<Database> findAllPublicOrSchemaPublicDesc();

    List<Database> findAllAtLestReadAccessDesc(String username);

    List<Database> findAllPublicOrSchemaPublicOrReadAccessDesc(String username);

    List<Database> findAllPublicOrSchemaPublicOrReadAccessByInternalNameDesc(String username, String internalName);

    List<Database> findAllPublicOrSchemaPublicByInternalNameDesc(String internalName);

    List<Database> findAllByInternalNameDesc(String internalName);

    /**
     * Find a database by its replica database ID from the mdb_databases_replica_urls table.
     *
     * @param replicaDatabaseId The replica database ID to search for
     * @return Optional containing the database if found
     */
    @Query("SELECT d FROM Database d JOIN d.replicaUrls r WHERE r.replicaDatabaseId = :replicaDatabaseId")
    Optional<Database> findByReplicaDatabaseId(@Param("replicaDatabaseId") UUID replicaDatabaseId);

}
