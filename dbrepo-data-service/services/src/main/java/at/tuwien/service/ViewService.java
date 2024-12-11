package at.tuwien.service;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.ViewMalformedException;
import at.tuwien.exception.ViewNotFoundException;

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
     * Deletes a view.
     *
     * @param database The database.
     * @param viewName The view name.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    void delete(PrivilegedDatabaseDto database, String viewName) throws SQLException, ViewMalformedException;

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
}
