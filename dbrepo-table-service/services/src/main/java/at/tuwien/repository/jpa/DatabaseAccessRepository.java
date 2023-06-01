package at.tuwien.repository.jpa;

import at.tuwien.entities.database.DatabaseAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatabaseAccessRepository extends JpaRepository<DatabaseAccess, Long> {

    @Query("select a from DatabaseAccess a where a.hdbid = :databaseId and a.user.username = :username")
    Optional<DatabaseAccess> findByDatabaseIdAndUsername(Long databaseId, String username);

}
