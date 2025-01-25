package at.tuwien.service;

import at.tuwien.api.database.table.CreateTableDto;
import at.tuwien.api.database.table.TableUpdateDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;

import java.security.Principal;

public interface TableService {

    /**
     * Find a table in the metadata database by database and table id.
     *
     * @param database The database.
     * @param tableId  The table id.
     * @return The table, if successful.
     */
    Table findById(Database database, Long tableId) throws TableNotFoundException, DatabaseNotFoundException;

    /**
     * Find a table in the metadata database by database id and table name.
     *
     * @param database     The database.
     * @param internalName The table name.
     * @return The table, if successful.
     */
    Table findByName(Database database, String internalName) throws TableNotFoundException, DatabaseNotFoundException;


    /**
     * Creates a table for a database id with given schema as data
     *
     * @param database  The database.
     * @param createDto The schema (as data).
     * @param principal The principal.
     * @return The created table.
     */
    Table createTable(Database database, CreateTableDto createDto, Principal principal)
            throws TableNotFoundException, DataServiceException, DataServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException, OntologyNotFoundException, SemanticEntityNotFoundException;

    /**
     * Deletes a table from the database in the metadata database and data database.
     *
     * @param table The table.
     */
    void deleteTable(Table table) throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException, SearchServiceConnectionException;

    Table updateTable(Table table, TableUpdateDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    TableColumn update(TableColumn column, ColumnSemanticsUpdateDto updateDto) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException, MalformedException, OntologyNotFoundException, SemanticEntityNotFoundException;

    TableColumn findColumnById(Table table, Long columnId) throws MalformedException;

    void updateStatistics(Table table) throws SearchServiceException, DatabaseNotFoundException, SearchServiceConnectionException, MalformedException, TableNotFoundException, DataServiceException, DataServiceConnectionException;
}
