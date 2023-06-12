package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;

import java.util.List;

public interface IdentifierService {

    /**
     * Finds all identifiers in the metadata database for a database with given id.
     *
     * @param databaseId The database id.
     * @return The list of identifiers.
     */
    List<Identifier> findAll(Long databaseId);
}
