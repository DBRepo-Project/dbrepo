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

    List<Database> findReadAccess(UUID id);

    List<Database> findWriteAccess(UUID id);

    List<Database> findConfigureAccess(UUID id);

    Optional<Database> findPublicOrMine(Long databaseId, UUID id);

    Optional<Database> findPublic(Long databaseId);

    @Query("select d from Database d where d.id = :databaseId and (d.isPublic = true or d.creator.username = :username)")
    Optional<Database> findPublicOrMine(@Param("databaseId") Long databaseId, @Param("username") String username);

    @Query("select d from Database d where d.id = :databaseId")
    Optional<Database> findByDatabaseId(@Param("databaseId") Long databaseId);

}
