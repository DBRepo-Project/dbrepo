package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.DataDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.ColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.PostgresMapper;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class TableServicePostgresImpl extends DataConnector implements TableService {

    private final S3Config s3Config;
    private final DataMapper dataMapper;
    private final DataDbConfig dataDbConfig;
    private final PostgresMapper postgresMapper;
    private final StorageService storageService;

    @Autowired
    public TableServicePostgresImpl(S3Config s3Config, DataMapper dataMapper, DataDbConfig dataDbConfig,
                                    PostgresMapper postgresMapper, StorageService storageService) {
        this.s3Config = s3Config;
        this.dataMapper = dataMapper;
        this.dataDbConfig = dataDbConfig;
        this.postgresMapper = postgresMapper;
        this.storageService = storageService;
    }

    @Override
    @Timed(value = "dbrepo_data_get_statistics", description = "Time spent obtaining simple table statistics", histogram = true)
    public TableStatisticDto getStatistics(Database database, UUID id, String tableName) throws SQLException,
            TableMalformedException, TableNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final TableStatisticDto statistic;
        try {
            /* obtain statistic */
            final long start = System.currentTimeMillis();
            final TableDto tmpTable = inspect(database, tableName);
            final String query = postgresMapper.tableColumnStatisticsSelectRawQuery(tableName, tmpTable.getColumns());
            if (query == null) {
                log.debug("table {}.{} does not have columns that can be analysed for statistical properties", database.getInternalName(), tableName);
                return null;
            }
            final ResultSet resultSet = connection.prepareStatement(query)
                    .executeQuery();
            statistic = dataMapper.resultSetToTableStatistic(resultSet);
            statistic.setTotalColumns(Long.parseLong("" + tmpTable.getColumns()
                    .size()));
            log.atDebug()
                    .setMessage("get table statistics: " + tableName + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "get_table_statistics")
                    .log();
            statistic.setAvgRowLength(tmpTable.getAvgRowLength());
            statistic.setDataLength(tmpTable.getDataLength());
            statistic.setMaxDataLength(tmpTable.getMaxDataLength());
            statistic.setTotalRows(tmpTable.getNumRows());
            /* add to statistic dto */
            tmpTable.getColumns()
                    .stream()
                    .filter(column -> !MariaDbUtil.numericDataTypes.contains(column.getColumnType()) || !MariaDbUtil.stringDataTypes.contains(column.getColumnType()))
                    .forEach(column -> ColumnStatisticDto.builder()
                            .name(column.getInternalName())
                            .build());
            log.info("Obtained statistics for the table and {} column(s)", statistic.getColumns().size());
        } catch (SQLException e) {
            log.error("Failed to obtain column statistics: {}", e.getMessage());
            throw new TableMalformedException("Failed to obtain column statistics: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return statistic;
    }

    @Override
    public TableDto create(Database database, CreateTableDto data) throws SQLException,
            TableMalformedException, TableExistsException, TableNotFoundException {
        final String tableName = postgresMapper.nameToInternalName(data.getName());
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(postgresMapper.tableCreateDtoToCreateTableRawQuery(data))
                    .execute();
            log.atDebug()
                    .setMessage("created table: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_table")
                    .log();
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists")) {
                log.error("Failed to create table: already exists");
                throw new TableExistsException("Failed to create table: already exists", e);
            }
            log.error("Failed to create table: {}", e.getMessage());
            throw new TableMalformedException("Failed to create table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created table with name {}.{}", database.getInternalName(), tableName);
        return inspect(database, tableName);
    }

    @Override
    @Timed(value = "dbrepo_data_update_table_comment", description = "Time spent updating the table comment", histogram = true)
    public void update(Database database, Table table, TableUpdateDto data) throws SQLException,
            TableMalformedException, TableNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(
                    postgresMapper.tableNameToUpdateTableRawQuery(table.getInternalName()));
            log.trace("1={}", data.getDescription());
            if (data.getDescription() == null) {
                statement.setString(1, "");
            } else {
                statement.setString(1, data.getDescription());
            }
            statement.executeUpdate();
            log.atDebug()
                    .setMessage("update table comment: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "update_table_comment")
                    .log();
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("doesn't exist")) {
                log.error("Failed to delete table: not found: {}", e.getMessage());
                throw new TableNotFoundException("Failed to delete table: not found", e);
            }
            log.error("Failed to update table: {}", e.getMessage());
            throw new TableMalformedException("Failed to update table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated table with name {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    public void delete(Database database, Table table) throws SQLException, QueryMalformedException,
            TableNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            long start = System.currentTimeMillis();
            connection.prepareStatement(postgresMapper.dropTableVersioningRawQuery(table.getInternalName()))
                    .execute();
            log.atDebug()
                    .setMessage("delete table versioning: " + database.getInternalName() + "." + table.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "delete_table_versioning")
                    .log();
            start = System.currentTimeMillis();
            connection.prepareStatement(postgresMapper.dropTableVersioningRawQuery(table.getInternalName() + "_history"))
                    .execute();
            log.atDebug()
                    .setMessage("delete history table: " + database.getInternalName() + "." + table.getInternalName() + "_history")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "delete_history_table")
                    .log();
            start = System.currentTimeMillis();
            connection.prepareStatement(postgresMapper.dropTableRawQuery(table.getInternalName()))
                    .execute();
            log.atDebug()
                    .setMessage("delete table: " + database.getInternalName() + "." + table.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "delete_table")
                    .log();
        } catch (SQLException e) {
            if (e.getMessage().toLowerCase().contains("unknown table")) {
                log.error("Failed to delete table: not found: {}", e.getMessage());
                throw new TableNotFoundException("Failed to delete table: not found", e);
            }
            log.error("Failed to delete table: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted table with name {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    public List<TableHistoryDto> history(Database database, Table table, Long size) throws SQLException,
            TableNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<TableHistoryDto> history;
        try {
            /* find table data */
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(postgresMapper.selectHistoryRawQuery(
                            table.getInternalName(), size))
                    .executeQuery();
            log.atDebug()
                    .setMessage("get table history: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "get_table_history")
                    .log();
            history = dataMapper.resultSetToTableHistory(resultSet);
        } catch (SQLException e) {
            log.error("Failed to find history for table {}.{}: {}", database, table.getInternalName(), e.getMessage());
            throw new TableNotFoundException("Failed to find history for table " + database + "." + table.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find history for table {}.{}", database.getInternalName(), table.getInternalName());
        return history;
    }

    @Override
    @Timed(value = "dbrepo_data_count_table_data", description = "Time spent counting the table data", histogram = true)
    public Long getCount(Database database, String tableName, Instant timestamp) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final Long queryResult;
        try {
            /* find table data */
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(postgresMapper.selectCountRawQuery(
                            tableName, timestamp))
                    .executeQuery();
            log.atDebug()
                    .setMessage("get table count: " + tableName + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "get_table_count")
                    .log();
            queryResult = postgresMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to find row count from table {}.{}: {}", database, tableName, e.getMessage());
            throw new QueryMalformedException("Failed to find row count from table " + database + "." + tableName + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find row count from table {}.{}", database.getInternalName(), tableName);
        return queryResult;
    }

    @Override
    @Timed(value = "dbrepo_data_import_table_data", description = "Time spent importing the table data", histogram = true)
    public void importDataset(Database database, Table table, ImportDto data) throws SQLException,
            QueryMalformedException {
        final List<String> columns = table.getColumns()
                .stream()
                .map(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column::getInternalName)
                .toList();
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        long start = System.currentTimeMillis();
        try {
            /* import tuple */
            connection.prepareStatement(postgresMapper.tableImportFromS3ToRawQuery(s3Config, data,
                            table.getInternalName(), String.join(",", columns)))
                    .execute();
            log.atDebug()
                    .setMessage("import s3 location " + data.getLocation() + " into table: " + table.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "import_from_s3")
                    .log();
        } catch (SQLException e) {
            log.atError()
                    .setMessage("Failed to import data from s3 " + data.getLocation())
                    .setCause(e)
                    .log();
            throw new QueryMalformedException("Failed to import data from s3: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        storageService.deleteObject(data.getLocation());
        log.info("Imported dataset into table {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    @Timed(value = "dbrepo_data_delete_tuple", description = "Time spent deleting a table tuple", histogram = true)
    public void deleteTuple(Database database, Table table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(postgresMapper.tupleToRawDeleteQuery(
                    table, data));
            for (String column : data.getKeys().keySet()) {
                postgresMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), column), idx[0], column, data.getKeys().get(column));
                idx[0]++;
            }
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.atDebug()
                    .setMessage("delete tuple in table: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_delete_tuple")
                    .log();
        } catch (SQLException e) {
            log.error("Failed to delete tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted tuple(s) from table: {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    @Timed(value = "dbrepo_data_create_tuple", description = "Time spent creating a table tuple", histogram = true)
    public void createTuple(Database database, Table table, TupleDto data) throws SQLException,
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
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(postgresMapper.tupleToRawCreateQuery(
                    table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                postgresMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.atDebug()
                    .setMessage("create tuple in table: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_create_tuple")
                    .log();
        } catch (SQLException e) {
            log.error("Failed to create tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to create tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created tuple(s) in table: {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    @Timed(value = "dbrepo_data_update_tuple", description = "Time spent updating a table tuple", histogram = true)
    public void updateTuple(Database database, Table table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        log.trace("update tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(postgresMapper.tupleToRawUpdateQuery(
                    table, data));
            /* set data */
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                postgresMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            /* set key(s) */
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                postgresMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.atDebug()
                    .setMessage("update tuple in table: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_update_tuple")
                    .log();
        } catch (SQLException e) {
            log.error("Failed to update tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to update tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated tuple(s) from table: {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    public List<TableDto> explore(Database database) throws SQLException, TableNotFoundException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<TableDto> tables = new LinkedList<>();
        if (database.getTables() == null) {
            database.setTables(new LinkedList<>());
        }
        try {
            /* inspect tables before views */
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(postgresMapper.databaseTablesSelectRawQuery());
            statement.setString(1, dataDbConfig.getDefaultSchema());
            log.trace("1={}", dataDbConfig.getDefaultSchema());
            final ResultSet resultSet1 = statement.executeQuery();
            log.atDebug()
                    .setMessage("explored tables in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_tables")
                    .log();
            while (resultSet1.next()) {
                final String tableName = resultSet1.getString(1);
                if (database.getTables().stream().anyMatch(t -> t.getInternalName().equals(tableName))) {
                    log.trace("table {}.{} already known to metadata database, skip.", database.getInternalName(), tableName);
                    continue;
                }
                final TableDto table = inspect(database, tableName);
                if (database.getTables().stream().noneMatch(t -> t.getInternalName().equals(tableName))) {
                    tables.add(table);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get table schemas: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get table schemas: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Found {} new table schema(s): {}", tables.size(), tables.stream().map(TableDto::getInternalName).toList());
        return tables;
    }

    @Override
    public TableDto inspect(Database database, String tableName) throws SQLException, TableNotFoundException {
        log.trace("inspecting table: {}.{}", database.getInternalName(), tableName);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* obtain only table metadata */
            long start = System.currentTimeMillis();
            /* obtain only table metadata */
            final PreparedStatement statement1 = connection.prepareStatement(
                    postgresMapper.databaseTableSelectRawQuery());
            statement1.setString(1, dataDbConfig.getDefaultSchema());
            statement1.setString(2, tableName);
            log.trace("1={}", dataDbConfig.getDefaultSchema());
            log.trace("2={}", tableName);
            TableDto table = dataMapper.schemaResultSetToTable(database, statement1.executeQuery());
            log.atDebug()
                    .setMessage("inspected table: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_table_schema")
                    .log();
            /* obtain columns metadata */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(
                    postgresMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, dataDbConfig.getDefaultSchema());
            statement2.setString(2, tableName);
            log.trace("1={}", dataDbConfig.getDefaultSchema());
            log.trace("2={}", tableName);
            final ResultSet resultSet2 = statement2.executeQuery();
            log.atDebug()
                    .setMessage("inspect table columns: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_table_columns")
                    .log();
            while (resultSet2.next()) {
                table = dataMapper.resultSetToTable(resultSet2, table);
            }
            /* obtain check constraints metadata */
            start = System.currentTimeMillis();
            final ResultSet resultSet3 = connection.prepareStatement(postgresMapper.columnsCheckConstraintSelectRawQuery(tableName))
                    .executeQuery();
            log.atDebug()
                    .setMessage("inspect table check constraints: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_table_constraints_check")
                    .log();
            while (resultSet3.next()) {
                final String clause = resultSet3.getString(1);
                table.getConstraints()
                        .getChecks()
                        .add(clause);
                log.trace("found check clause: {}", clause);
            }
            /* obtain column constraints metadata */
            start = System.currentTimeMillis();
            final PreparedStatement statement4 = connection.prepareStatement(
                    postgresMapper.databaseTableConstraintsSelectRawQuery(dataDbConfig.getDefaultSchema()));
            statement4.setString(1, tableName);
            log.trace("1={}", tableName);
            final ResultSet resultSet4 = statement4.executeQuery();
            log.atDebug()
                    .setMessage("inspect table constraints: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_table_constraints")
                    .log();
            while (resultSet4.next()) {
                table = dataMapper.resultSetToConstraint(resultSet4, table);
                for (UniqueDto uk : table.getConstraints().getUniques()) {
                    uk.setTable(dataMapper.tableDtoToTableBriefDto(table));
                    final TableDto tmpTable = table;
                    uk.getColumns()
                            .forEach(column -> {
                                column.setTableId(tmpTable.getId());
                                column.setDatabaseId(database.getId());
                            });
                }
            }
            table.setDatabaseId(database.getId());
            final TableDto tmpTable = table;
            tmpTable.getColumns()
                    .forEach(column -> {
                        column.setTableId(tmpTable.getId());
                        column.setDatabaseId(database.getId());
                    });
            log.debug("obtained metadata for table {}.{}", database.getInternalName(), tableName);
            return tmpTable;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<Map<String, Object>> getData(Database database, Set<String> columns, String tableName, Instant timestamp, Long page, Long size)
            throws SQLException, DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            long start = System.currentTimeMillis();
            final PreparedStatement statement;
            statement = connection.prepareStatement(postgresMapper.defaultRawTableSelectQuery(columns, tableName, timestamp, page, size));
            final ResultSet resultSet = statement.executeQuery();
            final long duration = System.currentTimeMillis() - start;
            log.atDebug()
                    .setMessage("executed query statement in " + duration + "ms")
                    .addKeyValue(Constants.DURATION, duration)
                    .addKeyValue(Constants.ACTION, "execute")
                    .log();
            final List<Map<String, Object>> data = new ArrayList<>();
            while (resultSet.next()) {
                final int[] idx = new int[]{1};
                final Map<String, Object> row = new LinkedHashMap<>();
                for (String header : columns) {
                    row.put(header, resultSet.getString(idx[0]++));
                }
                data.add(row);
            }
            return data;
        } catch (SQLException e) {
            log.error("Failed to get data: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get data: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    public ColumnType getColumnType(List<Column> columns, String name) throws QueryMalformedException {
        final Optional<Column> optional = columns.stream()
                .filter(c -> c.getInternalName().equals(name)).findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column with name {}", name);
            throw new QueryMalformedException("Failed to find column");
        }
        return optional.get()
                .getColumnType();
    }

}
