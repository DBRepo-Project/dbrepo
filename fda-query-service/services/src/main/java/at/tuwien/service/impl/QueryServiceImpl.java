package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.querystore.Query;
import at.tuwien.service.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QueryServiceImpl extends HibernateConnector implements QueryService {

    private final QueryMapper queryMapper;
    private final TableService tableService;
    private final StoreService storeService;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;

    @Autowired
    public QueryServiceImpl(QueryMapper queryMapper, TableService tableService,
                            DatabaseService databaseService, StoreService storeService, DatabaseMapper databaseMapper) {
        this.queryMapper = queryMapper;
        this.tableService = tableService;
        this.storeService = storeService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto execute(Long containerId, Long databaseId, ExecuteStatementDto statement,
                                  Principal principal, Long page, Long size, SortType sortDirection, String sortColumn)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, QueryStoreException,
            ContainerNotFoundException, ColumnParseException, UserNotFoundException, DatabaseConnectionException,
            TableMalformedException {
        if (statement.getStatement().contains(";")) {
            log.error("Failed to execute query since it contains ';'");
            throw new QueryMalformedException("Failed to execute query since it contains ';'");
        }
        final Query query = storeService.insert(containerId, databaseId, statement, principal);
        return reExecute(containerId, databaseId, query, page, size, sortDirection, sortColumn, principal);
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto reExecute(Long containerId, Long databaseId, Query query, Long page, Long size,
                                    SortType sortDirection, String sortColumn, Principal principal)
            throws QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ColumnParseException,
            TableMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* map the result to the tables (with respective columns) from the statement metadata */
        final List<TableColumn> columns;
        try {
            columns = parseColumns(query.getQuery(), database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new ColumnParseException("Failed to map/parse columns: " + e.getMessage(), e);
        }
        final QueryResultDto dto;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.queryToRawTimestampedQuery(connection, query.getQuery(),
                    database, query.getCreated(), true, page, size);
            final ResultSet resultSet = preparedStatement.executeQuery();
            dto = queryMapper.resultListToQueryResultDto(columns, resultSet);
        } catch (SQLException e) {
            log.error("Failed to execute and map time-versioned query: {}", e.getMessage());
            throw new TableMalformedException("Failed to execute and map time-versioned query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        dto.setId(query.getId());
        dto.setResultNumber(query.getResultNumber());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp, Long page,
                                  Long size, Principal principal) throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, TableMalformedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        final QueryResultDto result;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableToRawFindAllQuery(connection, table, timestamp, size, page);
            final ResultSet resultSet = preparedStatement.executeQuery();
            result = queryMapper.queryTableToQueryResultDto(resultSet, table);
        } catch (DateTimeException e) {
            log.error("Failed to parse date from the one stored in the metadata database: {}", e.getMessage());
            throw new TableMalformedException("Could not parse date from format: " + e.getMessage(), e);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp, Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseConnectionException, TableMalformedException, PaginationException, ContainerNotFoundException,
            FileStorageException, QueryMalformedException, UserNotFoundException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* read file */
        final InputStreamResource resource;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableToRawExportQuery(connection, table, timestamp, filename);
            preparedStatement.executeUpdate();
            final File file = new File("/tmp/" + filename);
            resource = new InputStreamResource(FileUtils.openInputStream(file));
            FileUtils.forceDelete(file);
        } catch (IOException | SQLException e) {
            log.error("Failed to execute query and/or export file: {}", e.getMessage());
            throw new FileStorageException("Failed to execute query and/or export file: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return ExportResource.builder()
                .resource(resource)
                .filename(filename)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource findOne(Long containerId, Long databaseId, Long queryId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, FileStorageException, QueryStoreException,
            QueryNotFoundException, QueryMalformedException, DatabaseConnectionException, UserNotFoundException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* read file */
        final InputStreamResource resource;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.queryToRawExportQuery(connection, query, filename);
            preparedStatement.executeUpdate();
            final File file = new File("/tmp/" + filename);
            resource = new InputStreamResource(FileUtils.openInputStream(file));
            if (!FileUtils.deleteQuietly(file)) {
                log.warn("Failed to delete exported file");
            }
        } catch (SQLException e) {
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryStoreException("Failed to execute query: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to export query: {}", e.getMessage());
            throw new FileStorageException("Failed to export query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return ExportResource.builder()
                .resource(resource)
                .filename(filename)
                .build();
    }

    @Override
    @Transactional
    public Long count(Long containerId, Long databaseId, Long tableId, Instant timestamp, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, ImageNotSupportedException,
            QueryMalformedException, QueryStoreException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableToRawCountAllQuery(connection, table, timestamp);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to count raw tuples: {}", e.getMessage());
            throw new TableMalformedException("Failed to count raw tuples: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void update(Long containerId, Long databaseId, Long tableId, TableCsvUpdateDto data, Principal principal)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, QueryMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        if (data.getData().size() == 0 || data.getKeys().size() == 0) return;
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableCsvDtoToRawUpdateQuery(connection, table, data);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update tuples: {}", e.getMessage());
            throw new TableMalformedException("Failed to update tuples: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void insert(Long containerId, Long databaseId, Long tableId, TableCsvDto data, Principal principal)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, ContainerNotFoundException, DatabaseConnectionException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        log.trace("parsed insert data {}", data);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* prepare the statement */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableCsvDtoToRawInsertQuery(connection, table, data);
            preparedStatement.executeUpdate();
        } catch (DateTimeParseException e) {
            log.error("Failed to parse date: {}", e.getMessage());
            throw new TableMalformedException("Failed to parse date: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            log.error("Failed to parse number: {}", e.getMessage());
            throw new TableMalformedException("Failed to parse number: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Database failed to accept tuple: {}", e.getMessage());
            log.throwing(e);
            throw new TableMalformedException("Database failed to accept tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Long tableId, TableCsvDeleteDto data, Principal principal)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, DatabaseConnectionException, QueryMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        if (data.getKeys().size() == 0) return;
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* prepare the statement */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableCsvDtoToRawDeleteQuery(connection, table, data);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete tuples: {}", e.getMessage());
            throw new TableMalformedException("Failed to delete tuples: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void insert(Long containerId, Long databaseId, Long tableId, ImportDto data, Principal principal)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, ContainerNotFoundException, DatabaseConnectionException, QueryMalformedException,
            UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* preparing the statements */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* Create a temporary table, insert there, transfer with update on duplicate key and lastly drops the temporary table */
        try {
            final Connection connection = dataSource.getConnection();
            queryMapper.dropTemporaryTableSQL(connection, table)
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to drop temporary table: {}", e.getMessage());
            log.trace("failed to drop temporary table {}", table);
            throw new TableMalformedException("Failed to drop temporary table", e);
        }
        try {
            final Connection connection = dataSource.getConnection();
            queryMapper.generateTemporaryTableSQL(connection, table)
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create temporary table: {}", e.getMessage());
            dataSource.close();
            throw new TableMalformedException("Failed to create temporary table", e);
        }
        try {
            final Connection connection = dataSource.getConnection();
            queryMapper.pathToRawInsertQuery(connection, table, data)
                    .executeUpdate();
            final File file = new File(data.getLocation());
            FileUtils.forceDelete(file);
            queryMapper.generateInsertFromTemporaryTableSQL(connection, table)
                    .executeUpdate();
        } catch (SQLException | IOException e) {
            log.error("Failed to insert temporary table: {}", e.getMessage());
            log.trace("failed to insert temporary table {}", table);
            dataSource.close();
            throw new TableMalformedException("Failed to insert temporary table", e);
        } finally {
            dataSource.close();
        }
    }

    /**
     * Parses the stored columns from a given query.
     *
     * @param query    The query.
     * @param database The database that contains the list of tables with list of columns.
     * @return List of columns in the order they are referenced in the query.
     * @throws JSQLParserException The columns could not be extracted from the query.
     */
    @Transactional(readOnly = true)
    protected List<TableColumn> parseColumns(String query, Database database) throws JSQLParserException {
        final List<TableColumn> columns = new ArrayList<>();
        final CCJSqlParserManager parserRealSql = new CCJSqlParserManager();
        final Statement statement = parserRealSql.parse(new StringReader(query));
        /* check */
        if (!(statement instanceof Select)) {
            log.error("Query attempts to update the dataset, not a SELECT statement");
            throw new JSQLParserException("Query attempts to update the dataset");
        }
        /* start parsing */
        final Select selectStatement = (Select) statement;
        final PlainSelect ps = (PlainSelect) selectStatement.getSelectBody();
        final List<SelectItem> clauses = ps.getSelectItems();
        log.trace("columns referenced in the from-clause: {}", clauses);
        /* Parse all tables */
        final List<FromItem> tables = new ArrayList<>();
        tables.add(ps.getFromItem());
        if (ps.getJoins() != null && ps.getJoins().size() > 0) {
            log.trace("query contains join items: {}", ps.getJoins());
            for (Join j : ps.getJoins()) {
                if (j.getRightItem() != null) {
                    tables.add(j.getRightItem());
                }
            }
        }
        log.trace("columns referenced in the from-clause and join-clause(s): {}", clauses);
        /* Checking if all tables or views exist */
        final List<TableColumn> allColumns = new ArrayList<>();
        for (FromItem fromItem : tables) {
            boolean foundTable = false;
            boolean foundView = false;
            for (Table table : database.getTables()) {
                allColumns.addAll(table.getColumns());
                if (table.equals(fromItem)) {
                    log.trace("table {} equals from item {}", table.getInternalName(), fromItem);
                    foundTable = true;
                    break;
                }
                log.trace("table {} did not equal from item {}", table.getInternalName(), fromItem);
            }
            for (View view : database.getViews()) {
                if (view.equals(fromItem)) {
                    log.trace("view {} equals from item {}", view.getInternalName(), fromItem);
                    foundView = true;
                    break;
                }
                log.trace("view {} did not equal from item {}", view.getInternalName(), fromItem);
            }
            if (!foundView && !foundTable) {
                final String tableName = queryMapper.stringToEscapedString(fromItem.toString());
                log.error("Table or view {} does not exist in tables {} or views {}", tableName,
                        database.getTables().stream().map(Table::getInternalName).collect(Collectors.toList()),
                        database.getViews().stream().map(View::getInternalName).collect(Collectors.toList()));
                throw new JSQLParserException("Table or view does not exist");
            }
        }
        /* Checking if all columns exist */
        for (SelectItem item : clauses) {
            if (item.toString().trim().equals("*")) {
                log.error("Do not use * in queries");
                continue;
            }
            boolean foundColumn = false;
            for (TableColumn column : allColumns) {
                if (column.equals(item)) {
                    columns.add(column);
                    foundColumn = true;
                    break;
                }
            }
            if (!foundColumn) {
                log.error("Column {} does not exist in columns {}", item, allColumns.stream().map(TableColumn::getInternalName).collect(Collectors.toList()));
                throw new JSQLParserException("Column does not exist");
            }
        }
        return columns;
    }


}
