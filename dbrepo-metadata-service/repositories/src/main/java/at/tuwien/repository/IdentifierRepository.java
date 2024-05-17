package at.tuwien.repository;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

    /**
     * Finds identifiers by given database id.
     *
     * @param databaseId The database id.
     * @return List of matching identifiers.
     */
    List<Identifier> findByDatabaseId(Long databaseId);

    /**
     * Finds identifiers by given database id and query id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return List of matching identifiers.
     */
    List<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId);

    List<Identifier> findDatabaseIdentifier(Long databaseId);

    List<Identifier> findSubsetIdentifier(Long databaseId, Long queryId);

    List<Identifier> findAllDatabaseIdentifiers();

    List<Identifier> findAllSubsetIdentifiers();

    Optional<Identifier> findByDoi(String doi);

    Optional<Identifier> findEarliest();

}
