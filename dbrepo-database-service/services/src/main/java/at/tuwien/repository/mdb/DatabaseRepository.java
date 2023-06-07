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

    @Query("select d from Database d where d.container.id = :containerId")
    List<Database> findAll(@Param("containerId") Long containerId);

    @Query("select d from Database d where d.container.id = :containerId and d.id = :databaseId and (d.isPublic = " +
            "true or d.owner.username = " +
            ":username)")
    Optional<Database> findPublicOrMine(@Param("containerId") Long containerId, @Param("databaseId") Long databaseId,
                                        @Param("username") String username);

    @Query("select d from Database d where d.container.id = :containerId and d.isPublic = true and d.id = :databaseId")
    Optional<Database> findPublic(@Param("containerId") Long containerId, @Param("databaseId") Long databaseId);

}
