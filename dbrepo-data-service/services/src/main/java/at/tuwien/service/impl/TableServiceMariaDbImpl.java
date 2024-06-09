package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.config.S3Config;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.SchemaService;
import at.tuwien.service.StorageService;
import at.tuwien.service.TableService;
import at.tuwien.utils.MariaDbUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.*;

@Log4j2
@Service
public class TableServiceMariaDbImpl extends HibernateConnector implements TableService {

    private final S3Config s3Config;
    private final MariaDbMapper mariaDbMapper;
    private final SchemaService schemaService;
    private final StorageService storageService;
    private final DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @Autowired
    public TableServiceMariaDbImpl(S3Config s3Config, MariaDbMapper mariaDbMapper, SchemaService schemaService,
                                   StorageService storageService,
                                   DataDatabaseSidecarGateway dataDatabaseSidecarGateway) {
        this.s3Config = s3Config;
        this.mariaDbMapper = mariaDbMapper;
        this.schemaService = schemaService;
        this.storageService = storageService;
        this.dataDatabaseSidecarGateway = dataDatabaseSidecarGateway;
    }

    @Override
    public List<TableDto> getSchemas(PrivilegedDatabaseDto database) throws SQLException, TableNotFoundException,
            QueryMalformedException, DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<TableDto> tables = new LinkedList<>();
        try {
            /* inspect tables before views */
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.databaseTablesSelectRawQuery());
            statement.setString(1, database.getInternalName());
            final ResultSet resultSet1 = statement.executeQuery();
            while (resultSet1.next()) {
                final String tableName = resultSet1.getString(1);
                if (database.getTables().stream().anyMatch(t -> t.getInternalName().equals(tableName))) {
                    log.trace("view {}.{} already known to metadata database, skip.", database.getInternalName(), tableName);
                    continue;
                }
                final TableDto table = schemaService.inspectTable(database, tableName);
                if (database.getTables().stream().noneMatch(t -> t.getInternalName().equals(table.getInternalName()))) {
                    tables.add(table);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get table schemas: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get table schemas: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Found {} table schema(s)", tables.size());
        return tables;
    }

    @Override
    public TableStatisticDto getStatistics(PrivilegedTableDto table) throws SQLException, TableMalformedException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final TableStatisticDto statistic;
        try {
            /* obtain statistic */
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.tableColumnStatisticsSelectRawQuery(table.getColumns(), table.getInternalName()))
                    .executeQuery();
            statistic = mariaDbMapper.resultSetToTableStatistic(resultSet);
            statistic.setRows(getCount(table, null));
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to obtain column statistics: {}", e.getMessage());
            throw new TableMalformedException("Failed to obtain column statistics: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        table.getColumns()
                .stream()
                .filter(column -> !MariaDbUtil.numericDataTypes.contains(column.getColumnType()))
                .forEach(column -> statistic.getColumns().put(column.getInternalName(), new ColumnStatisticDto()));
        log.info("Obtained column statistics for table: {}", table.getInternalName());
        return statistic;
    }

    @Override
    public TableDto find(PrivilegedDatabaseDto database, String tableName) throws TableNotFoundException, SQLException,
            QueryMalformedException {
        return schemaService.inspectTable(database, tableName);
    }

    @Override
    public TableDto createTable(PrivilegedDatabaseDto database, TableCreateDto data) throws SQLException,
            TableMalformedException, TableExistsException, TableNotFoundException, QueryMalformedException {
        final String tableName = mariaDbMapper.nameToInternalName(data.getName());
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            if (data.getNeedSequence()) {
                /* create table sequence if not exists */
                connection.prepareStatement(mariaDbMapper.tableCreateDtoToCreateSequenceRawQuery(data))
                        .execute();
                log.info("Created sequence as primary key");
            }
            /* create table if not exists */
            connection.prepareStatement(mariaDbMapper.tableCreateDtoToCreateTableRawQuery(data))
                    .execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            if (e.getMessage().contains("already exists")) {
                log.error("Failed to create table: already exists");
                throw new TableExistsException("Failed to create table: already exists", e);
            }
            log.error("Failed to create table: {}", e.getMessage());
            throw new TableMalformedException("Failed to create table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created table with name {}", tableName);
        final TableDto table = find(database, tableName);
        table.setName(data.getName());
        return table;
    }

    @Override
    public void delete(PrivilegedTableDto table) throws SQLException, QueryMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final String tableName = mariaDbMapper.nameToInternalName(table.getInternalName());
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            connection.prepareStatement(mariaDbMapper.dropTableRawQuery(tableName))
                    .execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete table: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted table with name {}", tableName);
    }

