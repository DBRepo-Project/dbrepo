package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SubsetService {

    /**
     * Creates a subset from the given statement at given time in the given database.
     *
     * @param database  The database.
     * @param subset The subset information.
     * @param timestamp The timestamp as of which the data is queried. If smaller than <now>, historic data is queried.
     * @param username    The username of the creating user.
     * @param creationLocation The location where the query was initially created (can be null).
     * @return The query id.
     * @throws QueryStoreInsertException The query store refused to insert the query.
     * @throws SQLException              The connection to the database could not be established.
     */
    UUID create(DatabaseDto database, SubsetDto subset, Instant timestamp, String username, String creationLocation)
            throws QueryStoreInsertException, SQLException, QueryMalformedException, TableNotFoundException, ImageNotFoundException, ViewMalformedException, ViewNotFoundException, ColumnNotFoundException;

    /**
     * Counts the subset row count of a query of a given subset in the given database.
     *
     * @param database The database.
     * @param query    The subset.
     * @return The row count.
     * @throws QueryMalformedException The query is malformed.
     * @throws SQLException            The connection to the database could not be established.
     */
    Long reExecuteCount(DatabaseDto database, QueryDto query) throws SQLException, QueryMalformedException;

    /**
     * Retrieve data from a subset in a database and optionally paginate with number of page and size of results.
     *
     * @param database The database.
     * @param query   The query statements.
     * @return The data.
     * @throws QueryMalformedException The mapped query produced a database error.
     * @throws TableNotFoundException  The database table is malformed.
     */
    Dataset<Row> getData(DatabaseDto database, String query) throws QueryMalformedException, TableNotFoundException;

    /**
     * Finds all queries in the query store of the given database id and query id.
     *
     * @param database        The database.
     * @param filterPersisted Optional filter to only display persisted queries, or non-persisted queries.
     * @return The list of queries.
     * @throws SQLException               The connection to the database could not be established.
     * @throws QueryNotFoundException     The query was not found for re-execution.
     * @throws RemoteUnavailableException The  database information could not be found in the Metadata Service.
     * @throws DatabaseNotFoundException  The database was not found in the Metadata Service.
     * @throws MetadataServiceException   The Metadata Service responded unexpected.
     */
    List<QueryDto> findAll(DatabaseDto database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException, RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException, UserNotFoundException;

    /**
     * Executes a subset query without saving it.
     *
     * @param database  The database.
     * @param statement The subset query.
     * @param timestamp The timestamp.
     * @return The row count.
     * @throws SQLException            The connection to the database could not be established.
     * @throws QueryMalformedException The mapped query produced a database error.
     */
    Long executeCountNonPersistent(DatabaseDto database, String statement, Instant timestamp)
            throws SQLException, QueryMalformedException;

    /**
     * Finds a query in the query store of the given database id and query id.
     *
     * @param database The database.
     * @param queryId  The query id.
     * @return The query.
     * @throws QueryNotFoundException     The query store did not return a query.
     * @throws SQLException               The connection to the database could not be established.
     * @throws RemoteUnavailableException The  database information could not be found in the Metadata Service.
     * @throws UserNotFoundException      The user that created the query was not found in the Metadata Service.
     * @throws DatabaseNotFoundException  The database metadata was not found in the Metadata Service.
     * @throws MetadataServiceException   Communication with the Metadata Service failed.
     */
    QueryDto findById(DatabaseDto database, UUID queryId) throws QueryNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException, MetadataServiceException;

    /**
     * Inserts a query and metadata to the query store of a given database id.
     *
     * @param database The database.
     * @param statement    The query statement.
     * @param timestamp   The timestamp.
     * @param username   The username.
     * @param creationLocation The location where the query was initially created (can be null).
     * @return The stored query id on success.
     * @throws SQLException              The connection to the database could not be established.
     * @throws QueryStoreInsertException The query store failed to insert the query.
     */
    UUID storeQuery(DatabaseDto database, String statement, Instant timestamp, String username, String creationLocation) throws SQLException,
            QueryStoreInsertException, ViewMalformedException, QueryMalformedException;

    /**
     * Persists a query to be displayed in the frontend.
     *
     * @param database The database.
     * @param queryId  The query id.
     * @param persist  If true, the query is retained in the query store, ephemeral otherwise.
     * @throws QueryStorePersistException The query store failed to persist/unpersist the query.
     * @throws SQLException               The connection to the database could not be established.
     * @throws QueryStorePersistException The query store failed to persist/unpersist the query.
     */
    void persist(DatabaseDto database, UUID queryId, Boolean persist) throws SQLException, QueryStorePersistException;

    /**
     * Replicates a query from another instance by directly inserting it into the query store.
     * This method does not re-execute the query but preserves all original data and metadata.
     *
     * @param database The database where the query should be replicated.
     * @param queryDto The query DTO containing all the data to be replicated.
     * @return The ID of the replicated query (same as the original).
     * @throws SQLException The connection to the database could not be established or the insert failed.
     */
    UUID replicateQuery(DatabaseDto database, QueryDto queryDto) throws SQLException;

    /**
     * Checks if a query needs to be modified based on its creation location.
     * A query needs modification if it was created on a different site (replica).
     * Returns the modified query if modification is needed, otherwise returns the original query.
     *
     * @param queryDto The QueryDto to check
     * @param currentBaseUrl The current base URL of this service instance
     * @return The modified query if modification is needed, otherwise the original query
     */
    String checkIfQueryNeedsModification(QueryDto queryDto, String currentBaseUrl);

    /**
     * Deletes the stale queries that have not been persisted within 24 hours.
     *
     * @param database The database.
     * @throws SQLException          The connection to the database could not be established.
     * @throws QueryStoreGCException The query store failed to delete stale queries.
     */
    void deleteStaleQueries(DatabaseDto database) throws SQLException, QueryStoreGCException;
}
