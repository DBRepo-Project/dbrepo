package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;

import java.util.List;

public interface IdentifierService {

    /**
     * Finds all identifiers in the metadata database.
     *
     * @return List of identifiers.
     */
    List<Identifier> findAll();

    /**
     * Finds an identifier with given id in the metadata database.
     *
     * @param id The identifier id.
     * @return The identifier, if successful.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database.
     */
    Identifier find(Long id) throws IdentifierNotFoundException;
}
