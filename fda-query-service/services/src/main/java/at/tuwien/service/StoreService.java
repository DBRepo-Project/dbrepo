package at.tuwien.service;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
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
    List<Query> findAll(Long containerId, Long databaseId, Boolean persisted, Principal principal) throws DatabaseNotFoundException,
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
    Query findOne(Long containerId, Long databaseId, Long queryId, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, QueryNotFoundException, QueryStoreException, UserNotFoundException;

    /**
     * Inserts a query and metadata to the query store of a given database id
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param metadata    The statement.
     * @param principal   The user principal.
     * @return The stored query on success
     * @throws QueryStoreException         The query store raised some error
     * @throws DatabaseNotFoundException   The database id was not found in the metadata database
     * @throws ImageNotSupportedException  The image is not supported
     * @throws ContainerNotFoundException  The container was not found in the metadata database.
     * @throws UserNotFoundException       The user was not found in the metadata database.
     * @throws DatabaseConnectionException The database connection to the remote container failed.
     * @throws TableMalformedException     The table is malformed and the tuple could not be inserted.
     */
    Query insert(Long containerId, Long databaseId, ExecuteStatementDto metadata, Principal principal) throws QueryStoreException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, UserNotFoundException,
            DatabaseConnectionException, TableMalformedException;

    /**
     * Persists a query to be displayed in the frontend
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param queryId     The query id.
     * @param principal   The user principal.
     * @return The stored query on success.
     * @throws DatabaseNotFoundException   The database id was not found in the metadata database
     * @throws ImageNotSupportedException  The image is not supported
     * @throws DatabaseConnectionException The database connection to the remote container failed.
     * @throws QueryStoreException         The query store raised some error
     */
    Query persist(Long containerId, Long databaseId, Long queryId, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, QueryStoreException, UserNotFoundException;

}
