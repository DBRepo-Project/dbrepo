package at.tuwien.service;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;

public interface TableService {

    /**
     * Find a table in the metadata database by database-table id tuple
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @return The database.
     * @throws DatabaseNotFoundException The database is not found.
     * @throws TableNotFoundException    The table is not found.
     */
    Table find(Long databaseId, Long tableId) throws DatabaseNotFoundException, TableNotFoundException;

    /**
     * Finds all tables in the metdata database.
     *
     * @return The list of tables.
     */
    List<Table> findAll();

    /**
     * Find the table history.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param principal  The user principal.
     * @return The history as a list, if successful.
     * @throws QueryMalformedException   The query is malformed.
     * @throws DatabaseNotFoundException The database is not found.
     * @throws TableNotFoundException    The table is not found.
     */
    List<TableHistoryDto> findHistory(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, QueryMalformedException, TableNotFoundException,
            DatabaseConnectionException, QueryStoreException, UserNotFoundException;

    /**
     * Select all tables from the metadata database.
     *
     * @param databaseId The database id.
     * @return The list of tables.
     */
    List<Table> findAll(Long databaseId) throws DatabaseNotFoundException;

    /**
     * Deletes a table for a fiven database-table id pair.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @throws TableNotFoundException     The table was not found in the metadata database.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws DataProcessingException    The deletion did not work.
     */
    void deleteTable(Long databaseId, Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, DataProcessingException, ContainerNotFoundException, TableMalformedException,
            QueryMalformedException;

    /**
     * Find a table by database-table id pair
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @return The table.
     * @throws TableNotFoundException    The table was not found in the metadata database.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Table findById(Long databaseId, Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException;


    /**
     * Creates a table for a database id with given schema as data
     *
     * @param databaseId The database id.
     * @param createDto  The schema (as data).
     * @param principal  The principal.
     * @return The created table.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws TableNameExistsException   The table name exists already in this database.
     * @throws ContainerNotFoundException The container was not found.
     * @throws TableMalformedException    The table seems malformed by the mapper.
     */
    Table createTable(Long databaseId, TableCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, DatabaseNotFoundException, TableMalformedException,
            TableNameExistsException, ContainerNotFoundException, UserNotFoundException, QueryMalformedException;


    /**
     * Updates a table column
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param columnId   The column id.
     * @param updateDto  The update data containing unit and concept uris.
     * @return The updated table column, if successful.
     * @throws TableNotFoundException     The table was not found in the metadata database.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws ContainerNotFoundException The container was not found.
     * @throws TableMalformedException    The table seems malformed by the mapper.
     */
    TableColumn update(Long databaseId, Long tableId, Long columnId,
                       ColumnSemanticsUpdateDto updateDto, String authorization)
            throws TableNotFoundException, DatabaseNotFoundException, ContainerNotFoundException,
            TableMalformedException, SemanticEntityPersistException, SemanticEntityNotFoundException,
            QueryMalformedException;
}
