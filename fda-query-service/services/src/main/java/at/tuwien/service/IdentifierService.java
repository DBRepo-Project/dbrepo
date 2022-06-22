package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;

public interface IdentifierService {

    /**
     * Finds a user by id.
     *
     * @param id The query id.
     * @return The identifier.
     */
    Identifier findByQueryId(Long id) throws IdentifierNotFoundException;
}
