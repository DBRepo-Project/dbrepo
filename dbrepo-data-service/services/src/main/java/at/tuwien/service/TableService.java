package at.tuwien.service;

import at.tuwien.api.SortTypeDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.exception.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public interface TableService {

    /**
     * Get table schemas from the information_schema in the data database.
     *
     * @param database The data database privileged object.
     * @return List of tables, if successful.
     * @throws SQLException               Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException     The table could not be inspected in the data database.
     * @throws DatabaseMalformedException The database inspection was unsuccessful, likely due to a bug in the mapping.
     */
    List<TableDto> getSchemas(PrivilegedDatabaseDto database) throws SQLException, TableNotFoundException,
            DatabaseMalformedException;

    /**
     * Generate table statistic for a given table. Only numerical columns are calculated.
     *
     * @param table The table.
     * @return The table statistic, if successful.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws TableMalformedException The table statistic generation was unsuccessful, likely due to a bug in the mapping.
     * @throws TableNotFoundException  The table could not be inspected in the data database.
     */
    TableStatisticDto getStatistics(PrivilegedTableDto table) throws SQLException, TableMalformedException,
            TableNotFoundException;

    /**
     * Finds a table with given data database and table name.
     *
     * @param database  The data database.
     * @param tableName The table name.
     * @return The table, if successful.
     * @throws TableNotFoundException  The table could not be inspected in the data database.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The inspection query is malformed.
     */
    TableDto find(PrivilegedDatabaseDto database, String tableName) throws TableNotFoundException, SQLException,
            QueryMalformedException;

    /**
     * Creates a table in given data database with table definition.
     *
     * @param database The data database privileged object.
     * @param data     The table definition.
     * @return The created table, if successful.
     * @throws SQLException            Query statement is malformed.
     * @throws TableMalformedException The table schema is malformed.
     * @throws TableExistsException    The table name already exists in the information_schema.
     * @throws TableNotFoundException  The table could not be inspected in the metadata database.
     */
    TableDto createTable(PrivilegedDatabaseDto database, TableCreateDto data) throws SQLException,
            TableMalformedException, TableExistsException, TableNotFoundException;

    /**
     * Updating table description.
     *
     * @param table The table.
     * @param data  The description.
     * @throws SQLException            Query statement is malformed.
     * @throws TableMalformedException The table schema is malformed.
     * @throws TableNotFoundException  The table could not be inspected in the metadata database.
     */
    void updateTable(PrivilegedTableDto table, TableUpdateDto data) throws SQLException,
            TableMalformedException, TableNotFoundException;

    /**
     * Drops a table in given table object.
     *
     * @param table The table object.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The drop table query is malformed.
     */
    void delete(PrivilegedTableDto table) throws SQLException, QueryMalformedException;

    /**
     * Obtains the table history for a given table object.
     *
     * @param table The table object.
     * @param size  The maximum size.
     * @return The table history.
     * @throws SQLException           Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException The table could not be found in the data database.
     */
    List<TableHistoryDto> history(PrivilegedTableDto table, Long size) throws SQLException, TableNotFoundException;

    /**
     * Obtains the table data tuples count at time.
     *
     * @param table     The table object.
     * @param timestamp The timestamp.
     * @return Number of tuples, if successful.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The count query is malformed, likely due to a bug in the application.
     */
    Long getCount(PrivilegedTableDto table, Instant timestamp) throws SQLException,
            QueryMalformedException;

    /**
     * Imports a dataset into the database by given table. By default, an <code>upsert</code> operation is performed
     * that updates duplicate values (identified by their primary key) or inserts values if no duplicate is detected.
     *
     * @param table The table.
     * @param data  The dataset metadata.
     * @throws MalformedException          The dataset is malformed.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException     The import query is malformed, likely due to a bug in the application.
     */
    void importDataset(PrivilegedTableDto table, ImportDto data) throws MalformedException, StorageNotFoundException,
            StorageUnavailableException, SQLException, QueryMalformedException, TableMalformedException;

    /**
     * Imports a dataset by metadata into the sidecar of the target database by given table.
     *
     * @param table The table.
     * @param data  The dataset metadata.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws TableMalformedException The tuple is malformed and does not fit the table schema.
     * @throws QueryMalformedException The delete query is malformed, likely due to a bug in the application.
     */
    void deleteTuple(PrivilegedTableDto table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException;

    /**
     * Creates a tuple in a table.
     *
     * @param table The table.
     * @param data  The tuple.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException     The create query is malformed, likely due to a bug in the application.
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     */
    void createTuple(PrivilegedTableDto table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException;

    /**
     * Updates a tuple in a table.
     *
     * @param table The table.
     * @param data  The tuple.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The update query is malformed, likely due to a bug in the application.
     * @throws TableMalformedException The tuple is malformed and does not fit the table schema.
     */
    void updateTuple(PrivilegedTableDto table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException;

    Dataset<Row> getData(PrivilegedDatabaseDto database, String tableOrView, Instant timestamp,
                         Long page, Long size, SortTypeDto sortDirection, String sortColumn)
            throws QueryMalformedException, TableNotFoundException;
}
