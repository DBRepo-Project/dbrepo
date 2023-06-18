package at.tuwien.repository.mdb;

import at.tuwien.entities.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    List<Database> findReadAccess(String username);

    List<Database> findWriteAccess(String username);

    List<Database> findConfigureAccess(String username);

    List<Database> findAll();

    Optional<Database> findPublicOrMine(Long databaseId, String username);

    Optional<Database> findPublic(Long databaseId);

}