    @Override
    public QueryResultDto getData(PrivilegedTableDto table, Instant timestamp, Long page, Long size) throws SQLException,
            TableMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final QueryResultDto queryResult;
        try {
            /* find table data */
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectDatasetRawQuery(
                            table.getDatabase().getInternalName(), table.getInternalName(), table.getColumns(),
                            timestamp, size, page))
                    .executeQuery();
            connection.commit();
            queryResult = mariaDbMapper.resultListToQueryResultDto(table.getColumns(), resultSet);
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find data from table {}.{}: {}", table.getDatabase().getInternalName(), table.getInternalName(), e.getMessage());
            throw new TableMalformedException("Failed to find data from table " + table.getDatabase().getInternalName() + "." + table.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find data from table {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
        queryResult.setId(table.getId());
        return queryResult;
    }

    @Override
    public List<TableHistoryDto> history(PrivilegedTableDto table, Long size) throws SQLException,
            TableNotFoundException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final List<TableHistoryDto> history;
        try {
            /* find table data */
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectHistoryRawQuery(
                            table.getDatabase().getInternalName(), table.getInternalName(), size))
                    .executeQuery();
            history = mariaDbMapper.resultSetToTableHistory(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find history for table {}.{}: {}", table.getDatabase().getInternalName(), table.getInternalName(), e.getMessage());
            throw new TableNotFoundException("Failed to find history for table " + table.getDatabase().getInternalName() + "." + table.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find history for table {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
        return history;
    }

    @Override
    public Long getCount(PrivilegedTableDto table, Instant timestamp) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final Long queryResult;
        try {
            /* find table data */
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectCountRawQuery(
                            table.getDatabase().getInternalName(), table.getInternalName(), timestamp))
                    .executeQuery();
            queryResult = mariaDbMapper.resultSetToNumber(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find row count from table {}.{}: {}", table.getDatabase().getInternalName(), table.getInternalName(), e.getMessage());
            throw new QueryMalformedException("Failed to find row count from table " + table.getDatabase().getInternalName() + "." + table.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find row count from table {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
        return queryResult;
    }

    @Override
    public void importDataset(PrivilegedTableDto table, ImportCsvDto data)
            throws StorageNotFoundException, SQLException, QueryMalformedException, ServiceException, RemoteUnavailableException {
        /* import .csv from blob storage to sidecar */
        dataDatabaseSidecarGateway.importFile(table.getDatabase().getContainer().getSidecarHost(), table.getDatabase().getContainer().getSidecarPort(), data.getLocation());
        /* import .csv from sidecar to database */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
            data.setLocation(s3Config.getS3FilePath() + "/" + data.getLocation());
            connection.prepareStatement(mariaDbMapper.datasetToRawInsertQuery(table.getDatabase().getInternalName(), table, data))
                    .execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to import tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to import tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Imported dataset into table: {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
    }

    @Override
    public void deleteTuple(PrivilegedTableDto table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException {
        log.trace("delete tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawDeleteQuery(table, data));
            for (String column : data.getKeys().keySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
                        getColumnType(table.getColumns(), column), idx[0], column, data.getKeys().get(column));
                idx[0]++;
            }
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted tuple(s) from table: {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
    }

    @Override
    public void createTuple(PrivilegedTableDto table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        log.trace("create tuple: {}", data);
        /* for each LOB-like data-column, retrieve the bytes and replace the value */
        for (String key : data.getData().keySet()) {
            final boolean found = table.getColumns()
                    .stream()
                    .filter(c -> List.of(ColumnTypeDto.BLOB, ColumnTypeDto.LONGBLOB, ColumnTypeDto.TINYBLOB, ColumnTypeDto.MEDIUMBLOB).contains(c.getColumnType()))
                    .anyMatch(c -> c.getInternalName().equals(key));
            if (!found || data.getData().get(key) == null) {
                continue;
            }
            final byte[] blob = storageService.getBytes(String.valueOf(data.getData().get(key)));
            log.debug("replaced S3 storage key {} with blob", key);
            data.getData()
                    .replace(key, blob);
        }
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* create tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawCreateQuery(table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to create tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created tuple(s) in table: {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
    }

    @Override
    public void updateTuple(PrivilegedTableDto table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException {
        log.trace("update tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawUpdateQuery(table, data));
            /* set data */
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            /* set key(s) */
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to update tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated tuple(s) from table: {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
    }

    public ColumnTypeDto getColumnType(List<ColumnDto> columns, String name) throws QueryMalformedException {
        final Optional<ColumnDto> optional = columns.stream()
                .filter(c -> c.getInternalName().equals(name)).findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column with name {}", name);
            throw new QueryMalformedException("Failed to find column");
        }
        return optional.get()
                .getColumnType();
    }

    @Override
    public ExportResourceDto exportDataset(PrivilegedTableDto table, Instant timestamp) throws SQLException,
            StorageNotFoundException, StorageUnavailableException, QueryMalformedException, ServiceException,
            RemoteUnavailableException {
        final String fileName = RandomStringUtils.randomAlphabetic(40) + ".csv";
        final String filePath = s3Config.getS3FilePath() + "/" + fileName;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* export to data database sidecar */
            connection.prepareStatement(mariaDbMapper.tableOrViewToRawExportQuery(table.getDatabase().getInternalName(),
                            table.getInternalName(), table.getColumns(), timestamp, filePath))
                    .executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        dataDatabaseSidecarGateway.exportFile(table.getDatabase().getContainer().getSidecarHost(), table.getDatabase().getContainer().getSidecarPort(), fileName);
        return storageService.getResource(fileName);
    }

}
