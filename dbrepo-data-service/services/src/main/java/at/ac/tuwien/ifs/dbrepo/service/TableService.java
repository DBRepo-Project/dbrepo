package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TableService {

    /**
     * Generate table statistic for a given table. Only numerical columns are calculated.
     *
     * @param database  The database name.
     * @param id        The table id.
     * @param tableName The table name.
     * @return The table statistic, if successful.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws TableMalformedException The table statistic generation was unsuccessful, likely due to a bug in the mapping.
     * @throws TableNotFoundException  The table could not be inspected in the data database.
     */
    TableStatisticDto getStatistics(Database database, UUID id, String tableName) throws SQLException,
            TableMalformedException, TableNotFoundException;

    /**
     * Creates a table in given data database with table definition.
     *
     * @param database The data database object.
     * @param data     The table definition.
     * @return The generated table.
     * @throws SQLException            Query statement is malformed.
     * @throws TableMalformedException The table schema is malformed.
     * @throws TableExistsException    The table name already exists in the information_schema.
     * @throws TableNotFoundException  The table could not be inspected in the metadata database.
     */
    TableDto create(Database database, CreateTableDto data) throws SQLException, TableMalformedException,
            TableExistsException, TableNotFoundException;

    /**
     * Updating table description.
     *
     * @param table The table.
     * @param data  The description.
     * @throws SQLException            Query statement is malformed.
     * @throws TableMalformedException The table schema is malformed.
     * @throws TableNotFoundException  The table could not be inspected in the metadata database.
     */
    void update(Database database, Table table, TableUpdateDto data) throws SQLException, TableMalformedException,
            TableNotFoundException;

    /**
     * Drops a table in given table object.
     *
     * @param table The table object.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The drop table query is malformed.
     * @throws TableNotFoundException  The table could not be found in the data database.
     */
    void delete(Database database, Table table) throws SQLException, QueryMalformedException, TableNotFoundException;

    /**
     * Obtains the table history for a given table object.
     *
     * @param table The table object.
     * @param size  The maximum size.
     * @return The table history.
     * @throws SQLException           Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException The table could not be found in the data database.
     */
    List<TableHistoryDto> history(Database database, Table table, Long size) throws SQLException, TableNotFoundException;

    /**
     * Obtains the table data tuples count at time.
     *
     * @param tableName The table name.
     * @param timestamp The timestamp.
     * @return Number of tuples, if successful.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The count query is malformed, likely due to a bug in the application.
     */
    Long getCount(Database database, String tableName, Instant timestamp) throws SQLException, QueryMalformedException;

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
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     */
    void importDataset(Database database, Table table, ImportDto data) throws MalformedException, StorageNotFoundException,
            StorageUnavailableException, SQLException, QueryMalformedException, TableMalformedException;

    /**
     * Imports a dataset by metadata into the sidecar of the target database by given table.
     *
     * @param table The table.
     * @param data  The dataset metadata.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     * @throws QueryMalformedException     The delete query is malformed, likely due to a bug in the application.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     */
    void deleteTuple(Database database, Table table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException;

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
    void createTuple(Database database, Table table, TupleDto data) throws SQLException, QueryMalformedException,
            TableMalformedException, StorageUnavailableException, StorageNotFoundException;

    /**
     * Creates a tuple and returns the stored tuple with MariaDB system-versioning timestamps.
     *
     * @param table The table.
     * @param data  The tuple.
     * @return The stored tuple with replication timestamps.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException     The create or read-back query is malformed.
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     */
    TupleWithTimestampsDto createTupleWithTimestamps(Database database, Table table, TupleDto data)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException;

    /**
     * Updates a tuple in a table.
     *
     * @param table The table.
     * @param data  The tuple.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException     The update query is malformed, likely due to a bug in the application.
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     */
    void updateTuple(Database database, Table table, TupleUpdateDto data) throws SQLException, QueryMalformedException,
            TableMalformedException, StorageUnavailableException, StorageNotFoundException;

    /**
     * Updates a tuple and returns the stored tuple with MariaDB system-versioning timestamps.
     *
     * @param table The table.
     * @param data  The tuple update.
     * @return The stored tuple with replication timestamps.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException     The update or read-back query is malformed.
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     */
    TupleWithTimestampsDto updateTupleWithTimestamps(Database database, Table table, TupleUpdateDto data)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException;

    /**
     * Deletes a tuple and returns the deleted version with MariaDB system-versioning timestamps.
     *
     * @param table The table.
     * @param data  The tuple delete keys.
     * @return The deleted tuple with replication timestamps.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException     The delete or read-back query is malformed.
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     */
    TupleWithTimestampsDto deleteTupleWithTimestamps(Database database, Table table, TupleDeleteDto data)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException;

    /**
     * Get table schemas from the information_schema in the data database.
     *
     * @param database The data database  object.
     * @return List of tables, if successful.
     * @throws SQLException               Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException     The table could not be inspected in the data database.
     * @throws DatabaseMalformedException The database inspection was unsuccessful, likely due to a bug in the mapping.
     */
    List<TableDto> explore(Database database) throws SQLException, TableNotFoundException,
            DatabaseMalformedException;

    /**
     * Inspects the schema (columns with names, data types, unique-, check-, primary- and foreign key constraints) of
     * a table with given name in the given database.
     *
     * @param database  The database.
     * @param tableName The table name.
     * @return The inspected table if successful.
     * @throws SQLException           The connection to the database could not be established.
     * @throws TableNotFoundException The table was not found in the given database.
     */
    TableDto inspect(Database database, String tableName) throws SQLException, TableNotFoundException;
}
