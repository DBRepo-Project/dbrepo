package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewNotFoundException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public interface ViewService {

    /**
     * Deletes a view.
     *
     * @param database The database.
     * @param view     The view.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The query is malformed and was rejected by the data database.
     */
    void delete(Database database, View view) throws SQLException, ViewMalformedException;

    /**
     * Inspects the schema (columns with names, data types) of a view with given name in the given database.
     *
     * @param database The database.
     * @param viewName The view name.
     * @return The inspected view if successful.
     * @throws SQLException          The connection to the database could not be established.
     * @throws ViewNotFoundException The view was not found in the given database.
     */
    ViewDto inspect(Database database, String viewName) throws SQLException, ViewNotFoundException;

    /**
     * Creates a view in given data database with view definition.
     *
     * @param database The data database object.
     * @param viewName The view name.
     * @param query    The view query.
     * @return The generated view.
     * @throws SQLException           The connection to the data database was unsuccessful.
     * @throws ViewMalformedException The view is malformed.
     */
    ViewDto create(Database database, String viewName, String query) throws SQLException,
            ViewMalformedException;

    /**
     * Gets the metadata schema for a given database.
     *
     * @param database The database.
     * @return The list of view metadata.
     * @throws SQLException               The connection to the data database was unsuccessful.
     * @throws DatabaseMalformedException The columns that are referenced in the views are unknown to the Metadata Database. Call {@link TableService#getSchemas(Database)} beforehand.
     * @throws ViewNotFoundException      The view with given name was not found.
     */
    List<ViewDto> explore(Database database) throws SQLException, DatabaseMalformedException,
            ViewNotFoundException;

    /**
     * Counts tuples in a view at system-versioned timestamp.
     *
     * @param database  The database.
     * @param view      The view.
     * @param timestamp The system-versioned timestamp.
     * @return The number of tuples.
     * @throws SQLException            The connection to the data database was unsuccessful.
     * @throws QueryMalformedException The query is malformed and was rejected by the data database.
     */
    Long count(Database database, View view, Instant timestamp) throws SQLException, QueryMalformedException;
}
