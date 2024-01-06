package at.tuwien.service;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;

public interface ViewService {

    View findById(Long id) throws ViewNotFoundException, DatabaseNotFoundException;

    /**
     * Find all views by database id.
     *
     * @param databaseId The database id.
     * @param principal  The user.
     * @return A list of views.
     * @throws UserNotFoundException The user with authorization principal was not found.
     */
    List<View> findAll(Long databaseId, Principal principal) throws UserNotFoundException, DatabaseNotFoundException;

    /**
     * Find a view by database id and view id.
     *
     * @param databaseId The database id.
     * @param id         The view id.
     * @param principal  The user.
     * @return The view, if successful.
     * @throws ViewNotFoundException The view was not found in the metadata database.
     * @throws UserNotFoundException The user with authorization principal was not found.
     */
    View findById(Long databaseId, Long id, Principal principal) throws ViewNotFoundException, UserNotFoundException,
            DatabaseNotFoundException;

    /**
     * Delete view in the container with the given id and database with id and the given view id.
     *
     * @param databaseId  The database id.
     * @param id          The view id.
     * @param principal   The authorization principal.
     * @throws ViewNotFoundException       The view was not found in the metadata database.
     * @throws UserNotFoundException       The user with authorization principal was not found.
     * @throws DatabaseNotFoundException   The database was not found.
     * @throws DatabaseConnectionException The connection to the database could not be established.
     * @throws QueryMalformedException     The query to delete the view is malformed.
     * @throws ViewMalformedException      The view is malformed and could not be deleted.
     */
    void delete(Long databaseId, Long id, Principal principal) throws ViewNotFoundException,
            UserNotFoundException, DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException, ViewMalformedException;

    /**
     * Creates a view in the container with given id and database with id with the given query.
     *
     * @param databaseId  The database id.
     * @param data        The given query.
     * @param principal   The authorization principal.
     * @return The view that was created.
     * @throws DatabaseNotFoundException   The database was not found.
     * @throws DatabaseConnectionException The connection to the database could not be established.
     * @throws QueryMalformedException     The query to create the view is malformed.
     * @throws ViewMalformedException      The view is malformed and could not be created.
     * @throws UserNotFoundException       The user with authorization principal was not found.
     */
    View create(Long databaseId, ViewCreateDto data, Principal principal) throws DatabaseNotFoundException,
            DatabaseConnectionException, QueryMalformedException, ViewMalformedException, UserNotFoundException;
}
