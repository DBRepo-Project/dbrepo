package at.tuwien.service;

import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.exception.TableNotFoundException;
import org.springframework.transaction.annotation.Transactional;

public interface TableColumnService {

    /**
     * Updates a table column
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param columnId   The column id.
     * @param updateDto  The update data containing unit and concept uris.
     * @return The updated table column, if successful.
     * @throws TableNotFoundException    The table was not found in the metadata database.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     * @throws TableMalformedException   The table seems malformed by the mapper.
     * @throws TableNotFoundException    The table is not found.
     */
    TableColumn update(Long databaseId, Long tableId, Long columnId, ColumnSemanticsUpdateDto updateDto)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException;

    /**
     * Finds a column in a given table with column id
     *
     * @param table    The table.
     * @param columnId The column id.
     * @return The column, if successful.
     * @throws TableMalformedException The requested column was not found in the table.
     */
    TableColumn findColumn(Table table, Long columnId) throws TableMalformedException;

    /**
     * Finds a column in a given table with column name.
     *
     * @param table The table.
     * @param name  The column name.
     * @return The column, if successful.
     * @throws TableMalformedException The requested column was not found in the table.
     */
    TableColumn findColumn(Table table, String name) throws TableMalformedException;

    /**
     * Finds a column in a database with given table name and given column name.
     *
     * @param database   The database.
     * @param tableName  The table name.
     * @param columnName The column name.
     * @return The column, if successful.
     * @throws TableMalformedException The requested column was not found in the database.
     */
    TableColumn findColumn(Database database, String tableName, String columnName) throws TableMalformedException;
}
