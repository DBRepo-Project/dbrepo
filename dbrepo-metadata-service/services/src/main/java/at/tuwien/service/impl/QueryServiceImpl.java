package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.TableColumnRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import at.tuwien.service.TableService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QueryServiceImpl extends HibernateConnector implements QueryService {

    private final QueryConfig queryConfig;
    private final QueryMapper queryMapper;
    private final StoreService storeService;
    private final TableService tableService;
    private final DatabaseService databaseService;
    private final TableColumnRepository tableColumnRepository;

    @Autowired
    public QueryServiceImpl(QueryConfig queryConfig, QueryMapper queryMapper, TableService tableService, DatabaseService databaseService,
                            StoreService storeService, TableColumnRepository tableColumnRepository) {
        this.queryConfig = queryConfig;
        this.queryMapper = queryMapper;
        this.tableService = tableService;
        this.storeService = storeService;
        this.databaseService = databaseService;
        this.tableColumnRepository = tableColumnRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto execute(Long databaseId, ExecuteStatementDto statement, Principal principal, Long page,
                                  Long size, SortType sortDirection, String sortColumn)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, QueryStoreException,
            ColumnParseException, UserNotFoundException, DatabaseConnectionException, TableMalformedException,
            KeycloakRemoteException, AccessDeniedException {
        if (statement.getStatement().contains(";")) {
            log.error("Failed to execute query since it contains ';'");
            throw new QueryMalformedException("Failed to execute query since it contains ';'");
        }
        final Query query = storeService.insert(databaseId, statement, principal);
        return reExecute(databaseId, query, page, size, sortDirection, sortColumn, principal);
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto reExecute(Long databaseId, Query query, Long page, Long size, SortType sortDirection,
                                    String sortColumn, Principal principal) throws QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ColumnParseException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* map the result to the tables (with respective columns) from the statement metadata */
        final List<TableColumn> columns;
        try {
            columns = parseColumns(query.getQuery(), database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new ColumnParseException("Failed to map/parse columns: " + e.getMessage(), e);
        }
        final String statement = queryMapper.queryToRawTimestampedQuery(query.getQuery(), query.getCreated(), true, page, size);
        final QueryResultDto dto = executeNonPersistent(databaseId, statement, columns);
        dto.setId(query.getId());
        dto.setResultNumber(query.getResultNumber());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Long reExecuteCount(Long databaseId, Query query, Principal principal)
            throws QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, ColumnParseException,
            TableMalformedException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        try {
            parseColumns(query.getQuery(), database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new ColumnParseException("Failed to map/parse columns: " + e.getMessage(), e);
        }
        final String statement = queryMapper.queryToRawTimestampedQuery(query.getQuery(), query.getCreated(), false, null, null);
        return executeCountNonPersistent(databaseId, statement);
    }

    private PreparedStatement prepareStatement(Connection connection, String statement) throws QueryMalformedException {
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}m reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    private QueryResultDto executeNonPersistent(Long databaseId, String statement, List<TableColumn> columns)
            throws QueryMalformedException, DatabaseNotFoundException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            log.trace("preparing statement {}", statement);
            final PreparedStatement preparedStatement = prepareStatement(connection, statement);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultListToQueryResultDto(columns, resultSet);
        } catch (SQLException e) {
            log.error("Failed to execute and map time-versioned query: {}", e.getMessage());
            throw new TableMalformedException("Failed to execute and map time-versioned query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    private Long executeCountNonPersistent(Long databaseId, String statement)
            throws QueryMalformedException, TableMalformedException, DatabaseNotFoundException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(databaseId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = prepareStatement(connection, statement);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto tableFindAll(Long databaseId, Long tableId, Instant timestamp, Long page,
                                       Long size, Principal principal) throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, TableMalformedException, QueryMalformedException {
        /* find */
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        String statement = queryMapper.tableToRawFindAllQuery(table, timestamp, size, page);
        return executeNonPersistent(databaseId, statement, table.getColumns());
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto viewFindAll(Long databaseId, View view, Long page, Long size, Principal principal)
            throws DatabaseNotFoundException, QueryMalformedException, TableMalformedException {
        /* run query */
        return executeNonPersistent(databaseId, view.getQuery(), view.getColumns());
    }

    @Override
    @Transactional
    public Long tableCount(Long databaseId, Long tableId, Instant timestamp, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, ImageNotSupportedException,
            QueryMalformedException, QueryStoreException, TableMalformedException {
        /* find */
        final Table table = tableService.find(databaseId, tableId);
        final String statement = queryMapper.tableToRawCountAllQuery(table, timestamp);
        return executeCountNonPersistent(databaseId, statement);
    }

    @Override
    @Transactional
    public Long viewCount(Long databaseId, View view, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, QueryStoreException, TableMalformedException {
        /* find */
        final String statement = queryMapper.viewToRawCountAllQuery(view);
        return executeCountNonPersistent(databaseId, statement);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource tableFindAll(Long databaseId, Long tableId, Instant timestamp, Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, FileStorageException, QueryMalformedException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
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
    public ExportResource findOne(Long databaseId, Long queryId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, FileStorageException, QueryStoreException,
            QueryNotFoundException, QueryMalformedException, DatabaseConnectionException, UserNotFoundException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(databaseId);
        final Query query = storeService.findOne(databaseId, queryId, principal);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
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
    public void update(Long databaseId, Long tableId, TableCsvUpdateDto data, Principal principal)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        if (data.getData().size() == 0 || data.getKeys().size() == 0) return;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
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
    public void insert(Long databaseId, Long tableId, TableCsvDto data, Principal principal)
            throws TableMalformedException, DatabaseNotFoundException, TableNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        log.trace("parsed insert data {}", data);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
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
            throw new TableMalformedException("Database failed to accept tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public void delete(Long databaseId, Long tableId, TableCsvDeleteDto data, Principal principal)
            throws ImageNotSupportedException, TableMalformedException, DatabaseNotFoundException,
            TableNotFoundException, DatabaseConnectionException, QueryMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        if (data.getKeys().size() == 0) return;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
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
    public void insert(Long databaseId, Long tableId, ImportDto data, Principal principal)
            throws TableMalformedException, DatabaseNotFoundException, TableNotFoundException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* preparing the statements */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
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
        data.setLocation(queryConfig.getSharedFilesystem() + File.separator + data.getLocation());
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
    public List<TableColumn> parseColumns(String query, Database database) throws JSQLParserException {
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
        final List<FromItem> tablesOrViews = new ArrayList<>();
        tablesOrViews.add(ps.getFromItem());
        if (ps.getJoins() != null && ps.getJoins().size() > 0) {
            log.trace("query contains join items: {}", ps.getJoins());
            for (Join j : ps.getJoins()) {
                if (j.getRightItem() != null) {
                    tablesOrViews.add(j.getRightItem());
                }
            }
        }
        final List<TableColumn> allColumns = tableColumnRepository.findAllByDatabaseId(database.getId());
        log.trace("columns referenced in the from-clause and join-clause(s): {}", clauses);
        /* Checking if all tables or views exist */
        log.trace("table(s) or view(s) referenced in the statement: {}", tablesOrViews.stream().map(t -> ((net.sf.jsqlparser.schema.Table) t).getName()).collect(Collectors.toList()));
        /* Checking if all columns exist */
        for (SelectItem clause : clauses) {
            final SelectExpressionItem item = (SelectExpressionItem) clause;
            final Column column = (Column) item.getExpression();
            final Optional<net.sf.jsqlparser.schema.Table> optionalTableOrView = tablesOrViews.stream()
                    .map(t -> (net.sf.jsqlparser.schema.Table) t)
                    .filter(t -> {
                        if (column.getTable() == null) {
                            /* column does not reference a specific table, so there is only one table */
                            final String tableName = ((net.sf.jsqlparser.schema.Table) tablesOrViews.get(0)).getName().replace("`", "");
                            if (t.getAlias() == null) {
                                /* table is non-aliased */
                                return t.getName().replace("`", "").equals(tableName);
                            }
                            /* has alias */
                            return t.getAlias().getName().equals(tableName);
                        }
                        final String tableName = column.getTable().getName().replace("`", "");
                        if (t.getAlias() == null) {
                            /* table is non-aliased */
                            return t.getName().replace("`", "").equals(tableName);
                        }
                        /* has alias */
                        return t.getAlias().getName().equals(tableName);
                    })
                    .findFirst();
            if (optionalTableOrView.isEmpty()) {
                log.error("Failed to find table or view with alias '{}'", column.getTable().getAlias());
                throw new JSQLParserException("Failed to find table or view with alias " + column.getTable().getAlias());
            }
            final Optional<TableColumn> optionalColumn = allColumns.stream()
                    .filter(c -> c.getInternalName().equals(column.getColumnName().replace("`", "")))
                    .filter(c -> columnMatches(c, optionalTableOrView.get().getName().replace("`", "")))
                    .findFirst();
            if (optionalColumn.isEmpty()) {
                log.error("Failed to find column with name {} in {}", column.getColumnName(), allColumns.stream().map(TableColumn::getInternalName).toList());
                throw new JSQLParserException("Failed to find column with name " + column.getColumnName() + " in " + allColumns.stream().map(TableColumn::getInternalName).toList());
            }
            final TableColumn aliasColumn = optionalColumn.get();
            if (item.getAlias() != null) {
                aliasColumn.setAlias(item.getAlias().getName());
            }
            log.trace("found column with internal name {} and alias {}", aliasColumn.getInternalName(), aliasColumn.getAlias());
            columns.add(aliasColumn);
        }
        return columns;
    }

    @Transactional(readOnly = true)
    public boolean columnMatches(TableColumn column, String tableOrView) {
        if (column.getTable().getInternalName().equals(tableOrView)) {
            /* matches table name */
            return true;
        }
        /* maybe matches one of the views */
        return column.getViews()
                .stream()
                .anyMatch(v -> v.getInternalName().equals(tableOrView));
    }

}
