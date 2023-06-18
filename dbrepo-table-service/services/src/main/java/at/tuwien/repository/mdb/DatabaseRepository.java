package at.tuwien.repository.mdb;

import at.tuwien.entities.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    @Query("select d from Database d where d.id = :databaseId and (d.isPublic = true or d.creator.username = :username)")
    Optional<Database> findPublicOrMine(@Param("databaseId") Long databaseId, @Param("username") String username);

    @Query("select d from Database d where d.id = :databaseId and d.isPublic = true")
    Optional<Database> findPublic(@Param("databaseId") Long databaseId);

    @Query("select d from Database d where d.id = :databaseId")
    Optional<Database> findByContainerIdAndDatabaseId(@Param("databaseId") Long databaseId);
    
}
