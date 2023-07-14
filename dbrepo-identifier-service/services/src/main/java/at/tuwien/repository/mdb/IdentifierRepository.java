package at.tuwien.repository.mdb;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * Finds identifiers by given query id.
     *
     * @param queryId The query id.
     * @return List of matching identifiers.
     */
    List<Identifier> findByQueryId(Long queryId);

    /**
     * Finds identifiers by given database id and query id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return List of matching identifiers.
     */
    List<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId);

    /**
     * Checks if an identifier exists by given database id and identifier type.
     *
     * @param databaseId The database id.
     * @param type       The identifier type.
     * @return True if identifier exists, false otherwise.
     */
    Boolean existsByDatabaseIdAndType(Long databaseId, IdentifierType type);

    /**
     * Checks if an identifier exists by given database id, query id and identifier type.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @param type       The identifier type.
     * @return True if identifier exists, false otherwise.
     */
    Boolean existsByDatabaseIdAndQueryIdAndType(Long databaseId, Long queryId, IdentifierType type);

}
