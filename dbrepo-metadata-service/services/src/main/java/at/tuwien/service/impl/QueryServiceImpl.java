package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDbSidecarGateway;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.querystore.Query;
import at.tuwien.service.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.*;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Log4j2
@Service
public class QueryServiceImpl extends HibernateConnector implements QueryService {

    private final ViewMapper viewMapper;
    private final QueryMapper queryMapper;
    private final StoreService storeService;
    private final TableService tableService;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final DataDbSidecarGateway dataDbSidecarGateway;

    @Autowired
    public QueryServiceImpl(ViewMapper viewMapper, QueryMapper queryMapper, TableService tableService,
                            StorageService storageService, DatabaseService databaseService, StoreService storeService,
                            DataDbSidecarGateway dataDbSidecarGateway) {
        this.viewMapper = viewMapper;
        this.queryMapper = queryMapper;
        this.tableService = tableService;
        this.storageService = storageService;
        this.storeService = storeService;
        this.databaseService = databaseService;
        this.dataDbSidecarGateway = dataDbSidecarGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto execute(Long databaseId, ExecuteStatementDto statement, Principal principal, Long page,
                                  Long size, SortType sortDirection, String sortColumn)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, QueryStoreException,
            ColumnParseException, UserNotFoundException, TableMalformedException, QueryNotFoundException {
        if (statement.getStatement().contains(";")) {
            log.error("Failed to execute query: contains ';'");
            throw new QueryMalformedException("Failed to execute query: contains ';'");
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
            columns = queryMapper.parseColumns(query.getQuery(), database);
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
            queryMapper.parseColumns(query.getQuery(), database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new ColumnParseException("Failed to map/parse columns: " + e.getMessage(), e);
        }
        final String statement = queryMapper.queryToRawTimestampedQuery(query.getQuery(), query.getCreated(), false, null, null);
        return executeCountNonPersistent(databaseId, statement);
    }

    public PreparedStatement prepareStatement(Connection connection, String statement) throws QueryMalformedException {
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement: {}", e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement: " + e.getMessage(), e);
        }
    }

    public QueryResultDto executeNonPersistent(Long databaseId, String statement, List<TableColumn> columns)
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

    public Long executeCountNonPersistent(Long databaseId, String statement)
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
                                       Long size, Principal principal) throws TableNotFoundException,
            DatabaseNotFoundException, TableMalformedException, QueryMalformedException, ImageNotSupportedException {
        /* find */
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        return executeNonPersistent(databaseId, queryMapper.tableToRawFindAllQuery(table, timestamp, size, page),
                table.getColumns());
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResultDto viewFindAll(Long databaseId, View view, Long page, Long size, Principal principal)
            throws DatabaseNotFoundException, QueryMalformedException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = viewMapper.viewToSelectAll(connection, view, page, size);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final List<TableColumn> columns = view.getColumns()
                    .stream()
                    .map(viewMapper::viewColumnToTableColumn)
                    .toList();
            return queryMapper.resultListToQueryResultDto(columns, resultSet);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
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
    public Long viewCount(Long databaseId, View view, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryMalformedException, QueryStoreException, TableMalformedException {
        /* find */
        final String statement = queryMapper.viewToRawCountAllQuery(view);
        return executeCountNonPersistent(databaseId, statement);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource tableFindAll(Long databaseId, Long tableId, Instant timestamp, Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, FileStorageException, QueryMalformedException,
            DataDbSidecarException, DataProcessingException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.tableToRawExportQuery(connection, table, timestamp, filename);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to execute query and/or export file: {}", e.getMessage());
            throw new FileStorageException("Failed to execute query and/or export file: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return retrieveBlobAsResource(database.getContainer(), filename);
    }

    public ExportResource retrieveBlobAsResource(Container container, String filename) throws DataDbSidecarException,
            FileStorageException, DataProcessingException {
        /* upload from sidecar into blob storage */
        dataDbSidecarGateway.exportFile(container.getSidecarHost(), container.getSidecarPort(), filename);
        /* export file from blob storage */
        return storageService.getResource(filename);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResource findOne(Long databaseId, Long queryId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, FileStorageException, QueryStoreException,
            QueryNotFoundException, QueryMalformedException, DataDbSidecarException, DataProcessingException {
        return findOne(databaseId, queryId, principal, RandomStringUtils.randomAlphabetic(40) + ".csv");
    }

    @Transactional(readOnly = true)
    public ExportResource findOne(Long databaseId, Long queryId, Principal principal, String filename)
            throws DatabaseNotFoundException, ImageNotSupportedException, FileStorageException, QueryStoreException,
            QueryNotFoundException, QueryMalformedException, DataDbSidecarException, DataProcessingException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Query query = storeService.findOne(databaseId, queryId, principal);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.queryToRawExportQuery(connection, query, filename);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryStoreException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return retrieveBlobAsResource(database.getContainer(), filename);
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
        if (data.getData().isEmpty() || data.getKeys().isEmpty()) return;
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
            throws TableMalformedException, DatabaseNotFoundException, TableNotFoundException, FileStorageException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        log.trace("parsed insert data {}", data);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        /* for each LOB-like data-column, retrieve the bytes and replace the value */
        for (String key : data.getData().keySet()) {
            final boolean found = table.getColumns()
                    .stream()
                    .filter(c -> List.of(TableColumnType.BLOB, TableColumnType.LONGBLOB, TableColumnType.TINYBLOB, TableColumnType.MEDIUMBLOB).contains(c.getColumnType()))
                    .anyMatch(c -> c.getInternalName().equals(key));
            if (!found || data.getData().get(key) == null) {
                continue;
            }
            final byte[] blob = storageService.getBytes(String.valueOf(data.getData().get(key)));
            log.debug("replaced S3 storage key {} with blob", key);
            data.getData().replace(key, blob);
        }
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
            TableNotFoundException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* run query */
        if (data.getKeys().isEmpty()) return;
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
            throws TableMalformedException, DatabaseNotFoundException, TableNotFoundException, DataDbSidecarException,
            DataProcessingException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = tableService.find(databaseId, tableId);
        /* import .csv from blob storage to sidecar */
        dataDbSidecarGateway.importFile(database.getContainer().getSidecarHost(), database.getContainer().getSidecarPort(), data.getLocation());
        /* import .csv from sidecar to database */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement statement = queryMapper.pathToRawInsertQuery(connection, table, data);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to open connection to data database: {}", e.getMessage());
            throw new TableMalformedException("Failed to open connection to data database: " + e.getMessage(), e);
        } catch (QueryMalformedException e) {
            log.error("Failed to import csv: {}", e.getMessage());
            throw new TableMalformedException("Failed to import csv: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

}
