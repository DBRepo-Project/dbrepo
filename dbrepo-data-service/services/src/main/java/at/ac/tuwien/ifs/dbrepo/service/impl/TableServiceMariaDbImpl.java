package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary.PrimaryKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationTimestampService;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
public class TableServiceMariaDbImpl extends DataConnector implements TableService {

    private final DataMapper dataMapper;
    private final MariaDbMapper mariaDbMapper;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final ReplicationTimestampService replicationTimestampService;

    @Autowired
    public TableServiceMariaDbImpl(DataMapper dataMapper, MariaDbMapper mariaDbMapper, StorageService storageService,
                                   DatabaseService databaseService, ReplicationTimestampService replicationTimestampService) {
        this.dataMapper = dataMapper;
        this.mariaDbMapper = mariaDbMapper;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.replicationTimestampService = replicationTimestampService;
    }

    @Override
    @Timed(value = "dbrepo_data_get_statistics", description = "Time spent obtaining simple table statistics", histogram = true)
    public TableStatisticDto getStatistics(DatabaseDto database, String tableName) throws SQLException,
            TableMalformedException, TableNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final TableStatisticDto statistic;
        try {
            /* obtain statistic */
            final long start = System.currentTimeMillis();
            final TableDto tmpTable = databaseService.inspectTable(database, tableName);
            final String query = mariaDbMapper.tableColumnStatisticsSelectRawQuery(tmpTable.getColumns(), tableName);
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
    @Timed(value = "dbrepo_data_update_table_comment", description = "Time spent updating the table comment", histogram = true)
    public void updateTable(DatabaseDto database, TableDto table, TableUpdateDto data) throws SQLException,
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
    public void delete(DatabaseDto database, TableDto table) throws SQLException, QueryMalformedException,
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
    public List<TableHistoryDto> history(DatabaseDto database, TableDto table, Long size) throws SQLException,
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
    public Long getCount(DatabaseDto database, String tableName, Instant timestamp) throws SQLException,
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
    public void importDataset(DatabaseDto database, TableDto table, ImportDto data) throws MalformedException,
            StorageNotFoundException, StorageUnavailableException, SQLException, QueryMalformedException,
            TableMalformedException {
        final List<String> columns = table.getColumns()
                .stream()
                .map(ColumnDto::getInternalName)
                .toList();
        
        // Check if table has replication_key column and log info
        final boolean hasReplicationKey = columns.stream()
                .anyMatch(c -> "replication_key".equalsIgnoreCase(c));
        if (hasReplicationKey) {
            log.info("Importing data into table with replication_key column - UUIDs will be auto-generated");
        }
        
        final Dataset<Row> dataset = storageService.loadDataset(columns, data.getLocation(),
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
                    .jdbc(getSparkUrl(database), temporaryTable, properties);
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
                            table.getInternalName(), table.getColumns().stream().map(c -> c.getInternalName()).toList()))
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
    public void deleteTuple(DatabaseDto database, TableDto table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException {
        log.trace("delete tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawDeleteQuery(
                    database.getInternalName(), table, data));
            for (String column : data.getKeys().keySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
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
    public void createTuple(DatabaseDto database, TableDto table, TupleDto data) throws SQLException,
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
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
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
    @Timed(value = "dbrepo_data_create_tuple_with_ts", description = "Time spent creating a table tuple incl. timestamps", histogram = true)
    public Map<String, Object> createTupleWithTimestamps(DatabaseDto database, TableDto table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        log.trace("create tuple with timestamps: {}", data);

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

        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        Map<String, Object> createdTupleWithTimestamps = null;
        try {
            /* create tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement insert = connection.prepareStatement(mariaDbMapper.tupleToRawCreateQuery(
                    database.getInternalName(), table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(insert,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            final long start = System.currentTimeMillis();
            insert.executeUpdate();
            log.atInfo()
                    .setMessage("create tuple in table (with ts): " + table.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_create_tuple_with_ts")
                    .log();

            /* read tuple including versioning timestamps: select by primary key FOR SYSTEM_TIME AS OF NOW() */
            // Build WHERE clause from primary key and select list with explicit columns + system versioning columns
            final List<String> primaryKeyColumns = table.getConstraints().getPrimaryKey().stream()
                    .map(pk -> pk.getColumn().getInternalName())
                    .toList();
            final StringBuilder select = new StringBuilder("SELECT ");
            final int[] colIdx = new int[]{0};
            for (ColumnDto c : table.getColumns()) {
                select.append(colIdx[0]++ == 0 ? "" : ", ")
                        .append("`")
                        .append(c.getInternalName())
                        .append("`");
            }
            // Always include replication_key column to avoid cache issues
            select.append(", `replication_key`");
            select.append(", ROW_START AS inserted_at, ROW_END AS deleted_at FROM `")
                    .append(database.getInternalName())
                    .append("`.`")
                    .append(table.getInternalName())
                    .append("` FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(DataMapper.mariaDbFormatter.format(java.time.Instant.now()))
                    .append("' WHERE ");
            final int[] sIdx = new int[]{0};
            primaryKeyColumns.forEach(col -> select.append(sIdx[0]++ == 0 ? "" : " AND ")
                    .append("`").append(col).append("` = ?"));
            select.append(" LIMIT 1;");

            final PreparedStatement selectStmt = connection.prepareStatement(select.toString());
            int bind = 1;
            for (String col : primaryKeyColumns) {
                final Object value = data.getData().get(col);
                mariaDbMapper.prepareStatementWithColumnTypeObject(selectStmt, getColumnType(table.getColumns(), col), bind++, col, value);
            }
            final ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                createdTupleWithTimestamps = new java.util.HashMap<>();
                // include all known columns
                for (ColumnDto c : table.getColumns()) {
                    createdTupleWithTimestamps.put(c.getInternalName(), rs.getObject(c.getInternalName()));
                }
                // Always try to include replication_key to avoid cache issues
                try {
                    Object replicationKey = rs.getObject("replication_key");
                    createdTupleWithTimestamps.put("replication_key", replicationKey);
                    log.debug("Retrieved replication_key: {}", replicationKey);
                } catch (Exception e) {
                    log.debug("replication_key column not present in result set: {}", e.getMessage());
                }
                // add versioning timestamps exposed by MariaDB system-versioned tables
                // They are accessible via ROW_START/ROW_END aliases when selecting all columns
                try {
                    Object rowStart = rs.getObject("ROW_START");
                    Object rowEnd = rs.getObject("ROW_END");
                    createdTupleWithTimestamps.put("inserted_at", rowStart);
                    createdTupleWithTimestamps.put("deleted_at", rowEnd);
                } catch (Exception ignore) {
                    // fallback: try common alias names if present in schema
                    putIfColumnExists(rs, createdTupleWithTimestamps, "inserted_at");
                    putIfColumnExists(rs, createdTupleWithTimestamps, "deleted_at");
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create tuple with timestamps: {}", e.getMessage());
            throw new QueryMalformedException("Failed to create tuple with timestamps: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        
        // Format timestamps with microsecond precision for consistent output
        formatTimestampsWithMicrosecondPrecision(createdTupleWithTimestamps);
        
        log.info("Created tuple(s) in table (with ts): {}.{}", database.getInternalName(), table.getInternalName());
        return createdTupleWithTimestamps;
    }

    /**
     * Formats timestamp fields with microsecond precision for consistent output
     */
    private void formatTimestampsWithMicrosecondPrecision(Map<String, Object> tuple) {
        // Format inserted_at timestamp
        if (tuple.get("inserted_at") != null) {
            Object insertedAt = tuple.get("inserted_at");
            if (insertedAt instanceof java.sql.Timestamp) {
                java.sql.Timestamp ts = (java.sql.Timestamp) insertedAt;
                // Convert to LocalDateTime and format with microsecond precision
                java.time.LocalDateTime ldt = ts.toLocalDateTime();
                String formatted = at.ac.tuwien.ifs.dbrepo.mapper.DataMapper.mariaDbFormatter.format(ldt) + "+00:00";
                tuple.put("inserted_at", formatted);
            }
        }
        
        // Format deleted_at timestamp
        if (tuple.get("deleted_at") != null) {
            Object deletedAt = tuple.get("deleted_at");
            if (deletedAt instanceof java.sql.Timestamp) {
                java.sql.Timestamp ts = (java.sql.Timestamp) deletedAt;
                // Convert to LocalDateTime and format with microsecond precision
                java.time.LocalDateTime ldt = ts.toLocalDateTime();
                String formatted = at.ac.tuwien.ifs.dbrepo.mapper.DataMapper.mariaDbFormatter.format(ldt) + "+00:00";
                tuple.put("deleted_at", formatted);
            }
        }
    }

    /**
     * Formats timestamps in TuplesWithTimestampsDto with microsecond precision
     */
    private void formatTimestampsWithMicrosecondPrecision(TuplesWithTimestampsDto tuplesWithTimestamps) {
        if (tuplesWithTimestamps.getTuples() != null) {
            for (TuplesWithTimestampsDto.TupleWithTimestampsDto tuple : tuplesWithTimestamps.getTuples()) {
                // Format inserted_at timestamp
                if (tuple.getInsertedAt() != null) {
                    // Convert Instant to LocalDateTime for formatting, then back to Instant
                    java.time.LocalDateTime ldt = tuple.getInsertedAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
                    String formatted = at.ac.tuwien.ifs.dbrepo.mapper.DataMapper.mariaDbFormatter.format(ldt) + "+00:00";
                    tuple.setInsertedAt(java.time.Instant.parse(formatted.replace("+00:00", "Z")));
                }
                
                // Format deleted_at timestamp
                if (tuple.getDeletedAt() != null) {
                    // Convert Instant to LocalDateTime for formatting, then back to Instant
                    java.time.LocalDateTime ldt = tuple.getDeletedAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
                    String formatted = at.ac.tuwien.ifs.dbrepo.mapper.DataMapper.mariaDbFormatter.format(ldt) + "+00:00";
                    tuple.setDeletedAt(java.time.Instant.parse(formatted.replace("+00:00", "Z")));
                }
            }
        }
    }

    private void putIfColumnExists(ResultSet rs, Map<String, Object> map, String column) {
        try {
            Object v = rs.getObject(column);
            map.put(column, v);
        } catch (Exception ignored) {
        }
    }

    @Override
    @Timed(value = "dbrepo_data_update_tuple", description = "Time spent updating a table tuple", histogram = true)
    public void updateTuple(DatabaseDto database, TableDto table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException {
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
    public void processReplicationTimestamps(DatabaseDto database, TableDto table,
                                          List<Map<String, Object>> timestamps) throws SQLException, QueryMalformedException {
        if (timestamps == null || timestamps.isEmpty()) {
            log.info("No timestamps to process");
            return;
        }
        
        log.info("Processing {} replication timestamps", timestamps.size());
        
        // Convert the received timestamps to TupleReplicationTimestamp objects
        List<TupleReplicationTimestamp> timestampsToSave = new ArrayList<>();
        for (Map<String, Object> ts : timestamps) {
            try {
                // Replace the replica URL with the current site URL to avoid duplicates
                TupleReplicationTimestamp timestamp = TupleReplicationTimestamp.builder()
                    .siteUrl((String) ts.get("siteUrl"))
                    .replicationId((String) ts.get("replicationId"))
                    .databaseId(UUID.fromString((String) ts.get("databaseId")))
                    .tableId(UUID.fromString((String) ts.get("tableId")))
                    .rowStart(parseTimestamp((String) ts.get("rowStart")))
                    .rowEnd(parseTimestamp((String) ts.get("rowEnd")))
                    .build();
                timestampsToSave.add(timestamp);
            } catch (Exception e) {
                log.error("Failed to process timestamp {}: {}", ts, e.getMessage());
            }
        }
        
        if (!timestampsToSave.isEmpty()) {
            try {
                // Ensure the table exists before saving
                replicationTimestampService.ensureTableExists(database);
                replicationTimestampService.saveReplicationTimestamps(database, timestampsToSave);
                log.info("Successfully saved {} replication timestamps to database", timestampsToSave.size());
            } catch (Exception e) {
                log.error("Failed to save replication timestamps: {}", e.getMessage(), e);
                throw new QueryMalformedException("Failed to save replication timestamps: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Parse microsecond timestamp string to SQL Timestamp
     */
    private java.sql.Timestamp parseTimestamp(String timestampStr) {
        if (timestampStr == null) return null;
        
        try {
            // Handle timestamps with timezone (e.g., "2025-08-25 06:14:19.776954+00:00")
            if (timestampStr.contains("+") || timestampStr.contains("-") && timestampStr.lastIndexOf("-") > 10) {
                String withoutTz = timestampStr.substring(0, timestampStr.length() - 6);
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(withoutTz, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                return java.sql.Timestamp.valueOf(ldt);
            } else {
                // Handle timestamps without timezone (e.g., "2025-08-25 06:14:19.776954")
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(timestampStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                return java.sql.Timestamp.valueOf(ldt);
            }
        } catch (Exception e) {
            log.error("Failed to parse timestamp: {} - Error: {}", timestampStr, e.getMessage());
            throw new IllegalArgumentException("Failed to parse timestamp: " + timestampStr, e);
        }
    }

    @Override
    public boolean checkTuplesAfterTimestamp(DatabaseDto database, 
                                           java.time.Instant timestamp, 
                                           String replicaDatabaseId) throws SQLException, 
                                           QueryMalformedException {
        log.info("=== CHECKING TUPLES AFTER TIMESTAMP (DATABASE LEVEL) ===");
        log.info("Database: {} ({})", database.getName(), database.getInternalName());
        log.info("Timestamp: {}", timestamp);
        log.info("Replica Database ID: {}", replicaDatabaseId);
        log.info("Creation Location: {}", database.getCreationLocation());
        log.info("Container Host: {}:{}", database.getContainer().getHost(), database.getContainer().getPort());

        // Check each table for new tuples after the timestamp
        boolean hasNewTuples = false;
        
        if (database.getTables() != null && !database.getTables().isEmpty()) {
            log.info("Checking {} tables for new tuples...", database.getTables().size());
            
            for (TableDto table : database.getTables()) {
                try {
                    log.info("Checking table: {} ({})", table.getName(), table.getInternalName());
                    
                    // Get current tuple count (null timestamp = current time)
                    Long currentCount = getCount(database, table.getInternalName(), null);
                    log.info("Current tuple count: {}", currentCount);
                    
                    // Get tuple count at the specified timestamp
                    Long timestampCount = getCount(database, table.getInternalName(), timestamp);
                    log.info("Tuple count at timestamp {}: {}", timestamp, timestampCount);
                    
                    // Calculate new tuples
                    Long newTuplesCount = currentCount - timestampCount;
                    log.info("New tuples since timestamp: {}", newTuplesCount);
                    
                    if (newTuplesCount > 0) {
                        hasNewTuples = true;
                        log.info("✅ Table {} has {} new tuples since timestamp {}", 
                                table.getInternalName(), newTuplesCount, timestamp);
                        break; // Found new tuples, no need to check other tables
                    } else {
                        log.info("ℹ️ Table {} has no new tuples since timestamp {}", 
                                table.getInternalName(), timestamp);
                    }
                    
                } catch (Exception e) {
                    log.error("❌ Error checking table {}: {}", table.getInternalName(), e.getMessage());
                    // Continue with other tables
                }
            }
        } else {
            log.info("No tables found in database");
        }

        if (hasNewTuples) {
            log.info("🔍 Found new tuples since timestamp {}", timestamp);
        } else {
            log.info("✅ No new tuples found since timestamp {}", timestamp);
        }

        log.info("=== END CHECKING TUPLES AFTER TIMESTAMP (DATABASE LEVEL) ===");
        return hasNewTuples;
    }

    @Override
    public java.util.List<TuplesWithTimestampsDto.TupleWithTimestampsDto> loadNewTuplesAfterTimestamp(DatabaseDto database, 
                                                               java.time.Instant timestamp) throws SQLException,
                                                               QueryMalformedException {
        log.info("=== LOADING NEW TUPLES AFTER TIMESTAMP (DATABASE LEVEL) ===");
        log.info("Database: {} ({})", database.getName(), database.getInternalName());
        log.info("Timestamp: {}", timestamp);

        List<TuplesWithTimestampsDto.TupleWithTimestampsDto> allNewTuples = new java.util.ArrayList<>();
        
        if (database.getTables() != null && !database.getTables().isEmpty()) {
            log.info("Loading new tuples from {} tables...", database.getTables().size());
            
            for (TableDto table : database.getTables()) {
                try {
                        // Load the actual tuples from the table
                        TuplesWithTimestampsDto tableTuplesWithTimestamps = loadNewTuplesFromTable(database, table, timestamp);
                        // Add all tuples with timestamps to the main list
                        allNewTuples.addAll(tableTuplesWithTimestamps.getTuples());
                        
                        log.info("✅ Loaded {} tuples from table {}", tableTuplesWithTimestamps.getTuples().size(), table.getInternalName());

                    
                } catch (Exception e) {
                    log.error("❌ Error loading tuples from table {}: {}", table.getInternalName(), e.getMessage());
                    // Continue with other tables
                }
            }
        } else {
            log.info("No tables found in database");
        }

        log.info("=== END LOADING NEW TUPLES AFTER TIMESTAMP (DATABASE LEVEL) ===");
        log.info("Total new tuples loaded: {}", allNewTuples.size());
        
        return allNewTuples;
    }

    /**
     * Loads new tuples from a specific table after the given timestamp
     */
    private TuplesWithTimestampsDto loadNewTuplesFromTable(DatabaseDto database, TableDto table, java.time.Instant timestamp) 
            throws SQLException, QueryMalformedException {
        
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        java.util.List<TuplesWithTimestampsDto.TupleWithTimestampsDto> tuples = new java.util.ArrayList<>();
        
        try {
            // Build query to select new tuples after timestamp with timestamps
            // Using system versioning to get tuples inserted after the timestamp
            final StringBuilder select = new StringBuilder("SELECT ");
            final int[] colIdx = new int[]{0};
            for (ColumnDto c : table.getColumns()) {
                select.append(colIdx[0]++ == 0 ? "" : ", ")
                        .append("`")
                        .append(c.getInternalName())
                        .append("`");
            }
            
            // Always include replication_key column to avoid cache issues
            select.append(", `replication_key`");
            select.append(", ROW_START AS inserted_at, ROW_END AS deleted_at");
            
            select.append(" FROM `")
                    .append(database.getInternalName())
                    .append("`.`")
                    .append(table.getInternalName())
                    .append("` FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(DataMapper.mariaDbFormatter.format(java.time.Instant.now().atZone(java.time.ZoneOffset.UTC).toLocalDateTime()))
                    .append("' WHERE ROW_START > TIMESTAMP '")
                    .append(DataMapper.mariaDbFormatter.format(timestamp.atZone(java.time.ZoneOffset.UTC).toLocalDateTime()))
                    .append("'");

            log.debug("Executing query: {}", select.toString());
            
            final PreparedStatement selectStmt = connection.prepareStatement(select.toString());
            final ResultSet rs = selectStmt.executeQuery();
            
            while (rs.next()) {
                java.util.Map<String, Object> tupleData = new java.util.HashMap<>();
                
                // Extract all column values
                for (ColumnDto column : table.getColumns()) {
                    Object value = rs.getObject(column.getInternalName());
                    tupleData.put(column.getInternalName(), value);
                }
                
                // Extract timestamps and replication key
                Object rowStart = rs.getObject("inserted_at");
                Object rowEnd = rs.getObject("deleted_at");
                Object replicationKey = rs.getObject("replication_key");
                
                // Create TupleWithTimestampsDto and add to list
                TuplesWithTimestampsDto.TupleWithTimestampsDto tuple = TuplesWithTimestampsDto.TupleWithTimestampsDto.builder()
                    .data(tupleData)
                    .insertedAt(rowStart instanceof java.sql.Timestamp ? ((java.sql.Timestamp) rowStart).toInstant() : null)
                    .deletedAt(rowEnd instanceof java.sql.Timestamp ? ((java.sql.Timestamp) rowEnd).toInstant() : null)
                    .replicationKey(replicationKey != null ? replicationKey.toString() : null)
                    .build();
                tuples.add(tuple);
            }
            
            connection.commit();
            
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to load new tuples from table {}.{}: {}", 
                    database.getInternalName(), table.getInternalName(), e.getMessage());
            throw new QueryMalformedException("Failed to load new tuples from table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        
        TuplesWithTimestampsDto result = TuplesWithTimestampsDto.builder()
            .tuples(tuples)
            .build();
        
        // Note: Timestamp formatting removed to avoid parsing errors
        // The timestamps are already in the correct Instant format from SQL Timestamps
        
        return result;
    }

}
