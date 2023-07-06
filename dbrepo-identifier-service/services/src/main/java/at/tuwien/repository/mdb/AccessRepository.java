
package at.tuwien.repository.mdb;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.DatabaseAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessRepository extends JpaRepository<DatabaseAccess, DatabaseAccessKey> {

    /**
     * Finds database access by given database id and user id.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return Non-empty optional if this database access exists, empty optional otherwise.
     */
    Optional<DatabaseAccess> findByHdbidAndHuserid(Long databaseId, UUID userId);

}
