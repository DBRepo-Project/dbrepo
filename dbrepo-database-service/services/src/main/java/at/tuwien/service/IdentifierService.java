package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.exception.IdentifierNotFoundException;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

@Service
public interface IdentifierService {

    /**
     * Finds all identifiers in the metadata database for a given database with id.
     *
     * @param databaseId The database id.
     * @return List of identifiers.
     */
    List<Identifier> findAll(Long databaseId);

    /**
     * Finds a specific identifier in the metadata database for a given database with id and type.
     *
     * @param databaseId The database id.
     * @param type       The type.
     * @return The identifier, if successful.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database.
     */
    Identifier find(Long databaseId, IdentifierType type) throws IdentifierNotFoundException;
}
