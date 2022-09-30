package at.tuwien.service;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

public interface ViewService {

    /**
     * Find all views by database id.
     *
     * @param databaseId The database id.
     * @return A list of views.
     */
    List<View> findAll(Long databaseId);

    /**
     * Find a view by database id and view id.
     *
     * @param databaseId The database id.
     * @param id         The view id.
     * @return The view, if successful.
     * @throws ViewNotFoundException The view was not found in the metadata database.
     */
    View findById(Long databaseId, Long id) throws ViewNotFoundException;

    /**
     * Counts the tuples for a given container id and database id and view id.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param viewId      The view id.
     * @return The number of tuples.
     * @throws DatabaseNotFoundException
     * @throws DatabaseConnectionException
     * @throws TableMalformedException
     * @throws ViewNotFoundException
     * @throws QueryMalformedException
     * @throws ImageNotSupportedException
     * @throws QueryStoreException
     */
    Long count(Long containerId, Long databaseId, Long viewId) throws DatabaseNotFoundException,
            DatabaseConnectionException, TableMalformedException, ViewNotFoundException, QueryMalformedException,
            ImageNotSupportedException, QueryStoreException;

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
