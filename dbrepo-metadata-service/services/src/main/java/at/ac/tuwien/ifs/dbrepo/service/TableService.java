package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.security.Principal;
import java.util.UUID;

public interface TableService {

    /**
     * Find a table in the metadata database by database and table id.
     *
     * @param database The database.
     * @param tableId  The table id.
     * @return The table, if successful.
     * @throws TableNotFoundException    The table was not found in the metadata service.
     * @throws DatabaseNotFoundException The database was not found in the metadata service.
     */
    Table findById(Database database, UUID tableId) throws TableNotFoundException, DatabaseNotFoundException;

    /**
     * Find a table in the metadata database by database id and table name.
     *
     * @param database     The database.
     * @param internalName The table name.
     * @return The table, if successful.
     * @throws TableNotFoundException    The table was not found in the metadata service.
     * @throws DatabaseNotFoundException The database was not found in the metadata service.
     */
    Table findByName(Database database, String internalName) throws TableNotFoundException, DatabaseNotFoundException;


    /**
     *
     * Creates a table for a database id with given schema as data
     *
     * @param database  The database.
     * @param createDto The schema (as data).
     * @param principal The principal.
     * @return The created table.
     * @throws TableNotFoundException           The table was not found in the metadata service.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws UserNotFoundException            The user with this username was not found in the metadata database.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws TableExistsException             The table with this name exists in the target database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     * @throws MalformedException               The table is malformed, e.g. a column of a primary key constraint could not be found.
     * @throws OntologyNotFoundException        The ontology was not found in the metadata database.
     * @throws SemanticEntityNotFoundException  The semantic entity was not found in the metadata database.
     */
    Table createTable(Database database, CreateTableDto createDto, Principal principal) throws TableNotFoundException,
            DataServiceException, DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException,
            OntologyNotFoundException, SemanticEntityNotFoundException;

    /**
     * Deletes a given table from the database in the metadata database and data database.
     *
     * @param table The table.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws TableNotFoundException           The table was not found in the metadata service.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    void deleteTable(Table table) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, TableNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Updates a given table from the database in the metadata database.
     *
     * @param table The table.
     * @param data The update data.
     * @return The updated table, if successful.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws TableNotFoundException           The table was not found in the metadata service.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    Table updateTable(Table table, TableUpdateDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    /**
     * Updates a given table column in the metadata database.
     *
     * @param column The table column.
     * @param updateDto The update data.
     * @return The updated table column, if successful.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     * @throws MalformedException               The table is malformed, e.g. a column of a primary key constraint could not be found.
     * @throws OntologyNotFoundException        The ontology was not found in the metadata database.
     * @throws SemanticEntityNotFoundException  The semantic entity was not found in the metadata database.
     */
    TableColumn update(TableColumn column, ColumnSemanticsUpdateDto updateDto) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException, OntologyNotFoundException,
            SemanticEntityNotFoundException;

    /**
     * Find a table column by given table and column id.
     *
     * @param table The table.
     * @param columnId The column id.
     * @return The column, if found.
     * @throws MalformedException The table column was not found.
     */
    TableColumn findColumnById(Table table, UUID columnId) throws MalformedException;

    /**
     * Updates the table statistics by given table.
     *
     * @param table The table.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws DatabaseNotFoundException        The database was not found in the metadata service.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     * @throws MalformedException               The table is malformed, e.g. a column of a primary key constraint could not be found.
     * @throws TableNotFoundException           The table was not found in the metadata service.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     */
    void updateStatistics(Table table) throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, MalformedException, TableNotFoundException, DataServiceException,
            DataServiceConnectionException;
}
