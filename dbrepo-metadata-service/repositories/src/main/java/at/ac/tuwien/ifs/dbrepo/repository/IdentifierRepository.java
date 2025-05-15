package at.ac.tuwien.ifs.dbrepo.repository;

import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, UUID> {

    List<Identifier> findAll();

    /**
     * Finds identifiers by given database id.
     *
     * @param databaseId The database id.
     * @return List of matching identifiers.
     */
    List<Identifier> findByDatabaseId(UUID databaseId);

    /**
     * Finds identifiers by given database id and query id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return List of matching identifiers.
     */
    List<Identifier> findByDatabaseIdAndQueryId(UUID databaseId, UUID queryId);

    List<Identifier> findDatabaseIdentifier(UUID databaseId);

    List<Identifier> findSubsetIdentifier(UUID databaseId, UUID queryId);

    List<Identifier> findAllDatabaseIdentifiers();

    List<Identifier> findAllSubsetIdentifiers();

    Optional<Identifier> findByDoi(String doi);

    Optional<Identifier> findEarliest();

}
