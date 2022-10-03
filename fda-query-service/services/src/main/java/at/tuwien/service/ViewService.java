package at.tuwien.service;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;

public interface ViewService {

    /**
     * Find all views by database id.
     *
     * @param databaseId The database id.
     * @param principal  The user.
     * @return A list of views.
     */
    List<View> findAll(Long databaseId, Principal principal) throws UserNotFoundException;

    /**
     * Find a view by database id and view id.
     *
     * @param databaseId The database id.
     * @param id         The view id.
     * @param principal  The user.
     * @return The view, if successful.
     * @throws ViewNotFoundException The view was not found in the metadata database.
     */
    View findById(Long databaseId, Long id, Principal principal) throws ViewNotFoundException, UserNotFoundException;

    /**
     * Creates a view in the container with given id and database with id with the given query.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param data        The given query.
     * @param principal   The authorization principal.
     * @return The view that was created.
     * @throws DatabaseNotFoundException
     * @throws DatabaseConnectionException
     * @throws QueryMalformedException
     * @throws ViewMalformedException
     */
    View create(Long containerId, Long databaseId, ViewCreateDto data, Principal principal) throws DatabaseNotFoundException,
            DatabaseConnectionException, QueryMalformedException, ViewMalformedException, UserNotFoundException;
}
