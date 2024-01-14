package at.tuwien.service;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
public interface StoreService {

    /**
     * Finds all queries in the query store of the given database id and query id.
     *
     * @param databaseId The database id.
     * @param persisted  Optional filter to only display persisted queries, or non-persisted queries.
     * @param principal  The user principal.
     * @return The list of queries.
     * @throws ImageNotSupportedException The image is not supported
     * @throws DatabaseNotFoundException  The database was not found in the metadata database
     * @throws QueryStoreException        The query store produced an invalid result
     */
    List<Query> findAll(Long databaseId, Boolean persisted, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException, ContainerNotFoundException, DatabaseConnectionException,
            TableMalformedException, UserNotFoundException;

    /**
     * Finds a query in the query store of the given database id and query id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @param principal  The user principal.
     * @return The query.
     * @throws ImageNotSupportedException The image is not supported
     * @throws DatabaseNotFoundException  The database was not found in the metadata database
     * @throws QueryStoreException        The query store produced an invalid result
     * @throws QueryNotFoundException     The query store did not return a query
     */
    Query findOne(Long databaseId, Long queryId, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, QueryNotFoundException, QueryStoreException, UserNotFoundException;

    /**
     * Inserts a query and metadata to the query store of a given database id.
     *
     * @param databaseId The database id.
     * @param metadata   The statement.
     * @param principal  The user principal.
     * @return The stored query on success
     * @throws QueryStoreException         The query store raised some error
     * @throws DatabaseNotFoundException   The database id was not found in the metadata database
     * @throws ImageNotSupportedException  The image is not supported
     * @throws UserNotFoundException       The user was not found in the metadata database.
     * @throws DatabaseConnectionException The database connection to the remote container failed.
     */
    Query insert(Long databaseId, ExecuteStatementDto metadata, Principal principal) throws QueryStoreException,
            DatabaseNotFoundException, ImageNotSupportedException, UserNotFoundException, DatabaseConnectionException, KeycloakRemoteException, AccessDeniedException, QueryNotFoundException;

    /**
     * Persists a query to be displayed in the frontend.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @param data       The desired persist state.
     * @return The stored query on success.
     * @throws DatabaseNotFoundException   The database id was not found in the metadata database
     * @throws ImageNotSupportedException  The image is not supported.
     * @throws DatabaseConnectionException The database connection to the remote container failed.
     * @throws QueryStoreException         The query store raised some error.
     */
    Query persist(Long databaseId, Long queryId, QueryPersistDto data) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, QueryStoreException, UserNotFoundException, IdentifierAlreadyPublishedException;

    /**
     * Deletes the stale queries that have not been persisted within 24 hozrs.
     *
     * @throws ImageNotSupportedException The image is not supported.
     * @throws QueryStoreException        The query store raised some error.
     */
    void deleteStaleQueries() throws ImageNotSupportedException, QueryStoreException;
}
