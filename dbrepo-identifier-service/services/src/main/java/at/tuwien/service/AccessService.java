package at.tuwien.service;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;

import java.util.UUID;

public interface AccessService {

    /**
     * Finds database access by given database id and user id.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return The database access.
     * @throws AccessDeniedException The access does not exist.
     */
    DatabaseAccess find(Long databaseId, UUID userId) throws AccessDeniedException;
}
