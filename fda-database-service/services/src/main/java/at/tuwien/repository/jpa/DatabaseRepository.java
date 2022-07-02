package at.tuwien.repository.jpa;

import at.tuwien.entities.database.Database;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    @Query("select d from Database d where d.container.id = :containerId and (d.isPublic = true or (d.isPublic = " +
            "false and d.creator.username = :username))")
    List<Database> findAllByPublicAndContainerIdOrMine(@Param("containerId") Long containerId,
                                             @Param("username") String username);

    @Query("select d from Database d where d.container.id = :containerId and d.isPublic = true")
    List<Database> findAllByPublicAndContainerId(@Param("containerId") Long containerId);

    @Query("select d from Database d where d.id = :databaseId and (d.isPublic = true or d.creator.username = " +
            ":username)")
    Optional<Database> findPublicOrMine(@Param("databaseId") Long databaseId, @Param("username") String username);

    @Query("select d from Database d where d.isPublic = true and d.id = :databaseId")
    Optional<Database> findPublic(@Param("databaseId") Long databaseId);

}
