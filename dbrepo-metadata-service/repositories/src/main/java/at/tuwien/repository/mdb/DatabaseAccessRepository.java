package at.tuwien.repository.mdb;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.DatabaseAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseAccessRepository extends JpaRepository<DatabaseAccess, DatabaseAccessKey> {

    void deleteByHdbidAndHuserid(Long databaseId, UUID userId);

    /**
     * Finds database access by given database id and user id.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return Non-empty optional if this database access exists, empty optional otherwise.
     */
    Optional<DatabaseAccess> findByHdbidAndHuserid(Long databaseId, UUID userId);

    List<DatabaseAccess> findByHdbid(Long databaseId);

    @Query("select a from DatabaseAccess a where a.hdbid = :databaseId and a.huserid = :userId")
    Optional<DatabaseAccess> findByDatabaseIdAndUserId(Long databaseId, UUID userId);

}
