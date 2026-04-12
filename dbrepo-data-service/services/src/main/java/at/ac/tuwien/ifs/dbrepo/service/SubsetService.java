package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.api.SubsetMetadata;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SubsetService {

    /**
     * Creates a subset from the given statement at given time in the given database.
     *
     * @param database  The database.
     * @param subset    The subset information.
     * @param timestamp The timestamp as of which the data is queried. If smaller than <now>, historic data is queried.
     * @return The query id.
     * @throws QueryStoreInsertException The query store refused to insert the query.
     * @throws SQLException              The connection to the database could not be established.
     * @throws QueryMalformedException   The query to create the subset is malformed or contains illegal keywords such as <code>DROP</code>.
     * @throws TableNotFoundException    The referenced table source was not found.
     * @throws ViewNotFoundException     The referenced view source was not found.
     * @throws ColumnNotFoundException   The referenced column was not found.
     */
    UUID create(Database database, SubsetDto subset, Instant timestamp) throws QueryStoreInsertException, SQLException,
            QueryMalformedException, TableNotFoundException, ImageNotFoundException, ViewNotFoundException,
            ColumnNotFoundException;

    /**
     * Compute result set count and -hash metadata for a given subset.
     *
     * @param database  The database.
     * @param statement The subset query.
     * @return The result set metadata.
     * @throws SQLException            The connection to the database could not be established.
     * @throws QueryExecutionException The query could not be executed.
     * @throws QueryMalformedException The query is malformed.
     */
    SubsetMetadata getMetadata(Database database, String statement) throws SQLException, QueryExecutionException,
            QueryMalformedException;

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
     * @throws UserNotFoundException      The user that created the query was not found in the Metadata Service.
     */
    List<QueryDto> findAll(Database database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException, RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException,
            UserNotFoundException;

    /**
     * Finds a query in the query store of the given database id and query id.
     *
     * @param database The database.
     * @param id       The subset id.
     * @return The query.
     * @throws QueryNotFoundException     The query store did not return a query.
     * @throws SQLException               The connection to the database could not be established.
     * @throws RemoteUnavailableException The  database information could not be found in the Metadata Service.
     * @throws UserNotFoundException      The user that created the query was not found in the Metadata Service.
     * @throws DatabaseNotFoundException  The database metadata was not found in the Metadata Service.
     * @throws MetadataServiceException   Communication with the Metadata Service failed.
     */
    Subset findById(Database database, UUID id) throws QueryNotFoundException, SQLException,
            RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException, MetadataServiceException;

    /**
     * Inserts a query and metadata to the query store of a given database id.
     *
     * @param database  The database.
     * @param statement The query statement.
     * @return The stored query id on success.
     * @throws SQLException              The connection to the database could not be established.
     * @throws QueryStoreInsertException The query store failed to insert the query.
     */
    UUID storeQuery(Database database, String statement, String normalizedQuery, Instant timestamp)
            throws SQLException, QueryStoreInsertException;

    /**
     * Persists a query to be displayed in the frontend.
     *
     * @param database The database.
     * @param subsetId The subset id.
     * @param persist  If true, the query is retained in the query store, ephemeral otherwise.
     * @throws SQLException               The connection to the database could not be established.
     * @throws QueryStorePersistException The query store failed to persist/unpersist the query.
     */
    void persist(Database database, UUID subsetId, Boolean persist) throws SQLException, QueryStorePersistException;
}
