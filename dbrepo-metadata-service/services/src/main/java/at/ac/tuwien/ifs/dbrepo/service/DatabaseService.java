package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseModifyVisibilityDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface DatabaseService {

    /**
     * Finds all databases stored in the metadata database.
     *
     * @return List of databases.
     */
    List<Database> findAll();

    /**
     * Filters all databases where {@link Database#isPublic} or {@link Database#isSchemaPublic} evaluates to true.
     *
     * @return List of databases.
     */
    List<Database> findAllPublicOrSchemaPublic();

    /**
     * Filters all databases whose internal name matches the given internal name.
     *
     * @param internalName The internal name.
     * @return List of databases.
     */
    List<Database> findByInternalName(String internalName);

    /**
     * Filters all databases where {@link Database#isPublic} or {@link Database#isSchemaPublic} and the user by given id
     * has at least read access and whose internal name matches the given internal name evaluates to true.
     *
     * @param username     The user name.
     * @param internalName The internal name.
     * @return List of databases.
     */
    List<Database> findAllPublicOrSchemaPublicOrReadAccessByInternalName(String username, String internalName);

    /**
     * Filters all databases where {@link Database#isPublic} or {@link Database#isSchemaPublic} or the user by given id
     * has at least read access evaluate to true.
     *
     * @param username The user name.
     * @return List of databases.
     */
    List<Database> findAllPublicOrSchemaPublicOrReadAccess(String username);

    /**
     * Filters all databases where {@link Database#isPublic} or {@link Database#isSchemaPublic} or the internal name
     * matches the given internal name evaluate to true.
     *
     * @param internalName The internal name.
     * @return List of databases.
     */
    List<Database> findAllPublicOrSchemaPublicByInternalName(String internalName);

    /**
     * Find a database by given id.
     *
     * @param databaseId The id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database findById(UUID databaseId) throws DatabaseNotFoundException;

    /**
     * Creates a new database with minimal metadata in the metadata database and creates a new database on the
     * container.
     *
     * @param container The container.
     * @param createDto The metadata.
     * @param user      The user.
     * @return The database, if successful.
     * @throws DataServiceException                The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException      The connection with the data service could not be established.
     * @throws DatabaseNotFoundException           The created database was not found in the metadata database.
     * @throws SearchServiceException              The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException    The connection with the search service could not be established.
     * @throws DashboardServiceException           The dashboard service responded with an unexpected error code.
     * @throws DashboardServiceConnectionException The connection to the dashboard service could not be established.
     */
    Database create(Container container, CreateDatabaseDto createDto, UserDto user)
            throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException;

    /**
     * Updates the visibility of the database.
     *
     * @param database The database.
     * @param data     The visibility
     * @return The database, if successful.
     * @throws DatabaseNotFoundException        The created database was not found in the metadata database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    Database modifyVisibility(Database database, DatabaseModifyVisibilityDto data) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException;

    /**
     * Transfer ownership of a database
     *
     * @param database The database.
     * @param username The new owner username.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException        The created database was not found in the metadata database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    Database modifyOwner(Database database, String username) throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    /**
     * Modify image of database with given id.
     *
     * @param database The database.
     * @param image    The image.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException        The created database was not found in the metadata database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    Database modifyImage(Database database, byte[] image) throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    /**
     * Modify dashboard uid of database with given id.
     *
     * @param database The database.
     * @param uid      The dashboard uid.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException        The created database was not found in the metadata database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    Database modifyDashboard(Database database, String uid) throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    /**
     * Updates the table metadata of a given database.
     *
     * @param database The database.
     * @return The updated database, if successful.
     * @throws DatabaseNotFoundException        The created database was not found in the metadata database.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws MalformedException               The table is malformed, e.g. a column of a primary key constraint could not be found.
     * @throws TableNotFoundException           The table was not found in the metadata database.
     */
    Database updateTableMetadata(Database database) throws DatabaseNotFoundException, DataServiceException,
            SearchServiceException, SearchServiceConnectionException, DataServiceConnectionException,
            MalformedException, TableNotFoundException;

    /**
     * Updates the view metadata of a given database.
     *
     * @param database The database.
     * @return The updated database, if successful.
     * @throws DatabaseNotFoundException        The created database was not found in the metadata database.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws ViewNotFoundException            The view was not found in the metadata database.
     */
    Database updateViewMetadata(Database database) throws DatabaseNotFoundException, DataServiceException,
            SearchServiceException, SearchServiceConnectionException, DataServiceConnectionException,
            ViewNotFoundException;
}
