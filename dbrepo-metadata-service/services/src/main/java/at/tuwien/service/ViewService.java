package at.tuwien.service;

import at.tuwien.api.database.CreateViewDto;
import at.tuwien.api.database.ViewUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import java.util.List;
import java.util.UUID;

public interface ViewService {

    /**
     * Find a view of a database with id.
     *
     * @param database The database.
     * @param viewId   The view id.
     * @return The view, if successful.
     */
    View findById(Database database, UUID viewId) throws ViewNotFoundException;

    /**
     * Find all views by database id.
     *
     * @param database The database.
     * @param user     The user.
     * @return A list of views.
     */
    List<View> findAll(Database database, User user);

    /**
     * Delete view in the container with the given id and database with id and the given view id.
     *
     * @param view The view.
     */
    void delete(View view) throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            ViewNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Creates a view in the container with given id and database with id with the given query.
     *
     * @param database The database.
     * @param user     The user.
     * @param data     The given query.
     * @return The view that was created.
     * @throws MalformedException
     * @throws DataServiceException
     * @throws DataServiceConnectionException
     * @throws DatabaseNotFoundException
     * @throws SearchServiceException
     * @throws SearchServiceConnectionException
     */
    View create(Database database, User user, CreateViewDto data) throws MalformedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    /**
     * @param database
     * @param view
     * @param data
     * @return
     * @throws DataServiceConnectionException
     * @throws DatabaseNotFoundException
     * @throws SearchServiceException
     * @throws SearchServiceConnectionException
     * @throws ViewNotFoundException
     */
    View update(Database database, View view, ViewUpdateDto data) throws DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException, ViewNotFoundException;
}
