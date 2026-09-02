package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.util.UUID;

public interface ViewService {

    /**
     * Find a view of a database with id.
     *
     * @param database The database.
     * @param viewId   The view id.
     * @return The view, if successful.
     * @throws ViewNotFoundException The view with given id was not found in the metadata database.
     */
    View findById(Database database, UUID viewId) throws ViewNotFoundException;

    /**
     * Delete view in the container with the given id and database with id and the given view id.
     *
     * @param view The view.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws ViewNotFoundException            The view was not found in the metadata database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    void delete(View view) throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            ViewNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Creates a view in the container with given id and database with id with the given query.
     *
     * @param database The database.
     * @param ownedBy  The owner username.
     * @param data     The given query.
     * @return The view that was created.
     * @throws MalformedException               The query was malformed in the data service.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    View create(Database database, String ownedBy, CreateViewDto data) throws MalformedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, ColumnNotFoundException;

    View createReplicated(Database database, String ownedBy, ViewDto data) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DataServiceConnectionException,
            DataServiceException;

    /**
     * Updates the view in the metadata database and search service.
     *
     * @param view The view.
     * @param data The update data.
     * @return The view, if successful.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    View update(View view, ViewUpdateDto data) throws DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException;
}
