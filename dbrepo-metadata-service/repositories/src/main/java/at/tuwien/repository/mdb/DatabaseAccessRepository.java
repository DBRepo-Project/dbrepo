package at.tuwien.repository.mdb;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.DatabaseAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseAccessRepository extends JpaRepository<DatabaseAccess, DatabaseAccessKey> {

    void deleteByHdbidAndHuserid(Long databaseId, UUID userId);

    /**
     * Finds all database access definitions for a database with given id.
     *
     * @param id The database id.
     * @return The list of database access definitions.
     */
    List<DatabaseAccess> findByDatabaseId(Long id);

    /**
     * Finds a specific database access definition for a database with given id and user id.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return The access definition, if successful.
     */
    Optional<DatabaseAccess> findByDatabaseIdAndUserId(Long databaseId, UUID userId);

}
