package at.tuwien.service;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;

@Service
public interface QueryService {

    /**
     * Executes an arbitrary query on the database. We allow the user to only view the data, therefore the
     * default "mariadb" user is allowed read-only access "SELECT".
     *
     * @param databaseId    The database id.
     * @param statement     The query.
     * @param principal     The current user.
     * @param page          The page number.
     * @param size          The page size.
     * @param sortDirection The sorting direction.
     * @param sortColumn    The sorting column.
     * @return The result.
     * @throws QueryStoreException        The query store is not reachable.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws QueryMalformedException    The query is malformed.
     * @throws ColumnParseException       The column could not be parsed.
     * @throws UserNotFoundException      The user could not be found.
     * @throws TableMalformedException    The table is malformed.
     * @throws QueryNotFoundException     The query was not found in the query store.
     */
    QueryResultDto execute(Long databaseId, ExecuteStatementDto statement, Principal principal, Long page, Long size,
                           SortType sortDirection, String sortColumn) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryMalformedException, QueryStoreException, ColumnParseException,
            UserNotFoundException, TableMalformedException, QueryNotFoundException;

    /**
     * Re-Executes an arbitrary query on the database. We allow the user to only view the data, therefore the
     * default "mariadb" user is allowed read-only access "SELECT".
     *
     * @param databaseId    The database id.
     * @param query         The query.
     * @param page          The page number.
     * @param size          The page size.
     * @param sortDirection The sorting direction.
     * @param sortColumn    The sorting column.
     * @param principal     The user principal.
     * @return The result.
     * @throws QueryMalformedException    The query is malformed.
     * @throws DatabaseNotFoundException  The database was not found in the metdata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table is malformed.
     * @throws ColumnParseException       The column mapping/parsing failed.
     * @throws QueryMalformedException    The query is malformed.
     */
    QueryResultDto reExecute(Long databaseId, Query query, Long page, Long size, SortType sortDirection,
                             String sortColumn, Principal principal) throws QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ColumnParseException, TableMalformedException;

    /**
     * Re-Executes the count-statement of an arbitrary query on the database. We allow the user to only view
     * the data, therefore the default "mariadb" user is allowed read-only access "SELECT".
     *
     * @param databaseId The database id.
     * @param query      The query.
     * @param principal  The user principal.
     * @return The result.
     * @throws QueryStoreException        The query store is not reachable.
     * @throws QueryMalformedException    The query is malformed.
     * @throws DatabaseNotFoundException  The database was not found in the metdata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table is malformed.
     * @throws ColumnParseException       The column mapping/parsing failed.
     */
    Long reExecuteCount(Long databaseId, Query query, Principal principal)
            throws QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ColumnParseException,
            TableMalformedException, QueryStoreException;

    /**
     * Select all data known in the database-table id tuple at a given time and return a page of specific size, using
     * Instant to better abstract time concept (JDK 8) from SQL. We use the "mariadb" user for this.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param timestamp  The given time.
     * @param page       The page.
     * @param size       The page size.
     * @param principal  The user principal.
     * @return The select all data result
     * @throws TableNotFoundException     The table was not found in the metadata database.
     * @throws DatabaseNotFoundException  The database was not found in the metdata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table is malformed.
     * @throws QueryMalformedException    The query is malformed.
     */
    QueryResultDto tableFindAll(Long databaseId, Long tableId, Instant timestamp, Long page, Long size,
                                Principal principal) throws TableNotFoundException, DatabaseNotFoundException,
            TableMalformedException, QueryMalformedException, ImageNotSupportedException;

