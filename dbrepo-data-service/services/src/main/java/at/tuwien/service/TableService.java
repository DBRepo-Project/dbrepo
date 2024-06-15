package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.exception.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public interface TableService {

    /**
     * Get table schemas from the information_schema in the data database.
     * @param database The data database privileged object.
     * @return List of tables, if successful.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException The table could not be inspected in the data database.
     * @throws DatabaseMalformedException The database inspection was unsuccessful, likely due to a bug in the mapping.
     */
    List<TableDto> getSchemas(PrivilegedDatabaseDto database) throws SQLException, TableNotFoundException,
            DatabaseMalformedException;

    /**
     * Generate table statistic for a given table. Only numerical columns are calculated.
     * @param table The table.
     * @return The table statistic, if successful.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws TableMalformedException The table statistic generation was unsuccessful, likely due to a bug in the mapping.
     * @throws QueryMalformedException The inspection query is malformed.
     */
    TableStatisticDto getStatistics(PrivilegedTableDto table) throws SQLException, TableMalformedException,
            QueryMalformedException;

    /**
     * Finds a table with given data database and table name.
     * @param database The data database.
     * @param tableName The table name.
     * @return The table, if successful.
     * @throws TableNotFoundException The table could not be inspected in the data database.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The inspection query is malformed.
     */
    TableDto find(PrivilegedDatabaseDto database, String tableName) throws TableNotFoundException, SQLException,
            QueryMalformedException;

    /**
     * Creates a table in given data database with table definition.
     * @param database The data database privileged object.
     * @param data The table definition.
     * @return The created table, if successful.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException The table could not be inspected in the data database.
     * @throws TableExistsException The table name already exists in the information_schema.
     * @throws TableNotFoundException The table could not be inspected in the data database.
     */
    TableDto createTable(PrivilegedDatabaseDto database, TableCreateDto data) throws SQLException,
            TableMalformedException, TableExistsException, TableNotFoundException;

    /**
     * Drops a table in given table object.
     * @param table The table object.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The drop table query is malformed.
     */
    void delete(PrivilegedTableDto table) throws SQLException, QueryMalformedException;

    /**
     * Obtains data from a table with given table object at timestamp, loaded as page number and length size.
     * @param table The table object.
     * @param timestamp The timestamp.
     * @param page The page number.
     * @param size The page size/length.
     * @return The data.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws TableMalformedException The table schema is malformed, likely due to a bug in the application.
     */
    QueryResultDto getData(PrivilegedTableDto table, Instant timestamp, Long page, Long size) throws SQLException,
            TableMalformedException;

    /**
     * Obtains the table history for a given table object.
     * @param table The table object.
     * @param size The maximum size.
     * @return The table history.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException The table could not be found in the data database.
     */
    List<TableHistoryDto> history(PrivilegedTableDto table, Long size) throws SQLException, TableNotFoundException;

    /**
     * Obtains the table data tuples count at time.
     * @param table The table object.
     * @param timestamp The timestamp.
     * @return Number of tuples, if successful.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The count query is malformed, likely due to a bug in the application.
     */
    Long getCount(PrivilegedTableDto table, Instant timestamp) throws SQLException,
            QueryMalformedException;

    void importDataset(PrivilegedTableDto table, ImportCsvDto data) throws SidecarImportException,
            StorageNotFoundException, SQLException, QueryMalformedException, ServiceException, RemoteUnavailableException;

    void deleteTuple(PrivilegedTableDto table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException;

    void createTuple(PrivilegedTableDto table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException;

    void updateTuple(PrivilegedTableDto table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException;

    ExportResourceDto exportDataset(PrivilegedTableDto table, Instant timestamp)
            throws SQLException, SidecarExportException, StorageNotFoundException, StorageUnavailableException,
            QueryMalformedException, ServiceException, RemoteUnavailableException;
}
