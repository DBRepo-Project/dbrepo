package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.exception.*;
import jakarta.validation.constraints.NotNull;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public interface ViewService {

    /**
     * Gets the metadata schema for a given database.
     *
     * @param database The database.
     * @return The list of view metadata.
     * @throws SQLException               The connection to the data database was unsuccessful.
     * @throws DatabaseMalformedException The columns that are referenced in the views are unknown to the Metadata Database. Call {@link TableService#getSchemas(PrivilegedDatabaseDto)} beforehand.
     * @throws ViewNotFoundException      The view with given name was not found.
     */
    List<ViewDto> getSchemas(PrivilegedDatabaseDto database) throws SQLException, DatabaseMalformedException,
            ViewNotFoundException;

    /**
     * Creates a view in the given data database.
     *
     * @param database The data database.
     * @param data     The view.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    ViewDto create(PrivilegedDatabaseDto database, ViewCreateDto data) throws SQLException,
            ViewMalformedException;

    /**
     * Get data from the given view at specific timestamp, paginated by page and size.
     *
     * @param view      The view.
     * @param timestamp The timestamp.
     * @param page      The page number.
     * @param size      The page size.
     * @return The data, if successful.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    QueryResultDto data(PrivilegedViewDto view, Instant timestamp, Long page, Long size) throws SQLException,
            ViewMalformedException;

    /**
     * Deletes a view.
     *
     * @param view The view.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    void delete(PrivilegedViewDto view) throws SQLException, ViewMalformedException;

    /**
     * Counts tuples in a view at system-versioned timestamp.
     *
     * @param view      The view.
     * @param timestamp The system-versioned timestamp.
     * @return The number of tuples.
     * @throws SQLException            The connection to the data database was unsuccessful.
     * @throws QueryMalformedException The query is malformed and was rejected by the data database.
     */
    Long count(PrivilegedViewDto view, Instant timestamp) throws SQLException, QueryMalformedException;

    /**
     * Exports view data into a dataset.
     *
     * @param view The view.
     * @return The dataset.
     * @throws QueryMalformedException     The query is malformed and was rejected by the data database.
     * @throws StorageUnavailableException Failed to establish a connection with the Storage Service.
     * @throws ViewNotFoundException       The view with given name was not found.
     */
    ExportResourceDto exportDataset(PrivilegedViewDto view) throws QueryMalformedException,
            StorageUnavailableException, ViewNotFoundException, MalformedException;

    /**
     * Get data from a given view.
     *
     * @param view The view.
     * @return The data.
     * @throws ViewNotFoundException   The view with given name was not found.
     * @throws QueryMalformedException The query is malformed and was rejected by the data database.
     */
    Dataset<Row> getData(@NotNull PrivilegedViewDto view) throws ViewNotFoundException,
            QueryMalformedException;
}
