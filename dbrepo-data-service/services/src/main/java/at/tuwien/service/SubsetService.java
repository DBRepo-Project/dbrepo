package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.SortTypeDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.exception.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SubsetService {

    /**
     * Creates the query store in the container and database.
     *
     * @param container    The container.
     * @param databaseName The database name.
     * @throws SQLException              The connection to the database could not be established.
     * @throws QueryStoreCreateException The query store could not be created.
     */
    void createQueryStore(PrivilegedContainerDto container, String databaseName) throws SQLException,
            QueryStoreCreateException;

    QueryResultDto execute(PrivilegedDatabaseDto database, String statement, Instant timestamp, UUID userId, Long page,
                           Long size, SortTypeDto sortDirection, String sortColumn)
            throws QueryStoreInsertException, SQLException, QueryNotFoundException, TableMalformedException, UserNotFoundException, NotAllowedException, RemoteUnavailableException, ServiceException, DatabaseNotFoundException;

    QueryResultDto reExecute(PrivilegedDatabaseDto database, QueryDto query, Long page, Long size,
                             SortTypeDto sortDirection, String sortColumn) throws TableMalformedException,
            SQLException;

    Long reExecuteCount(PrivilegedDatabaseDto database, QueryDto query) throws TableMalformedException,
            SQLException, QueryMalformedException;

    /**
     * Finds all queries in the query store of the given database id and query id.
     *
     * @param database        The database.
     * @param filterPersisted Optional filter to only display persisted queries, or non-persisted queries.
     * @return The list of queries.
     */
    List<QueryDto> findAll(PrivilegedDatabaseDto database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException, NotAllowedException, RemoteUnavailableException, ServiceException, DatabaseNotFoundException;

    ExportResourceDto export(PrivilegedDatabaseDto database, QueryDto query, Instant timestamp, String filename)
            throws SQLException, QueryMalformedException, SidecarExportException, StorageNotFoundException,
            StorageUnavailableException, ServiceException, RemoteUnavailableException;

    Long executeCountNonPersistent(PrivilegedDatabaseDto database, String statement, Instant timestamp)
            throws SQLException, QueryMalformedException, TableMalformedException;

    /**
     * Finds a query in the query store of the given database id and query id.
     *
     * @param database The database.
     * @param queryId  The query id.
     * @return The query.
     * @throws QueryNotFoundException The query store did not return a query
     */
    QueryDto findById(PrivilegedDatabaseDto database, Long queryId) throws QueryNotFoundException, SQLException, NotAllowedException, RemoteUnavailableException, UserNotFoundException, ServiceException, DatabaseNotFoundException;

    /**
     * Inserts a query and metadata to the query store of a given database id.
     *
     * @param database The database.
     * @param query    The query statement.
     * @param userId   The user id.
     * @return The stored query on success
     */
    Long storeQuery(PrivilegedDatabaseDto database, String query, Instant timestamp, UUID userId) throws SQLException,
            QueryStoreInsertException;

    /**
     * Persists a query to be displayed in the frontend.
     *
     * @param database The database id.
     * @param queryId  The query id.
     * @param persist  If true, the query is retained in the query store, ephemeral otherwise.
     */
    void persist(PrivilegedDatabaseDto database, Long queryId, Boolean persist) throws SQLException,
            QueryStorePersistException;

    /**
     * Deletes the stale queries that have not been persisted within 24 hours.
     */
    void deleteStaleQueries(PrivilegedDatabaseDto database) throws SQLException, QueryStoreGCException;
}
