package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;

import java.util.List;

public interface IdentifierService {

    /**
     * Finds all identifiers in the metadata database.
     *
     * @return List of identifiers.
     */
    List<Identifier> findAll();
}
