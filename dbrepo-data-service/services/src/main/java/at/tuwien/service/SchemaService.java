package at.tuwien.service;

import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.exception.*;

import java.sql.SQLException;

public interface SchemaService {

    /**
     * Inspects the schema (columns with names, data types, unique-, check-, primary- and foreign key constraints) of
     * a table with given name in the given database.
     * @param database The database.
     * @param tableName The table name.
     * @return The inspected table if successful.
     * @throws SQLException The connection to the database could not be established.
     * @throws TableNotFoundException The table was not found in the given database.
     */
    TableDto inspectTable(PrivilegedDatabaseDto database, String tableName) throws SQLException,
            TableNotFoundException;

    /**
     * Inspects the schema (columns with names, data types) of a view with given name in the given database.
     * @param database The database.
     * @param viewName The table name.
     * @return The inspected view if successful.
     * @throws SQLException The connection to the database could not be established.
     * @throws ViewNotFoundException The view was not found in the given database.
     */
    ViewDto inspectView(PrivilegedDatabaseDto database, String viewName) throws SQLException, ViewNotFoundException;
}