    /**
     * Select all data known in the database-table id tuple at a given time and return a downloadable input stream
     * resource at a given time. Instant to better abstract time concept (JDK 8) from SQL. We use the "mariadb" user
     * for this.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param timestamp  The given time.
     * @param principal  The user principal.
     * @return The select all data result in the form of a downloadable .csv file.
     * @throws TableNotFoundException    The table was not found in the metadata database.
     * @throws DatabaseNotFoundException The database was not found in the remote database.
     * @throws FileStorageException      The file could not be exported.
     * @throws QueryMalformedException   The query is malformed.
     * @throws DataDbSidecarException    The data database sidecar failed to produce the export resource.
     */
    ExportResource tableFindAll(Long databaseId, Long tableId, Instant timestamp, Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, FileStorageException, QueryMalformedException,
            DataDbSidecarException, DataProcessingException;

    /**
     * Select all data known in the view id tuple and return a page of specific size.
     * We use the "mariadb" user for this.
     *
     * @param databaseId The database id.
     * @param view       The view.
     * @param page       The page.
     * @param size       The page size.
     * @param principal  The user principal.
     * @return The select all data result
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     * @throws QueryMalformedException   The query is malformed.
     * @throws TableMalformedException   The table is malformed.
     */
    QueryResultDto viewFindAll(Long databaseId, View view, Long page, Long size, Principal principal)
            throws DatabaseNotFoundException, QueryMalformedException, TableMalformedException;

    /**
     * Finds one query by database id and query id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @param principal  The user principal.
     * @return The query result in the form  of a downloadable .csv file.
     * @throws DatabaseNotFoundException  The database was not found in the remote database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws FileStorageException       The file could not be exported.
     * @throws QueryStoreException        The query store is not reachable.
     * @throws QueryNotFoundException     THe query was not found in the query store.
     * @throws QueryMalformedException    The query is malformed.
     * @throws DataDbSidecarException     The data database sidecar failed to produce the export resource.
     */
    ExportResource findOne(Long databaseId, Long queryId, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, FileStorageException, QueryStoreException, QueryNotFoundException,
            QueryMalformedException, DataDbSidecarException, DataProcessingException;

    /**
     * Count the total tuples for a given table id within a database id at a given time.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param timestamp  The time.
     * @param principal  The user principal.
     * @return The number of records, if successful
     * @throws DatabaseNotFoundException  The database was not found in the remote database.
     * @throws TableNotFoundException     The table was not found in the metadata database.
     * @throws TableMalformedException    The table columns are messed up what we got from the metadata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws QueryMalformedException    The query is malformed.
     * @throws QueryStoreException        The query store could not retrieve.
     */
    Long tableCount(Long databaseId, Long tableId, Instant timestamp, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, ImageNotSupportedException,
            QueryMalformedException, QueryStoreException, TableMalformedException;

    /**
     * Count the total tuples for a given table id within a database id at a given time.
     *
     * @param databaseId The database id.
     * @param view       The view.
     * @param principal  The user principal.
     * @return The number of records, if successful
     * @throws DatabaseNotFoundException  The database was not found in the remote database.
     * @throws TableMalformedException    The view columns are messed up what we got from the metadata database.
     * @throws ImageNotSupportedException The image is not supported.
     */
    Long viewCount(Long databaseId, View view, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryMalformedException, QueryStoreException, TableMalformedException;

    /**
     * Insert data from AMQP client into a table of a table-database id tuple, we need the "root" role for this as the
     * default "mariadb" user is configured to only be allowed to execute "SELECT" statements.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param data       The data.
     * @param principal  The user principal.
     * @throws TableMalformedException   The table does not exist in the metadata database.
     * @throws DatabaseNotFoundException The database is not found in the metadata database.
     * @throws TableNotFoundException    The table is not found in the metadata database.
     */
    void insert(Long databaseId, Long tableId, TableCsvDto data, Principal principal) throws TableMalformedException,
            DatabaseNotFoundException, TableNotFoundException;

    /**
     * Deletes a tuple by given constraint set
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param data       The constraint set.
     * @param principal  The user principal.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws TableMalformedException    The table does not exist in the metadata database.
     * @throws DatabaseNotFoundException  The database is not found in the metadata database.
     * @throws TableNotFoundException     The table is not found in the metadata database.
     * @throws QueryMalformedException    The query is malformed.
     */
    void delete(Long databaseId, Long tableId, TableCsvDeleteDto data, Principal principal)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, QueryMalformedException;

    /**
     * Insert data from a csv into a table of a table-database id tuple, we need the "root" role for this as the
     * default "mariadb" user is configured to only be allowed to execute "SELECT statements.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param data       The data path.
     * @param principal  The user principal.
     * @throws TableMalformedException   The table does not exist in the metadata database.
     * @throws DatabaseNotFoundException The database is not found in the metadata database.
     * @throws TableNotFoundException    The table is not found in the metadata database.
     * @throws DataDbSidecarException    The data database sidecar failed to import the dataset.
     */
    void insert(Long databaseId, Long tableId, ImportDto data, Principal principal) throws TableMalformedException,
            DatabaseNotFoundException, TableNotFoundException, DataDbSidecarException, DataProcessingException;
}
