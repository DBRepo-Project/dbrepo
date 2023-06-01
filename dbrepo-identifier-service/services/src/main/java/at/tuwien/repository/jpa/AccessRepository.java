
package at.tuwien.repository.jpa;

import at.tuwien.entities.database.DatabaseAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessRepository extends JpaRepository<DatabaseAccess, Long> {

    Optional<DatabaseAccess> findByHdbidAndHuserid(Long databaseId, UUID userId);

}
