package at.tuwien.service;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;

public interface TableService {

    /**
     * Find a table in the metadata database by database-table id tuple
     *
     * @param databaseId  The database id.
     * @param tableId     The table id.
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
     * @param databaseId  The database id.
     * @param tableId     The table id.
     * @param principal   The user principal.
     * @return The history as a list, if successful.
     * @throws QueryMalformedException   The query is malformed.
     * @throws DatabaseNotFoundException The database is not found.
     * @throws TableNotFoundException    The table is not found.
     */
    List<TableHistoryDto> findHistory(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, QueryMalformedException, TableNotFoundException, DatabaseConnectionException, QueryStoreException, UserNotFoundException;
}
