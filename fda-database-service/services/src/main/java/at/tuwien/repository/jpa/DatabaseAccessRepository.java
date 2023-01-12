package at.tuwien.repository.jpa;

import at.tuwien.entities.database.DatabaseAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseAccessRepository extends JpaRepository<DatabaseAccess, Long> {

    void deleteByHdbidAndHuserid(Long databaseId, Long userId);

    Optional<DatabaseAccess> findByHdbidAndHuserid(Long databaseId, Long userId);

    List<DatabaseAccess> findByHdbid(Long databaseId);

    @Query("select a from DatabaseAccess a where a.hdbid = :databaseId and a.user.username = :username")
    Optional<DatabaseAccess> findByDatabaseIdAndUsername(@Param("databaseId") Long databaseId, @Param("username") String username);

}
