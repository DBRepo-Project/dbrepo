package at.tuwien.service.impl;

import at.tuwien.api.SortTypeDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.exception.*;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.StorageService;
import at.tuwien.service.TableService;
import at.tuwien.utils.MariaDbUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.ExtendedAnalysisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Log4j2
@Service
public class TableServiceMariaDbImpl extends DataConnector implements TableService {

    private final DataMapper dataMapper;
    private final SparkSession sparkSession;
    private final MariaDbMapper mariaDbMapper;
    private final StorageService storageService;
    private final DatabaseService databaseService;

    @Autowired
    public TableServiceMariaDbImpl(DataMapper dataMapper, SparkSession sparkSession, MariaDbMapper mariaDbMapper,
                                   StorageService storageService, DatabaseService databaseService) {
        this.dataMapper = dataMapper;
        this.sparkSession = sparkSession;
        this.mariaDbMapper = mariaDbMapper;
        this.storageService = storageService;
        this.databaseService = databaseService;
    }

    @Override
    public TableStatisticDto getStatistics(TableDto table) throws SQLException, TableMalformedException,
            TableNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(table);
        final Connection connection = dataSource.getConnection();
        final TableStatisticDto statistic;
        try {
            /* obtain statistic */
            final long start = System.currentTimeMillis();
            final String query = mariaDbMapper.tableColumnStatisticsSelectRawQuery(table.getColumns(), table.getInternalName());
            if (query == null) {
                log.debug("table {}.{} does not have columns that can be analysed for statistical properties (i.e. no numeric columns)", table.getDatabase().getInternalName(), table.getInternalName());
                statistic = null;
            } else {
                final ResultSet resultSet = connection.prepareStatement(query)
                        .executeQuery();
                log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
                statistic = dataMapper.resultSetToTableStatistic(resultSet);
                final TableDto tmpTable = databaseService.inspectTable(table.getDatabase(), table.getInternalName());
                statistic.setAvgRowLength(tmpTable.getAvgRowLength());
                statistic.setDataLength(tmpTable.getDataLength());
                statistic.setMaxDataLength(tmpTable.getMaxDataLength());
                statistic.setRows(tmpTable.getNumRows());
                /* add to statistic dto */
                table.getColumns()
                        .stream()
                        .filter(column -> !MariaDbUtil.numericDataTypes.contains(column.getColumnType()))
                        .forEach(column -> statistic.getColumns().put(column.getInternalName(), new ColumnStatisticDto()));
                log.info("Obtained statistics for the table and {} column(s)", statistic.getColumns().size());
                log.trace("obtained statistics: {}", statistic);
            }
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
    public void updateTable(TableDto table, TableUpdateDto data) throws SQLException,
            TableMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tableNameToUpdateTableRawQuery(table.getInternalName()));
            log.trace("prepare with arg 1={}", data.getDescription());
            if (data.getDescription() == null) {
                statement.setString(1, "");
            } else {
                statement.setString(1, data.getDescription());
            }
            statement.executeUpdate();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update table: {}", e.getMessage());
            throw new TableMalformedException("Failed to update table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated table with name {}", table.getInternalName());
    }

    @Override
    public void delete(TableDto table) throws SQLException, QueryMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.dropTableRawQuery(table.getInternalName()))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete table: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted table with name {}", table.getInternalName());
    }

    @Override
    public List<TableHistoryDto> history(TableDto table, Long size) throws SQLException,
            TableNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final List<TableHistoryDto> history;
        try {
            /* find table data */
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectHistoryRawQuery(
                            table.getDatabase().getInternalName(), table.getInternalName(), size))
                    .executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            history = dataMapper.resultSetToTableHistory(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find history for table {}.{}: {}", table.getDatabase(), table.getInternalName(), e.getMessage());
            throw new TableNotFoundException("Failed to find history for table " + table.getDatabase() + "." + table.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find history for table {}.{}", table.getDatabase(), table.getInternalName());
        return history;
    }

    @Override
    public Long getCount(TableDto table, Instant timestamp) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final Long queryResult;
        try {
            /* find table data */
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectCountRawQuery(
                            table.getDatabase().getInternalName(), table.getInternalName(), timestamp))
                    .executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            queryResult = mariaDbMapper.resultSetToNumber(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find row count from table {}.{}: {}", table.getDatabase(), table.getInternalName(), e.getMessage());
            throw new QueryMalformedException("Failed to find row count from table " + table.getDatabase() + "." + table.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find row count from table {}.{}", table.getDatabase(), table.getInternalName());
        return queryResult;
    }

    @Override
    public void importDataset(TableDto table, ImportDto data) throws MalformedException,
            StorageNotFoundException, StorageUnavailableException, SQLException, QueryMalformedException,
            TableMalformedException {
        final List<String> columns = table.getColumns()
                .stream()
                .map(ColumnDto::getInternalName)
                .toList();
        final Dataset<Row> dataset = storageService.loadDataset(columns, data.getLocation(),
                String.valueOf(data.getSeparator()), data.getHeader());
        final Properties properties = new Properties();
        properties.setProperty("user", table.getDatabase().getContainer().getUsername());
        properties.setProperty("password", table.getDatabase().getContainer().getPassword());
        final String temporaryTable = table.getInternalName() + "_tmp";
        try {
            log.trace("import dataset to temporary table: {}", temporaryTable);
            dataset.write()
                    .mode(SaveMode.Overwrite)
                    .option("header", data.getHeader())
                    .option("inferSchema", "true")
                    .jdbc(getSparkUrl(table), temporaryTable, properties);
        } catch (Exception e) {
            if (e instanceof AnalysisException exception) {
                final String message = exception.getSimpleMessage()
                        .replaceAll(" Some\\(.*", "");
                log.error("Failed to write dataset: schema malformed: {}", message);
                throw new MalformedException("Failed to write dataset: schema malformed: " + message) /* remove throwable on purpose, clutters the output */;
            }
            log.error("Failed to write dataset: {}", e.getMessage());
            throw new MalformedException("Failed to write dataset: " + e.getMessage()) /* remove throwable on purpose, clutters the output */;
        }
        /* import .csv from sidecar to database */
        final ComboPooledDataSource dataSource = getDataSource(table);
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
            connection.prepareStatement(mariaDbMapper.temporaryTableToRawMergeQuery(temporaryTable,
                            table.getInternalName(), table.getColumns().stream().map(c -> c.getInternalName()).toList()))
                    .execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to import tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to import tuple: " + e.getMessage(), e);
        } finally {
            /* delete temporary table */
            connection.prepareStatement(mariaDbMapper.dropTableRawQuery(temporaryTable, false))
                    .execute();
            connection.commit();
            dataSource.close();
        }
        log.info("Imported dataset into table: {}.{}", table.getDatabase(), table.getInternalName());
    }

    @Override
    public void deleteTuple(TableDto table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException {
        log.trace("delete tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getDataSource(table);
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
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted tuple(s) from table: {}.{}", table.getDatabase(), table.getInternalName());
    }

    @Override
    public void createTuple(TableDto table, TupleDto data) throws SQLException, QueryMalformedException,
            TableMalformedException, StorageUnavailableException, StorageNotFoundException {
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
        final ComboPooledDataSource dataSource = getDataSource(table);
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
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to create tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created tuple(s) in table: {}.{}", table.getDatabase(), table.getInternalName());
    }

    @Override
    public void updateTuple(TableDto table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException {
        log.trace("update tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getDataSource(table);
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
            final long start = System.currentTimeMillis();
            statement.executeUpdate();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to update tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated tuple(s) from table: {}.{}", table.getDatabase(), table.getInternalName());
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
    public Dataset<Row> getData(DatabaseDto database, String tableOrView, Instant timestamp,
                                Long page, Long size, SortTypeDto sortDirection, String sortColumn)
            throws QueryMalformedException, TableNotFoundException {
        try {
            return sparkSession.read()
                    .format("jdbc")
                    .option("user", database.getContainer().getUsername())
                    .option("password", database.getContainer().getPassword())
                    .option("url", getSparkUrl(database))
                    .option("query", mariaDbMapper.defaultRawSelectQuery(database.getInternalName(), tableOrView,
                            timestamp, page, size))
                    .load();

        } catch (Exception e) {
            if (e instanceof ExtendedAnalysisException exception) {
                if (exception.getSimpleMessage().contains("TABLE_OR_VIEW_NOT_FOUND")) {
                    log.error("Failed to find named reference: {}", exception.getSimpleMessage());
                    throw new TableNotFoundException("Failed to find named reference: " + exception.getSimpleMessage()) /* remove throwable on purpose, clutters the output */;
                }
            }
            log.error("Malformed query: {}", e.getMessage());
            throw new QueryMalformedException("Malformed query: " + e.getMessage(), e);
        }
    }

}
