package at.ac.tuwien.ifs.dbrepo.service.impl;

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
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.DataService;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
public class TableServiceMariaDbImpl extends DataConnector implements TableService {

    private final DataMapper dataMapper;
    private final MariaDbMapper mariaDbMapper;
    private final SubsetService subsetService;
    private final StorageService storageService;
    private final DataService computeService;

    @Autowired
    public TableServiceMariaDbImpl(DataMapper dataMapper, MariaDbMapper mariaDbMapper, SubsetService subsetService,
                                   StorageService storageService, DataService computeService) {
        this.dataMapper = dataMapper;
        this.mariaDbMapper = mariaDbMapper;
        this.subsetService = subsetService;
        this.storageService = storageService;
        this.computeService = computeService;
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
            final String query = mariaDbMapper.tableColumnStatisticsSelectRawQuery(database.getInternalName(),
                    tableName, tmpTable.getColumns());
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
            connection.rollback();
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
        final String tableName = mariaDbMapper.nameToInternalName(data.getName());
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.tableCreateDtoToCreateTableRawQuery(database.getInternalName(),
                            data))
                    .execute();
            log.atDebug()
                    .setMessage("created table: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_table")
                    .log();
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
                    mariaDbMapper.tableNameToUpdateTableRawQuery(database.getInternalName(), table.getInternalName()));
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
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
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
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.dropTableRawQuery(database.getInternalName(),
                            table.getInternalName()))
                    .execute();
            log.atDebug()
                    .setMessage("delete table: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "delete_table")
                    .log();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
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
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectHistoryRawQuery(
                            database.getInternalName(), table.getInternalName(), size))
                    .executeQuery();
            log.atDebug()
                    .setMessage("get table history: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "get_table_history")
                    .log();
            history = dataMapper.resultSetToTableHistory(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
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
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectCountRawQuery(
                            database.getInternalName(), tableName, timestamp))
                    .executeQuery();
            log.atDebug()
                    .setMessage("get table count: " + tableName + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "get_table_count")
                    .log();
            queryResult = mariaDbMapper.resultSetToNumber(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
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
    public void importDataset(Database database, Table table, ImportDto data) throws MalformedException,
            SQLException, QueryMalformedException, StorageUnavailableException, TableMalformedException,
            StorageNotFoundException {
        final List<String> columns = table.getColumns()
                .stream()
                .map(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column::getInternalName)
                .toList();
        final Dataset<Row> dataset = computeService.getCsv(columns, data.getLocation(),
                String.valueOf(data.getSeparator()), data.getHeader());
        final Properties properties = new Properties();
        properties.setProperty("user", database.getContainer().getUsername());
        properties.setProperty("password", database.getContainer().getPassword());
        final String temporaryTable = table.getInternalName() + "_tmp";
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        long start = System.currentTimeMillis();
        try {
            /* import tuple */
            connection.prepareStatement(mariaDbMapper.copyTableSchemaToRawQuery(table.getInternalName(), temporaryTable))
                    .execute();
            connection.commit();
            log.atDebug()
                    .setMessage("copy table schema from " + table.getInternalName() + "." + database.getInternalName() + " into temporary table: " + temporaryTable + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_copy_schema")
                    .log();
        } catch (SQLException e) {
            connection.rollback();
            log.atError()
                    .setMessage("Failed to import data from temporary table " + database.getInternalName() + "." + temporaryTable)
                    .setCause(e)
                    .log();
            throw new QueryMalformedException("Failed to import data: " + e.getMessage(), e);
        }
        log.debug("copied schema from target table {} to import table: {}", table.getInternalName(), temporaryTable);
        try {
            start = System.currentTimeMillis();
            dataset.write()
                    .mode(SaveMode.Overwrite)
                    .option("header", data.getHeader())
                    .jdbc(getSparkJdbcUrl(database), temporaryTable, properties);
            log.atDebug()
                    .setMessage("write data into temporary table: " + temporaryTable + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_import_data")
                    .log();
        } catch (Exception e) {
            log.atError()
                    .setMessage("Failed to write dataset: schema malformed")
                    .setCause(e)
                    .log();
            throw new MalformedException("Failed to write dataset: schema malformed: " + e.getMessage()) /* remove throwable on purpose, clutters the output */;
        }
        try {
            /* import tuple */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.temporaryTableToRawMergeQuery(temporaryTable,
                            table.getInternalName(), table.getColumns().stream().map(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column::getInternalName).toList()))
                    .execute();
            connection.commit();
            log.atDebug()
                    .setMessage("merge data from temporary table " + temporaryTable + "." + database.getInternalName() + " into table: " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_merge_data")
                    .log();
        } catch (SQLException e) {
            connection.rollback();
            log.atError()
                    .setMessage("Failed to import data from temporary table " + database.getInternalName() + "." + temporaryTable)
                    .setCause(e)
                    .log();
            throw new MalformedException("Failed to import tuple: " + e.getMessage(), e);
        } finally {
            /* delete temporary table */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.dropTableRawQuery(database.getInternalName(), temporaryTable,
                            false))
                    .execute();
            log.debug("deleted temporary table: {}", temporaryTable);
            connection.commit();
            log.atDebug()
                    .setMessage("delete temporary table: " + temporaryTable + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_delete_schema")
                    .log();
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
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawDeleteQuery(
                    database.getInternalName(), table, data));
            for (String column : data.getKeys().keySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
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
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
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
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawCreateQuery(
                    database.getInternalName(), table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
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
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to create tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created tuple(s) in table: {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    @Timed(value = "dbrepo_data_create_tuple_with_timestamps", description = "Time spent creating a table tuple with replication timestamps", histogram = true)
    public TupleWithTimestampsDto createTupleWithTimestamps(Database database, Table table, TupleDto data)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException {
        log.trace("create tuple with timestamps: {}", data);
        ensureReplicationKey(table, data);
        for (String key : data.getData().keySet()) {
            final boolean found = table.getColumns()
                    .stream()
                    .filter(c -> List.of(ColumnTypeDto.BLOB, ColumnTypeDto.LONGBLOB, ColumnTypeDto.TINYBLOB,
                            ColumnTypeDto.MEDIUMBLOB).contains(ColumnTypeDto.valueOf(c.getColumnType().name())))
                    .anyMatch(c -> c.getInternalName().equals(key));
            if (!found || data.getData().get(key) == null) {
                continue;
            }
            final byte[] blob = storageService.getBytes(String.valueOf(data.getData().get(key)));
            log.debug("replaced S3 storage key {} with blob", key);
            data.getData()
                    .replace(key, blob);
        }
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawCreateQuery(
                    database.getInternalName(), table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.atDebug()
                    .setMessage("create tuple with timestamps in table: " + table.getInternalName() + "."
                            + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_create_tuple_with_timestamps")
                    .log();
            final TupleWithTimestampsDto tuple = selectTupleWithTimestamps(connection, database, table,
                    lookupKeys(data.getData()));
            connection.commit();
            log.info("Created tuple with timestamps in table: {}.{}", database.getInternalName(),
                    table.getInternalName());
            return tuple;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create tuple with timestamps: {}", e.getMessage());
            throw new QueryMalformedException("Failed to create tuple with timestamps: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
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
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawUpdateQuery(
                    database.getInternalName(), table, data));
            /* set data */
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            /* set key(s) */
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
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
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to update tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated tuple(s) from table: {}.{}", database.getInternalName(), table.getInternalName());
    }

    @Override
    @Timed(value = "dbrepo_data_update_tuple_with_timestamps", description = "Time spent updating a table tuple with replication timestamps", histogram = true)
    public TupleWithTimestampsDto updateTupleWithTimestamps(Database database, Table table, TupleUpdateDto data)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException {
        log.trace("update tuple with timestamps: {}", data);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawUpdateQuery(
                    database.getInternalName(), table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.atDebug()
                    .setMessage("update tuple with timestamps in table: " + table.getInternalName() + "."
                            + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_update_tuple_with_timestamps")
                    .log();
            final Map<String, Object> lookup = new LinkedHashMap<>(data.getKeys());
            data.getData()
                    .forEach((key, value) -> {
                        if (lookup.containsKey(key)) {
                            lookup.put(key, value);
                        }
                    });
            final TupleWithTimestampsDto tuple = selectTupleWithTimestamps(connection, database, table,
                    lookupKeys(lookup));
            connection.commit();
            log.info("Updated tuple with timestamps in table: {}.{}", database.getInternalName(),
                    table.getInternalName());
            return tuple;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update tuple with timestamps: {}", e.getMessage());
            throw new QueryMalformedException("Failed to update tuple with timestamps: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_delete_tuple_with_timestamps", description = "Time spent deleting a table tuple with replication timestamps", histogram = true)
    public TupleWithTimestampsDto deleteTupleWithTimestamps(Database database, Table table, TupleDeleteDto data)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException {
        log.trace("delete tuple with timestamps: {}", data);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawDeleteQuery(
                    database.getInternalName(), table, data));
            for (String column : data.getKeys().keySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                        getColumnType(table.getColumns(), column), idx[0], column, data.getKeys().get(column));
                idx[0]++;
            }
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.atDebug()
                    .setMessage("delete tuple with timestamps in table: " + table.getInternalName() + "."
                            + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_delete_tuple_with_timestamps")
                    .log();
            final TupleWithTimestampsDto tuple = selectTupleWithTimestamps(connection, database, table,
                    lookupKeys(data.getKeys()));
            connection.commit();
            log.info("Deleted tuple with timestamps from table: {}.{}", database.getInternalName(),
                    table.getInternalName());
            return tuple;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete tuple with timestamps: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete tuple with timestamps: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public List<TableDto> explore(Database database) throws SQLException, TableNotFoundException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<TableDto> tables = new LinkedList<>();
        try {
            /* inspect tables before views */
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.databaseTablesSelectRawQuery());
            statement.setString(1, database.getInternalName());
            final ResultSet resultSet1 = statement.executeQuery();
            log.atDebug()
                    .setMessage("explored tables in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_tables")
                    .log();
            final List<Table> knownTables = Optional.ofNullable(database.getTables())
                    .orElseGet(List::of);
            while (resultSet1.next()) {
                final String tableName = resultSet1.getString(1);
                if (knownTables.stream().anyMatch(t -> t.getInternalName().equals(tableName))) {
                    log.trace("view {}.{} already known to metadata database, skip.", database.getInternalName(), tableName);
                    continue;
                }
                final TableDto table = inspect(database, tableName);
                if (knownTables.stream().noneMatch(t -> t.getInternalName().equals(tableName))) {
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
    public TableDto inspect(Database database, String tableName) throws SQLException, TableNotFoundException {
        log.trace("inspecting table: {}.{}", database.getInternalName(), tableName);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* obtain only table metadata */
            long start = System.currentTimeMillis();
            final PreparedStatement statement0 = connection.prepareStatement(mariaDbMapper.analyseTableRawQuery());
            statement0.setString(1, database.getInternalName());
            statement0.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            statement0.execute();
            log.atDebug()
                    .setMessage("analysed table: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_table_schema")
                    .log();
            /* obtain only table metadata */
            start = System.currentTimeMillis();
            final PreparedStatement statement1 = connection.prepareStatement(mariaDbMapper.databaseTableSelectRawQuery());
            statement1.setString(1, database.getInternalName());
            statement1.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            TableDto table = dataMapper.schemaResultSetToTable(database, statement1.executeQuery());
            log.atDebug()
                    .setMessage("inspected table: " + database.getInternalName() + "." + tableName)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_table_schema")
                    .log();
            /* obtain columns metadata */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
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
            final PreparedStatement statement3 = connection.prepareStatement(mariaDbMapper.columnsCheckConstraintSelectRawQuery());
            statement3.setString(1, database.getInternalName());
            statement3.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            final ResultSet resultSet3 = statement3.executeQuery();
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
            final PreparedStatement statement4 = connection.prepareStatement(mariaDbMapper.databaseTableConstraintsSelectRawQuery());
            statement4.setString(1, database.getInternalName());
            statement4.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
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

    private void ensureReplicationKey(Table table, TupleDto data) {
        if (data.getData() == null) {
            data.setData(new LinkedHashMap<>());
        }
        if (!hasColumn(table, "replication_key")) {
            return;
        }
        if (!data.getData().containsKey("replication_key") || data.getData().get("replication_key") == null) {
            final String key = UUID.randomUUID().toString();
            data.getData().put("replication_key", key);
            log.debug("Generated replication key {}", key);
        }
    }

    private boolean hasColumn(Table table, String name) {
        return table.getColumns() != null && table.getColumns()
                .stream()
                .anyMatch(column -> name.equals(column.getInternalName()));
    }

    private Map<String, Object> lookupKeys(Map<String, Object> values) {
        if (values != null && values.containsKey("replication_key")) {
            final Map<String, Object> keys = new LinkedHashMap<>();
            keys.put("replication_key", values.get("replication_key"));
            return keys;
        }
        return values;
    }

    private TupleWithTimestampsDto selectTupleWithTimestamps(Connection connection, Database database, Table table,
                                                            Map<String, Object> keys)
            throws SQLException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        if (keys == null || keys.isEmpty()) {
            throw new QueryMalformedException("Failed to select tuple with timestamps: no lookup keys provided");
        }
        final List<String> columns = table.getColumns()
                .stream()
                .map(Column::getInternalName)
                .distinct()
                .toList();
        final StringBuilder query = new StringBuilder("SELECT ");
        final int[] columnIndex = new int[]{0};
        columns.forEach(column -> query.append(columnIndex[0]++ == 0 ? "" : ", ")
                .append("`")
                .append(column)
                .append("`"));
        query.append(", ROW_START AS inserted_at, ROW_END AS deleted_at FROM `")
                .append(database.getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("` FOR SYSTEM_TIME ALL WHERE ");
        final int[] keyIndex = new int[]{0};
        keys.forEach((key, value) -> {
            query.append(keyIndex[0]++ == 0 ? "" : " AND ")
                    .append("`")
                    .append(key)
                    .append("`");
            if (value == null) {
                query.append(" IS NULL");
            } else {
                query.append(" = ?");
            }
        });
        query.append(" ORDER BY ROW_START DESC LIMIT 1;");
        final PreparedStatement statement = connection.prepareStatement(query.toString());
        int bind = 1;
        for (Map.Entry<String, Object> entry : keys.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement,
                    getColumnType(table.getColumns(), entry.getKey()), bind++, entry.getKey(), entry.getValue());
        }
        final ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            throw new QueryMalformedException("Failed to select tuple with timestamps");
        }
        final Map<String, Object> data = new LinkedHashMap<>();
        for (String column : columns) {
            data.put(column, resultSet.getObject(column));
        }
        final Instant insertedAt = timestampToInstant(resultSet.getObject("inserted_at"));
        final Instant deletedAt = normaliseRowEnd(timestampToInstant(resultSet.getObject("deleted_at")));
        return TupleWithTimestampsDto.builder()
                .data(data)
                .insertedAt(insertedAt)
                .deletedAt(deletedAt)
                .replicationKey(data.get("replication_key") != null ? String.valueOf(data.get("replication_key")) : null)
                .build();
    }

    private Instant timestampToInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        final String timestamp = String.valueOf(value).trim();
        if (timestamp.isEmpty()) {
            return null;
        }
        final String normalized = timestamp.replace(" ", "T");
        try {
            return Instant.parse(normalized);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(normalized)
                        .toInstant();
            } catch (DateTimeParseException ignoredToo) {
                return Instant.parse(normalized + "Z");
            }
        }
    }

    private Instant normaliseRowEnd(Instant rowEnd) {
        if (rowEnd != null && rowEnd.isAfter(Instant.parse("2038-01-01T00:00:00Z"))) {
            return null;
        }
        return rowEnd;
    }

}
