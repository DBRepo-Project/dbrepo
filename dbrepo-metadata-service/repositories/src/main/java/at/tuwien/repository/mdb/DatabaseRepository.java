package at.tuwien.repository.mdb;

import at.tuwien.entities.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    List<Database> findReadAccess(UUID id);

    List<Database> findWriteAccess(UUID id);

    List<Database> findConfigureAccess(UUID id);

    List<Long> findAllOnlyIds();

    Optional<Database> findPublicOrMine(Long databaseId, UUID id);

    Optional<Database> findPublic(Long databaseId);

    Optional<Database> findByInternalName(String internalName);

}
