package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface TableService {

    /**
     * Generate table statistic for a given table. Only numerical columns are calculated.
     *
     * @param tableName The table name.
     * @return The table statistic, if successful.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws TableMalformedException The table statistic generation was unsuccessful, likely due to a bug in the mapping.
     * @throws TableNotFoundException  The table could not be inspected in the data database.
     */
    TableStatisticDto getStatistics(DatabaseDto database, String tableName) throws SQLException,
            TableMalformedException, TableNotFoundException;

    /**
     * Updating table description.
     *
     * @param table The table.
     * @param data  The description.
     * @throws SQLException            Query statement is malformed.
     * @throws TableMalformedException The table schema is malformed.
     * @throws TableNotFoundException  The table could not be inspected in the metadata database.
     */
    void updateTable(DatabaseDto database, TableDto table, TableUpdateDto data) throws SQLException,
            TableMalformedException, TableNotFoundException;

    /**
     * Drops a table in given table object.
     *
     * @param table The table object.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The drop table query is malformed.
     * @throws TableNotFoundException The table could not be found in the data database.
     */
    void delete(DatabaseDto database, TableDto table) throws SQLException, QueryMalformedException, TableNotFoundException;

    /**
     * Obtains the table history for a given table object.
     *
     * @param table The table object.
     * @param size  The maximum size.
     * @return The table history.
     * @throws SQLException           Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException The table could not be found in the data database.
     */
    List<TableHistoryDto> history(DatabaseDto database, TableDto table, Long size) throws SQLException, TableNotFoundException;

    /**
     * Obtains the table data tuples count at time.
     *
     * @param tableName     The table name.
     * @param timestamp The timestamp.
     * @return Number of tuples, if successful.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The count query is malformed, likely due to a bug in the application.
     */
    Long getCount(DatabaseDto database, String tableName, Instant timestamp) throws SQLException,
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
    void importDataset(DatabaseDto database, TableDto table, ImportDto data) throws MalformedException, StorageNotFoundException,
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
    void deleteTuple(DatabaseDto database, TableDto table, TupleDeleteDto data) throws SQLException,
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
    void createTuple(DatabaseDto database, TableDto table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException;

    /**
     * Creates a tuple in a table and returns the created tuple including its versioning timestamps.
     *
     * <p>For system-versioned tables, the returned DTO contains the tuple data plus the timestamps
     * {@code insertedAt} and {@code deletedAt} representing the MariaDB ROW_START and ROW_END values
     * (deletedAt will be null for active rows).</p>
     *
     * @param database The database.
     * @param table    The table.
     * @param data     The tuple.
     * @return A DTO containing the created tuple data and timestamps.
     * @throws SQLException                Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException     The create or select query is malformed.
     * @throws TableMalformedException     The tuple is malformed and does not fit the table schema.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws StorageNotFoundException    The storage service was not able to find the dataset for import.
     */
    TupleWithTimestampsDto createTupleWithTimestamps(DatabaseDto database, TableDto table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException;

    /**
     * Deletes a tuple and returns the versioned row including ROW_START/ROW_END as timestamps.
     */
    TupleWithTimestampsDto deleteTupleWithTimestamps(DatabaseDto database, TableDto table, TupleDeleteDto data) throws SQLException,
            QueryMalformedException, TableMalformedException;

    /**
     * Updates a tuple in a table.
     *
     * @param table The table.
     * @param data  The tuple.
     * @throws SQLException            Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The update query is malformed, likely due to a bug in the application.
     * @throws TableMalformedException The tuple is malformed and does not fit the table schema.
     */
    void updateTuple(DatabaseDto database, TableDto table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException;

    /**
     * Processes and persists replication timestamps received from other sites.
     *
     * @param database The database.
     * @param table The table.
     * @param timestamps The list of timestamps to process.
     * @throws SQLException Failed to parse SQL query, contains invalid syntax.
     * @throws QueryMalformedException The query is malformed, likely due to a bug in the application.
     */
    void processReplicationTimestamps(DatabaseDto database, TableDto table,
                                    List<at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto> timestamps) throws SQLException, QueryMalformedException;

    /**
     * Checks for tuples inserted after a given timestamp across all tables in a database.
     *
     * @param database The database to check.
     * @param timestamp The timestamp to check against.
     * @param replicaDatabaseId The ID of the replica database.
     * @return true if there are new tuples after the timestamp, false otherwise.
     * @throws SQLException Failed to establish connection to database.
     * @throws QueryMalformedException The count query is malformed.
     */
    boolean checkTuplesAfterTimestamp(DatabaseDto database, 
                                    java.time.Instant timestamp, 
                                    String replicaDatabaseId) throws SQLException, 
                                    QueryMalformedException;

    /**
     * Loads all tuples inserted after a given timestamp across all tables in a database.
     *
     * @param database  The database to load from.
     * @param timestamp The timestamp to check against.
     * @return A TuplesAndReplicationTimestampsResponse object containing both new tuples with timestamps and replication timestamps.
     * @throws SQLException            Failed to establish connection to database.
     * @throws QueryMalformedException The query is malformed.
     */
    ReplicationSynchronisationDataDto loadNewTuplesAfterTimestamp(DatabaseDto database,
                                                                  Instant timestamp) throws SQLException,
                                                        QueryMalformedException;
}
