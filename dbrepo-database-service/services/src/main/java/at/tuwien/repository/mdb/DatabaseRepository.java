package at.tuwien.repository.mdb;

import at.tuwien.entities.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    @Query("select d from Database d where d.owner.username = :username")
    List<Database> findAllByUsername(@Param("username") String username);

    List<Database> findAll();

    @Query("select d from Database d where d.id = :databaseId and (d.isPublic = " +
            "true or d.owner.username = " +
            ":username)")
    Optional<Database> findPublicOrMine(@Param("databaseId") Long databaseId, @Param("username") String username);

    @Query("select d from Database d where d.isPublic = true and d.id = :databaseId")
    Optional<Database> findPublic(@Param("databaseId") Long databaseId);

}
