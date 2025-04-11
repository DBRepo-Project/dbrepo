package at.ac.tuwien.ac.at.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.internal.TableCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.SQLException;
import java.util.List;

public interface DatabaseService {

    /**
     * Inspects the schema (columns with names, data types) of a view with given name in the given database.
     * @param database The database.
     * @param viewName The view name.
     * @return The inspected view if successful.
     * @throws SQLException The connection to the database could not be established.
     * @throws ViewNotFoundException The view was not found in the given database.
     */
    ViewDto inspectView(DatabaseDto database, String viewName) throws SQLException, ViewNotFoundException;

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
    TableDto createTable(DatabaseDto database, TableCreateDto data) throws SQLException,
            TableMalformedException, TableExistsException, TableNotFoundException;

    /**
     * Creates a view in given data database with view definition.
     * @param database The data database object.
     * @param viewName The view name.
     * @param query The view query.
     * @return The generated view.
     * @throws SQLException
     * @throws ViewMalformedException
     */
    ViewDto createView(DatabaseDto database, String viewName, String query) throws SQLException,
            ViewMalformedException;

    /**
     * Gets the metadata schema for a given database.
     *
     * @param database The database.
     * @return The list of view metadata.
     * @throws SQLException               The connection to the data database was unsuccessful.
     * @throws DatabaseMalformedException The columns that are referenced in the views are unknown to the Metadata Database. Call {@link TableService#getSchemas(DatabaseDto)} beforehand.
     * @throws ViewNotFoundException      The view with given name was not found.
     */
    List<ViewDto> exploreViews(DatabaseDto database) throws SQLException, DatabaseMalformedException,
            ViewNotFoundException;

    /**
     * Get table schemas from the information_schema in the data database.
     *
     * @param database The data database  object.
     * @return List of tables, if successful.
     * @throws SQLException               Failed to parse SQL query, contains invalid syntax.
     * @throws TableNotFoundException     The table could not be inspected in the data database.
     * @throws DatabaseMalformedException The database inspection was unsuccessful, likely due to a bug in the mapping.
     */
    List<TableDto> exploreTables(DatabaseDto database) throws SQLException, TableNotFoundException,
            DatabaseMalformedException;

    /**
     * Inspects the schema (columns with names, data types, unique-, check-, primary- and foreign key constraints) of
     * a table with given name in the given database.
     *
     * @param database The database.
     * @param tableName The table name.
     * @return The inspected table if successful.
     * @throws SQLException The connection to the database could not be established.
     * @throws TableNotFoundException The table was not found in the given database.
     */
    TableDto inspectTable(DatabaseDto database, String tableName) throws SQLException, TableNotFoundException;

    /**
     * Updates a user's password in a given database.
     * @param database The database.
     * @param data The user-password tuple.
     * @throws SQLException The connection to the database could not be established.
     * @throws DatabaseMalformedException The database schema is malformed.
     */
    void update(DatabaseDto database, UpdateUserPasswordDto data) throws SQLException,
            DatabaseMalformedException;
}
