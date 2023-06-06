
package at.tuwien.repository.mdb;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.DatabaseAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessRepository extends JpaRepository<DatabaseAccess, DatabaseAccessKey> {

    Optional<DatabaseAccess> findByHdbidAndHuserid(Long databaseId, UUID userId);

}
