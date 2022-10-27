package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
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
import java.io.InputStream;
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
    private final DatabaseService databaseService;

    @Autowired
    public QueryServiceImpl(QueryMapper queryMapper, TableService tableService, DatabaseService databaseService,
                            StoreService storeService) {
        this.queryMapper = queryMapper;
        this.tableService = tableService;
        this.databaseService = databaseService;
        this.storeService = storeService;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto execute(Long containerId, Long databaseId, ExecuteStatementDto statement,
                                  QueryTypeDto type, Principal principal, Long page, Long size,
                                  SortType sortDirection, String sortColumn) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryMalformedException, QueryStoreException, ContainerNotFoundException,
            ColumnParseException, UserNotFoundException, DatabaseConnectionException, TableMalformedException {
        final Query query = storeService.insert(containerId, databaseId, null, statement, type, principal, Instant.now());
        final QueryResultDto result = this.reExecute(containerId, databaseId, query, page, size, sortDirection,
                sortColumn);
        if (type.equals(QueryTypeDto.QUERY)) {
            /* view executions are not stored in the query store */
            storeService.update(containerId, databaseId, result, result.getResultNumber(), query);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto reExecute(Long containerId, Long databaseId, Query query, Long page, Long size,
                                    SortType sortDirection, String sortColumn)
            throws QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ColumnParseException,
            DatabaseConnectionException, TableMalformedException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        /* map the result to the tables (with respective columns) from the statement metadata */
        final List<TableColumn> columns;
        try {
            columns = parseColumns(query.getQuery(), database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new ColumnParseException("Failed to map/parse columns", e);
        }
        final QueryResultDto dto;
        try {
            final Connection connection = dataSource.getConnection();
            final String selection = queryMapper.tableColumnsToSelection(columns);
            final PreparedStatement preparedStatement = queryMapper.queryToRawTimestampedQuery(connection, query.getQuery(),
                    database, query.getExecution(), selection, page, size);
            final ResultSet resultSet = preparedStatement.executeQuery();
            dto = queryMapper.resultListToQueryResultDto(columns, resultSet);
        } catch (SQLException e) {
            log.error("Failed to execute and map time-versioned query: {}", e.getMessage());
            throw new TableMalformedException("Failed to execute and map time-versioned query", e);
        } finally {
            dataSource.close();
        }
        dto.setId(query.getId());
        dto.setResultNumber(countQueryResults(containerId, databaseId, query));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp, Long page,
                                  Long size) throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, TableMalformedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        final QueryResultDto result;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableToRawFindAllQuery(connection, table, timestamp, size, page);
            final ResultSet resultSet = preparedStatement.executeQuery();
            result = queryMapper.queryTableToQueryResultDto(resultSet, table);
        } catch (DateTimeException e) {
            log.error("Failed to parse date from the one stored in the metadata database: {}", e.getMessage());
            throw new TableMalformedException("Could not parse date from format", e);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object", e);
        } finally {
            dataSource.close();
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource findAll(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseConnectionException, TableMalformedException, PaginationException, ContainerNotFoundException,
            FileStorageException, QueryMalformedException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
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
            throw new FileStorageException("Failed to execute query and/or export file", e);
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
    public ExportResource findOne(Long containerId, Long databaseId, Long queryId)
            throws DatabaseNotFoundException, ImageNotSupportedException, FileStorageException, QueryStoreException,
            QueryNotFoundException, QueryMalformedException, DatabaseConnectionException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Query query = storeService.findOne(containerId, databaseId, queryId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        /* read file */
        final InputStreamResource resource;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.queryToRawExportQuery(connection, query, filename);
            preparedStatement.executeUpdate();
            final File file = new File("/tmp/" + filename);
            resource = new InputStreamResource(FileUtils.openInputStream(file));
            FileUtils.forceDelete(file);
        } catch (IOException | SQLException e) {
            log.error("Failed to execute query and/or export file: {}", e.getMessage());
            throw new FileStorageException("Failed to execute query and/or export file", e);
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
    public Long count(Long containerId, Long databaseId, Long tableId, Instant timestamp)
            throws DatabaseNotFoundException, TableNotFoundException, ImageNotSupportedException,
            DatabaseConnectionException, QueryMalformedException, QueryStoreException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableToRawCountAllQuery(connection, table, timestamp);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to count raw tuples: {}", e.getMessage());
            throw new TableMalformedException("Failed to count raw tuples", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void update(Long containerId, Long databaseId, Long tableId, TableCsvUpdateDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, DatabaseConnectionException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        if (data.getData().size() == 0 || data.getKeys().size() == 0) return;
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableCsvDtoToRawUpdateQuery(connection, table, data);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update tuples: {}", e.getMessage());
            throw new TableMalformedException("Failed to update tuples", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void insert(Long containerId, Long databaseId, Long tableId, TableCsvDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, ContainerNotFoundException, DatabaseConnectionException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        log.trace("parsed insert data {} into container {} database {} table {}", data, containerId, databaseId, tableId);
        /* run query */
        if (data.getData().size() == 0) {
            log.error("Failed to parse data, the provided map {} is empty", data.getData());
            throw new TableMalformedException("Failed to parse data");
        }
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        /* prepare the statement */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableCsvDtoToRawInsertQuery(connection, table, data);
            preparedStatement.executeUpdate();
        } catch (DateTimeParseException e) {
            log.error("Failed to parse date: {}", e.getMessage());
            throw new TableMalformedException("Failed to parse date", e);
        } catch (NumberFormatException e) {
            log.error("Failed to parse number: {}", e.getMessage());
            throw new TableMalformedException("Failed to parse number", e);
        } catch (Exception e) {
            log.error("Failed for unknown reason: {}", e.getMessage());
            throw new TableMalformedException("Failed for unknown reason", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Long tableId, TableCsvDeleteDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, DatabaseConnectionException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* run query */
        if (data.getKeys().size() == 0) return;
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        /* prepare the statement */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableCsvDtoToRawDeleteQuery(connection, table, data);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete tuples: {}", e.getMessage());
            throw new TableMalformedException("Failed to delete tuples", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void insert(Long containerId, Long databaseId, Long tableId, ImportDto data)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, ContainerNotFoundException, DatabaseConnectionException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = tableService.find(containerId, databaseId, tableId);
        /* preparing the statements */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        /* Create a temporary table, insert there, transfer with update on duplicate key and lastly drops the temporary table */
        try {
            final Connection connection = dataSource.getConnection();
            queryMapper.generateTemporaryTableSQL(connection, table)
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create temporary table: {}", e.getMessage());
            log.debug("failed to create temporary table {}", table);
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
            log.debug("failed to insert temporary table {}", table);
            dataSource.close();
            throw new TableMalformedException("Failed to insert temporary table", e);
        }
        try {
            final Connection connection = dataSource.getConnection();
            queryMapper.dropTemporaryTableSQL(connection, table)
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to drop temporary table: {}", e.getMessage());
            log.debug("failed to drop temporary table {}", table);
            throw new TableMalformedException("Failed to drop temporary table", e);
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
        log.debug("tables referenced: {}", tables);
        log.trace("columns referenced in the from-clause and join-clause(s): {}", clauses);

        /* Checking if all tables exist */
        final List<TableColumn> allColumns = new ArrayList<>();
        for (FromItem fromItem : tables) {
            boolean i = false;
            for (final Table table : database.getTables()) {
                if (table.equals(fromItem)) {
                    log.trace("table {} equals from item {}", table.getInternalName(), fromItem);
                    allColumns.addAll(table.getColumns());
                    i = false;
                    break;
                }
                log.trace("table {} did not equal from item {}", table.getInternalName(), fromItem);
                i = true;
            }
            if (i) {
                final String tableName = queryMapper.stringToEscapedString(fromItem.toString());
                log.error("Table {} does not exist", tableName);
                log.debug("table {} does not exist, available tables are {}", tableName, database.getTables().stream().map(Table::getInternalName).collect(Collectors.toList()));
                throw new JSQLParserException("Table does not exist");
            }
        }

        /* Checking if all columns exist */
        for (SelectItem item : clauses) {
            if (item.toString().trim().equals("*")) {
                log.error("Do not use * in queries");
                continue;
            }
            final String clause = queryMapper.selectItemToEscapedString(item);
            boolean i = false;
            for (TableColumn tc : allColumns) {
                if (tc.equals(item)) {
                    i = false;
                    columns.add(tc);
                    break;
                }
                i = true;
            }
            if (i) {
                log.error("Column {} does not exist", item);
                log.debug("column {} does not exist, available columns are {}", item, allColumns.stream().map(TableColumn::getInternalName).collect(Collectors.toList()));
                throw new JSQLParserException("Column does not exist");
            }
        }
        return columns;

    }

    /**
     * Counts the total number of tuples in the user database with given id for a given query object
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param query       The query object.
     * @return The number of tuples this query returns.
     * @throws DatabaseNotFoundException  The user database was not found in the container.
     * @throws ImageNotSupportedException The database image is not supported.
     */
    @Transactional(readOnly = true)
    protected Long countQueryResults(Long containerId, Long databaseId, Query query)
            throws DatabaseNotFoundException, ImageNotSupportedException, DatabaseConnectionException,
            QueryMalformedException, QueryStoreException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.queryToRawTimestampedQuery(connection, query.getQuery(),
                    database, query.getExecution(), "COUNT(*)", null, null);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to count tuples: {}", e.getMessage());
            throw new TableMalformedException("Failed to count tuples", e);
        } finally {
            dataSource.close();
        }
    }


}
