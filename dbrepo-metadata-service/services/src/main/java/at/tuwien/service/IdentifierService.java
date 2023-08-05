package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;

import java.util.List;

public interface IdentifierService {

    /**
     * Finds all identifiers.
     *
     * @return List of identifiers.
     */
    List<Identifier> findAll();

    /**
     * Finds all identifiers in the metadata database for a database with given id.
     *
     * @param databaseId The database id.
     * @return The list of identifiers.
     */
    List<Identifier> findAll(Long databaseId);

    /**
     * Finds an identifier by given id.
     *
     * @param id The identifier id.
     * @return The identifier.
     * @throws IdentifierNotFoundException The identifier does not exist.
     */
    Identifier find(Long id) throws IdentifierNotFoundException;

    /**
     * Finds a user by id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return The identifier.
     */
    Identifier findByDatabaseIdAndQueryId(Long databaseId, Long queryId) throws IdentifierNotFoundException;

    List<Identifier> findAllDatabaseIdentifiers();

    List<Identifier> findAllSubsetIdentifiers();
}
