package at.tuwien.service;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

public interface TableService {

    /**
     * Find a table in the metadata database by database and table id.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @return The table, if successful.
     */
    Table findById(Long databaseId, Long tableId) throws TableNotFoundException, DatabaseNotFoundException;

    /**
     * Find a table in the metadata database by database id and table name.
     *
     * @param databaseId   The database id.
     * @param internalName The table name.
     * @return The table, if successful.
     */
    Table findByName(Long databaseId, String internalName) throws TableNotFoundException, DatabaseNotFoundException;


    /**
     * Creates a table for a database id with given schema as data
     *
     * @param database  The database.
     * @param createDto The schema (as data).
     * @param principal The principal.
     * @return The created table.
     */
    Table createTable(Database database, TableCreateDto createDto, Principal principal)
            throws TableNotFoundException, ServiceException, ServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException;

    /**
     * Deletes a table from the database in the metadata database and data database.
     *
     * @param table The table.
     */
    void deleteTable(Table table) throws ServiceException, ServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException, SearchServiceConnectionException;

    TableColumn update(TableColumn column, ColumnSemanticsUpdateDto updateDto) throws ServiceException,
            ServiceConnectionException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException, MalformedException, OntologyNotFoundException, SemanticEntityNotFoundException;

    TableColumn findColumnById(Table table, Long columnId) throws MalformedException;

    TableColumn findColumnByName(Table table, String name) throws MalformedException;

    @Transactional
    void updateStatistics(Table table, TableStatisticDto data) throws MalformedException, SearchServiceException, DatabaseNotFoundException, SearchServiceConnectionException;
}
