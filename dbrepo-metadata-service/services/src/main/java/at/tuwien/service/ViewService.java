package at.tuwien.service;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import java.util.List;

public interface ViewService {

    /**
     * Find a view of a database with id.
     *
     * @param database The database.
     * @param viewId   The view id.
     * @return The view, if successful.
     */
    View findById(Database database, Long viewId) throws ViewNotFoundException;

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
    void delete(View view) throws ServiceException, ServiceConnectionException, DatabaseNotFoundException,
            ViewNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Creates a view in the container with given id and database with id with the given query.
     *
     * @param database The database.
     * @param user     The user.
     * @param data     The given query.
     * @return The view that was created.
     */
    View create(Database database, User user, ViewCreateDto data) throws MalformedException, ServiceException,
            ServiceConnectionException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;
}
