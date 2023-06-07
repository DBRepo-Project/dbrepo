package at.tuwien.repository.mdb;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.DatabaseAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatabaseAccessRepository extends JpaRepository<DatabaseAccess, DatabaseAccessKey> {

    @Query("select a from DatabaseAccess a where a.hdbid = :databaseId and a.user.username = :username")
    Optional<DatabaseAccess> findByDatabaseIdAndUsername(Long databaseId, String username);

}
