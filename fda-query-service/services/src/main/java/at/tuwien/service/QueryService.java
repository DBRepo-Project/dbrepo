package at.tuwien.service;

import at.tuwien.ExportResource;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.Principal;
import java.time.Instant;

@Service
public interface QueryService {

    /**
     * Executes an arbitrary query on the database container. We allow the user to only view the data, therefore the
     * default "mariadb" user is allowed read-only access "SELECT".
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param statement   The query.
     * @param principal   The current user.
     * @param page        The page number.
     * @param size        The page size.
     * @return The result.
     * @throws QueryStoreException        The query store is not reachable.
     * @throws QueryMalformedException    The query is malformed.
     * @throws DatabaseNotFoundException  The database was not found in the metdata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     */
    QueryResultDto execute(Long containerId, Long databaseId, ExecuteStatementDto statement,
                           Principal principal, Long page, Long size)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, QueryStoreException,
            ContainerNotFoundException, ColumnParseException, UserNotFoundException, TableMalformedException, DatabaseConnectionException;

    /**
     * Re-Executes an arbitrary query on the database container. We allow the user to only view the data, therefore the
     * default "mariadb" user is allowed read-only access "SELECT".
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param query       The query.
     * @param page        The page number.
     * @param size        The page size.
     * @return The result.
     * @throws TableNotFoundException     The table was not found in the metadata database.
     * @throws QueryStoreException        The query store is not reachable.
     * @throws QueryMalformedException    The query is malformed.
     * @throws DatabaseNotFoundException  The database was not found in the metdata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     * @throws TableMalformedException    The table is malformed.
     * @throws ColumnParseException       The column mapping/parsing failed.
     */
    QueryResultDto reExecute(Long containerId, Long databaseId, Query query, Long page, Long size)
            throws TableNotFoundException, QueryStoreException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, TableMalformedException, ColumnParseException, DatabaseConnectionException;


    /**
     * Select all data known in the database-table id tuple at a given time and return a page of specific size, using
     * Instant to better abstract time concept (JDK 8) from SQL. We use the "mariadb" user for this.
     *
     * @param containerId The container-database id pair.
     * @param databaseId  The container-database id pair.
     * @param tableId     The table id.
     * @param timestamp   The given time.
     * @param page        The page.
     * @param size        The page size.
     * @return The select all data result
     * @throws TableNotFoundException     The table was not found in the metadata database.
     * @throws DatabaseNotFoundException  The database was not found in the metdata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     * @throws TableMalformedException    The table is malformed.
     */
    QueryResultDto findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp,
                           Long page, Long size) throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, TableMalformedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException;

    /**
     * Select all data known in the database-table id tuple at a given time and return a downloadable input stream
     * resource at a given time. Instant to better abstract time concept (JDK 8) from SQL. We use the "mariadb" user
     * for this.
     *
     * @param containerId The container-database id pair.
     * @param databaseId  The container-database id pair.
     * @param tableId     The table id.
     * @param timestamp   The given time.
     * @return The select all data result in the form of a downloadable .csv file.
     * @throws ContainerNotFoundException  The container was not found in the metadata database.
     * @throws TableNotFoundException      The table was not found in the metadata database.
     * @throws TableMalformedException     The table columns are messed up what we got from the metadata database.
     * @throws DatabaseNotFoundException   The database was not found in the remote database.
     * @throws ImageNotSupportedException  The image is not supported.
     * @throws DatabaseConnectionException The connection to the remote database was unsuccessful.
     * @throws FileStorageException        The file could not be exported.
     */
    ExportResource findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseConnectionException, TableMalformedException, PaginationException, ContainerNotFoundException,
            FileStorageException, QueryMalformedException;

    /**
     * Finds one query by container-database-query triple.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param queryId     The query id.
     * @return The query result in the form  of a downloadable .csv file.
     * @throws DatabaseNotFoundException  The database was not found in the remote database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table columns are messed up what we got from the metadata database.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     * @throws FileStorageException       The file could not be exported.
     * @throws QueryStoreException        The query store is not reachable.
     * @throws QueryNotFoundException     THe query was not found in the query store.
     */
    ExportResource findOne(Long containerId, Long databaseId, Long queryId)
            throws DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException,
            ContainerNotFoundException, FileStorageException, QueryStoreException, QueryNotFoundException, QueryMalformedException, DatabaseConnectionException;

    /**
     * Count the total tuples for a given table id within a container-database id tuple at a given time.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param tableId     The table id.
     * @param timestamp   The time.
     * @return The number of records, if successful
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     * @throws DatabaseNotFoundException  The database was not found in the remote database.
     * @throws TableNotFoundException     The table was not found in the metadata database.
     * @throws TableMalformedException    The table columns are messed up what we got from the metadata database.
     * @throws ImageNotSupportedException The image is not supported.
     */
    Long count(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws ContainerNotFoundException, DatabaseNotFoundException, TableNotFoundException,
            TableMalformedException, ImageNotSupportedException, DatabaseConnectionException, QueryMalformedException, QueryStoreException;

    void update(Long containerId, Long databaseId, Long tableId, TableCsvUpdateDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, DatabaseConnectionException, QueryMalformedException;

    /**
     * Insert data from AMQP client into a table of a table-database id tuple, we need the "root" role for this as the
     * default "mariadb" user is configured to only be allowed to execute "SELECT" statements.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param tableId     The table id.
     * @param data        The data.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table does not exist in the metadata database.
     * @throws DatabaseNotFoundException  The database is not found in the metadata database.
     * @throws TableNotFoundException     The table is not found in the metadata database.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     */
    void insert(Long containerId, Long databaseId, Long tableId, TableCsvDto data) throws ImageNotSupportedException,
            TableMalformedException, DatabaseNotFoundException, TableNotFoundException, ContainerNotFoundException, DatabaseConnectionException;

    /**
     * Deletes a tuple by given constraint set
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param tableId     The table id.
     * @param data        The constraint set.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table does not exist in the metadata database.
     * @throws DatabaseNotFoundException  The database is not found in the metadata database.
     * @throws TableNotFoundException     The table is not found in the metadata database.
     * @throws TupleDeleteException       The tuple was not deleted.
     */
    void delete(Long containerId, Long databaseId, Long tableId, TableCsvDeleteDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, TupleDeleteException, ContainerNotFoundException, DatabaseConnectionException, QueryMalformedException;

    /**
     * Insert data from a csv into a table of a table-database id tuple, we need the "root" role for this as the
     * default "mariadb" user is configured to only be allowed to execute "SELECT statements.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param data       The data path.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table does not exist in the metadata database.
     * @throws DatabaseNotFoundException  The database is not found in the metadata database.
     * @throws TableNotFoundException     The table is not found in the metadata database.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     */
    void insert(Long containerId, Long databaseId, Long tableId, ImportDto data) throws ImageNotSupportedException,
            TableMalformedException, DatabaseNotFoundException, TableNotFoundException, ContainerNotFoundException, DatabaseConnectionException, QueryMalformedException;
}
