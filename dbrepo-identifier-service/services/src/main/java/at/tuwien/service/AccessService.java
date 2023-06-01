package at.tuwien.service;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;

import java.util.UUID;

public interface AccessService {

    DatabaseAccess find(Long databaseId, UUID userId) throws AccessDeniedException;
}
