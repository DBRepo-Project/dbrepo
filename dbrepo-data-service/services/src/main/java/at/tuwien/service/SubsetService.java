package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.SortTypeDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.exception.*;
import jakarta.validation.constraints.NotNull;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

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

    /**
     * Creates a subset from the given statement at given time in the given database.
     *
     * @param database      The database.
     * @param statement     The subset statement.
     * @param timestamp     The timestamp as of which the data is queried. If smaller than <now>, historic data is queried.
     * @param userId        The user id of the creating user.
     * @param page          The page number. Optional but requires size to be set too.
     * @param size          The page size. Optional but requires page to be set too.
     * @param sortDirection The sort direction.
     * @param sortColumn    The column that is sorted.
     * @return The query result.
     * @throws QueryStoreInsertException  The query store refused to insert the query.
     * @throws SQLException               The connection to the database could not be established.
     * @throws QueryNotFoundException     The query was not found for re-execution.
     * @throws TableMalformedException    The table is malformed.
     * @throws UserNotFoundException      The user was not found.
     * @throws NotAllowedException        The operation is not allowed.
     * @throws RemoteUnavailableException The privileged database information could not be found in the Metadata Service.
     * @throws DatabaseNotFoundException  The database was not found in the Metadata Service.
     * @throws MetadataServiceException   The Metadata Service responded unexpected.
     */
    QueryResultDto execute(PrivilegedDatabaseDto database, String statement, Instant timestamp, UUID userId, Long page,
                           Long size, SortTypeDto sortDirection, String sortColumn)
            throws QueryStoreInsertException, SQLException, QueryNotFoundException, TableMalformedException,
            UserNotFoundException, NotAllowedException, RemoteUnavailableException, DatabaseNotFoundException,
            MetadataServiceException;

    /**
     * Re-executes the query of a given subset in the given database.
     *
     * @param database      The database.
     * @param query         The subset.
     * @param page          The page number. Optional but requires size to be set too.
     * @param size          The page size. Optional but requires page to be set too.
     * @param sortDirection The sort direction.
     * @param sortColumn    The column that is sorted.
     * @return The query result.
     * @throws TableMalformedException The table is malformed.
     * @throws SQLException            The connection to the database could not be established.
     */
    QueryResultDto reExecute(PrivilegedDatabaseDto database, QueryDto query, Long page, Long size,
                             SortTypeDto sortDirection, String sortColumn) throws TableMalformedException,
            SQLException;

    /**
     * Counts the subset row count of a query of a given subset in the given database.
     *
     * @param database The database.
     * @param query    The subset.
     * @return The row count.
     * @throws TableMalformedException The table is malformed.
     * @throws SQLException            The connection to the database could not be established.
     * @throws QueryMalformedException The re-execute query is malformed.
     */
    Long reExecuteCount(PrivilegedDatabaseDto database, QueryDto query) throws TableMalformedException,
            SQLException, QueryMalformedException;

    /**
     * Finds all queries in the query store of the given database id and query id.
     *
     * @param database        The database.
     * @param filterPersisted Optional filter to only display persisted queries, or non-persisted queries.
     * @return The list of queries.
     * @throws SQLException               The connection to the database could not be established.
     * @throws QueryNotFoundException     The query was not found for re-execution.
     * @throws RemoteUnavailableException The privileged database information could not be found in the Metadata Service.
     * @throws DatabaseNotFoundException  The database was not found in the Metadata Service.
     * @throws MetadataServiceException   The Metadata Service responded unexpected.
     */
    List<QueryDto> findAll(PrivilegedDatabaseDto database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException, RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException;

    /**
     * Exports a subset by re-executing the query in a given database with given timestamp to a given s3key.
     *
     * @param database  The database.
     * @param query     The query.
     * @param timestamp The timestamp.
     * @return The exported subset.
     * @throws SQLException                The connection to the database could not be established.
     * @throws QueryMalformedException     The mapped export query produced a database error.
     * @throws StorageNotFoundException    The exported subset was not found from the key provided by the sidecar in the Storage Service.
     * @throws StorageUnavailableException The communication to the Storage Service failed.
     * @throws RemoteUnavailableException  The privileged database information could not be found in the Metadata Service.
     * @throws ViewNotFoundException       The source view was not found in the metadata database.
     */
    ExportResourceDto export(PrivilegedDatabaseDto database, QueryDto query, Instant timestamp) throws SQLException,
            QueryMalformedException, StorageNotFoundException, StorageUnavailableException, RemoteUnavailableException,
            ViewNotFoundException;

    /**
     * Executes a subset query without saving it.
     *
     * @param database  The database.
     * @param statement The subset query.
     * @param timestamp The timestamp.
     * @return The row count.
     * @throws SQLException            The connection to the database could not be established.
     * @throws QueryMalformedException The mapped query produced a database error.
     * @throws TableMalformedException The database table is malformed.
     */
    Long executeCountNonPersistent(PrivilegedDatabaseDto database, String statement, Instant timestamp)
            throws SQLException, QueryMalformedException, TableMalformedException;

    /**
     * Finds a query in the query store of the given database id and query id.
     *
     * @param database The database.
     * @param queryId  The query id.
     * @return The query.
     * @throws QueryNotFoundException     The query store did not return a query.
     * @throws SQLException               The connection to the database could not be established.
     * @throws RemoteUnavailableException The privileged database information could not be found in the Metadata Service.
     * @throws DatabaseNotFoundException  The database metadata was not found in the Metadata Service.
     * @throws MetadataServiceException   Communication with the Metadata Service failed.
     */
    QueryDto findById(PrivilegedDatabaseDto database, Long queryId) throws QueryNotFoundException, SQLException,
            RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException;

    /**
     * Inserts a query and metadata to the query store of a given database id.
     *
     * @param database The database.
     * @param query    The query statement.
     * @param userId   The user id.
     * @return The stored query id on success.
     * @throws SQLException              The connection to the database could not be established.
     * @throws QueryStoreInsertException The query store failed to insert the query.
     */
    Long storeQuery(PrivilegedDatabaseDto database, String query, Instant timestamp, UUID userId) throws SQLException,
            QueryStoreInsertException;

    /**
     * Persists a query to be displayed in the frontend.
     *
     * @param database The database.
     * @param queryId  The query id.
     * @param persist  If true, the query is retained in the query store, ephemeral otherwise.
     * @throws SQLException               The connection to the database could not be established.
     * @throws QueryStorePersistException The query store failed to persist/unpersist the query.
     */
    void persist(PrivilegedDatabaseDto database, Long queryId, Boolean persist) throws SQLException,
            QueryStorePersistException;

    /**
     * Deletes the stale queries that have not been persisted within 24 hours.
     *
     * @param database The database.
     * @throws SQLException          The connection to the database could not be established.
     * @throws QueryStoreGCException The query store failed to delete stale queries.
     */
    void deleteStaleQueries(PrivilegedDatabaseDto database) throws SQLException, QueryStoreGCException;

    /**
     * Exports data as dataset from the database view with given name at a given timestamp.
     *
     * @param database  The database.
     * @param viewName  The view name.
     * @param timestamp The timestamp.
     * @return The dataset.
     * @throws ViewNotFoundException   The view was not found in the metadata database.
     * @throws QueryMalformedException The query to eis malformed.
     */
    Dataset<Row> getData(@NotNull PrivilegedDatabaseDto database, String viewName, Instant timestamp)
            throws ViewNotFoundException, QueryMalformedException;
}
