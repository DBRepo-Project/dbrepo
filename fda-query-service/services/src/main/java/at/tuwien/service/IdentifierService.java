package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;

public interface IdentifierService {

    /**
     * Finds a user by id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return The identifier.
     */
    Identifier findByDatabaseIdAndQueryId(Long databaseId, Long queryId) throws IdentifierNotFoundException;
}
