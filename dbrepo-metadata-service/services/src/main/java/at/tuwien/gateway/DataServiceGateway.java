package at.tuwien.gateway;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.CreateViewDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.CreateTableDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.api.database.table.TableUpdateDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.exception.*;

import java.util.List;
import java.util.UUID;

public interface DataServiceGateway {

    /**
     * Create r/w access for a given user to a given database.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @param access     The access.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws DatabaseNotFoundException      Some of the privileged parameters of the given database were not provided by the metadata service.
     */
    void createAccess(UUID databaseId, UUID userId, AccessTypeDto access) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException;

    /**
     * Update r/w access for a given user to a given database.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @param access     The access.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws AccessNotFoundException        Some of the privileged parameters of the given database were not provided by the metadata service.
     */
    void updateAccess(UUID databaseId, UUID userId, AccessTypeDto access) throws DataServiceConnectionException,
            DataServiceException, AccessNotFoundException;

    /**
     * Deletes access for a given user to a given database.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws AccessNotFoundException        Some of the privileged parameters of the given database were not provided by the metadata service.
     */
    void deleteAccess(UUID databaseId, UUID userId) throws DataServiceConnectionException, DataServiceException,
            AccessNotFoundException;

    /**
     * Creates a database in the data service.
     *
     * @param data The data.
     * @return The created database, if successful.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws DatabaseNotFoundException      Some of the privileged parameters of the given database were not provided by the metadata service.
     */
    DatabaseDto createDatabase(CreateDatabaseDto data) throws DataServiceConnectionException, DataServiceException,
            DatabaseNotFoundException;

    /**
     * Updates the user password in the given database in the data service.
     *
     * @param databaseId The database id.
     * @param data       The user password.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws DatabaseNotFoundException      Some of the privileged parameters of the given database were not provided by the metadata service.
     */
    void updateDatabase(UUID databaseId, UpdateUserPasswordDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException;

    void updateTable(UUID databaseId, UUID tableId, TableUpdateDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException;

    /**
     * Creates a table in a given database.
     *
     * @param databaseId The database id.
     * @param data       The table data.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws DatabaseNotFoundException      Some of the privileged parameters of the given database were not provided by the metadata service.
     * @throws TableExistsException           A table with this internal name exists already in the database.
     */
    void createTable(UUID databaseId, CreateTableDto data) throws DataServiceConnectionException, DataServiceException,
            DatabaseNotFoundException, TableExistsException;

    /**
     * Deletes a given table in a given database.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws TableNotFoundException         The given table was not found in the database.
     */
    void deleteTable(UUID databaseId, UUID tableId) throws DataServiceConnectionException, DataServiceException,
            TableNotFoundException;

    /**
     * Creates a view in the given database.
     *
     * @param databaseId The database id.
     * @param data       The view data.
     * @return The created view, if successful.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     */
    ViewDto createView(UUID databaseId, CreateViewDto data) throws DataServiceConnectionException, DataServiceException;

    /**
     * Deletes a given view in the given database.
     *
     * @param databaseId The database id.
     * @param viewId     The view id.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws ViewNotFoundException          The given view was not found in the database.
     */
    void deleteView(UUID databaseId, UUID viewId) throws DataServiceConnectionException, DataServiceException,
            ViewNotFoundException;

    /**
     * Finds a given query in a given database.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return The query, if successful.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws QueryNotFoundException         The given query was not found in the query store.
     */
    QueryDto findQuery(UUID databaseId, UUID queryId) throws DataServiceConnectionException, DataServiceException,
            QueryNotFoundException;

    /**
     * Exports a given query.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return The exported resource, if successful.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws QueryNotFoundException         The given query was not found in the query store.
     */
    ExportResourceDto exportQuery(UUID databaseId, UUID queryId) throws DataServiceConnectionException,
            DataServiceException, QueryNotFoundException;

    /**
     * Obtain table schemas from a given database.
     *
     * @param databaseId The database id.
     * @return The list of tables, if successful.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws TableNotFoundException         The table was not found in the database.
     */
    List<TableDto> getTableSchemas(UUID databaseId) throws DataServiceConnectionException, DataServiceException,
            TableNotFoundException;

    /**
     * Obtain view schemas from a given database.
     *
     * @param databaseId The database id.
     * @return The list of tables, if successful.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws ViewNotFoundException          The table was not found in the database.
     */
    List<ViewDto> getViewSchemas(UUID databaseId) throws DataServiceConnectionException, DataServiceException,
            ViewNotFoundException;

    /**
     * Obtain table statistics for a given table in a given database.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @return The statistic, if successful.
     * @throws DataServiceConnectionException The connection to the data service could not be established.
     * @throws DataServiceException           The data service responded unexpectedly.
     * @throws TableNotFoundException         The table was not found in the database.
     */
    TableStatisticDto getTableStatistics(UUID databaseId, UUID tableId) throws DataServiceConnectionException,
            DataServiceException, TableNotFoundException;
}
