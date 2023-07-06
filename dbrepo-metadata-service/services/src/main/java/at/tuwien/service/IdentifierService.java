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
     * Finds an identifier by given id.
     *
     * @param id The identifier id.
     * @return The identifier.
     * @throws IdentifierNotFoundException The identifier does not exist.
     */
    Identifier find(Long id) throws IdentifierNotFoundException;
}
