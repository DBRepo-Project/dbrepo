package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.ColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.DataService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationActivationService;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpMethod;

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
    private final ReplicationService replicationService;

    @Autowired
    private ReplicationActivationService activation;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    @Autowired
    public TableServiceMariaDbImpl(DataMapper dataMapper, MariaDbMapper mariaDbMapper, SubsetService subsetService,
                                   StorageService storageService, DataService computeService,
                                   ReplicationService replicationService) {
        this.dataMapper = dataMapper;
        this.mariaDbMapper = mariaDbMapper;
        this.subsetService = subsetService;
        this.storageService = storageService;
        this.computeService = computeService;
        this.replicationService = replicationService;
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
            final String sql = mariaDbMapper.tableCreateDtoToCreateTableRawQuery(database.getInternalName(), data);
            if (ReplicationSites.isReplica(data.getCreationLocation(), baseUrl)) {
                ReplicaDdl.createTable(connection, database.getInternalName(), tableName, sql);
            } else {
                try (var statement = connection.prepareStatement(sql)) {
                    statement.execute();
                }
            }
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
        throw new QueryMalformedException("Physical deletion would destroy historical queries; archive the table through the metadata service");
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
        try (var mutation = activation == null ? null : activation.beginMutation(database, table)) {
            importDatasetUnlocked(mutation == null ? database : mutation.database(),
                    mutation == null ? table : mutation.table(), data);
        }
    }

    private void importDatasetUnlocked(Database database, Table table, ImportDto data) throws MalformedException,
            SQLException, QueryMalformedException, StorageUnavailableException, TableMalformedException,
            StorageNotFoundException {
        if (ReplicationSites.isReplica(database.getCreationLocation(), baseUrl)
                || ReplicationSites.isReplica(table.getCreationLocation(), baseUrl)) {
            throw new QueryMalformedException("Cannot import a dataset into a remote read-only table");
        }
        final boolean replicate = replicationService.isEnabled(database, table);
        if (replicate) {
            requireReplicationKeyColumn(table);
        }
        final List<String> columns = table.getColumns()
                .stream()
                .map(Column::getInternalName)
                .toList();
        final Dataset<Row> dataset = importCsv(columns, data);
        final String[] csvColumns = dataset.columns();
        final Map<String, Object> values = new LinkedHashMap<>();
        columns.forEach(column -> values.put(column, null));
        final TupleDto tuple = TupleDto.builder().data(values).build();
        final List<UUID> eventIds = new ArrayList<>();
        try (ComboPooledDataSource dataSource = getDataSource(database);
             Connection connection = dataSource.getConnection()) {
            replicationService.prepare(connection, database, table);
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    importUpsertQuery(database, table, columns, replicate))) {
                // Stream Spark partitions; no shared staging table or per-row commits.
                final Iterator<Row> rows = dataset.toLocalIterator();
                while (rows.hasNext()) {
                    final Row row = rows.next();
                    values.replaceAll((column, value) -> null);
                    for (int index = 0; index < csvColumns.length; index++) {
                        values.put(csvColumns[index], row.get(index));
                    }
                    ensureReplicationKey(table, tuple);
                    int index = 1;
                    for (Object value : values.values()) {
                        // Retain INSERT SELECT's database-side conversion of CSV text, including BLOB literals.
                        statement.setObject(index++, value);
                    }
                    if (replicate) {
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException("Imported tuple was not returned by the database");
                            }
                            // POST is an identity-keyed upsert at the receiver, including duplicate CSV rows.
                            eventIds.add(replicationService.enqueue(connection, tupleWithTimestamps(result, columns),
                                    database, table, HttpMethod.POST));
                        }
                    } else {
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw new MalformedException("Failed to import dataset: " + e.getMessage(), e);
            }
        }
        replicationService.dispatchCommitted(database, eventIds);
        storageService.deleteObject(data.getLocation());
        log.info("Imported dataset into table {}.{}", database.getInternalName(), table.getInternalName());
    }

    private String importUpsertQuery(Database database, Table table, List<String> columns, boolean replicate) {
        final List<String> quoted = columns.stream().map(column -> "`" + column.replace("`", "``") + "`").toList();
        final List<String> updates = quoted.stream().filter(column -> !"`replication_key`".equals(column))
                .map(column -> column + " = VALUES(" + column + ")").toList();
        // Preserve identity even when a different unique key selects the existing row.
        final String onDuplicate = updates.isEmpty() ? "`replication_key` = `replication_key`" : String.join(", ", updates);
        return "INSERT INTO `" + database.getInternalName().replace("`", "``") + "`.`"
                + table.getInternalName().replace("`", "``") + "` (" + String.join(", ", quoted) + ") VALUES ("
                + String.join(", ", Collections.nCopies(columns.size(), "?")) + ") ON DUPLICATE KEY UPDATE "
                + onDuplicate + (replicate ? " RETURNING " + String.join(", ", quoted)
                + ", ROW_START AS inserted_at, ROW_END AS deleted_at" : "");
    }

    private Dataset<Row> importCsv(List<String> columns, ImportDto data) throws StorageNotFoundException,
            StorageUnavailableException, MalformedException, TableMalformedException {
        try {
            return computeService.getCsv(columns, data.getLocation(), String.valueOf(data.getSeparator()), data.getHeader());
        } catch (MalformedException | StorageUnavailableException e) {
            // Spark 4's column-count AnalysisException is wrapped as a storage error by getCsv.
            if (e instanceof StorageUnavailableException
                    && (e.getMessage() == null || !e.getMessage().contains("[ASSIGNMENT_ARITY_MISMATCH]"))) {
                throw e;
            }
            if (!columns.contains("replication_key")) {
                throw new MalformedException("CSV columns do not match the table: " + e.getMessage(), e);
            }
            // getCsv validates the positional column count before any target data is written.
            final List<String> withoutKey = columns.stream().filter(column -> !"replication_key".equals(column)).toList();
            return importCsv(withoutKey, data);
        }
    }

    @Override
    @Timed(value = "dbrepo_data_delete_tuple", description = "Time spent deleting a table tuple", histogram = true)
    public void deleteTuple(Database database, Table table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        try (var mutation = activation == null ? null : activation.beginMutation(database, table)) {
            deleteTupleUnlocked(mutation == null ? database : mutation.database(),
                    mutation == null ? table : mutation.table(), data);
        }
    }

    private void deleteTupleUnlocked(Database database, Table table, TupleDeleteDto data) throws SQLException,
            TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        if (replicationService.isEnabled(database, table)) {
            deleteTupleWithTimestamps(database, table, data);
            return;
        }
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawDeleteQuery(
                    database.getInternalName(), table, data));
            for (String column : data.getKeys().keySet()) {
                bindTupleValue(statement,
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
        try (var mutation = activation == null ? null : activation.beginMutation(database, table)) {
            createTupleUnlocked(mutation == null ? database : mutation.database(),
                    mutation == null ? table : mutation.table(), data);
        }
    }

    private void createTupleUnlocked(Database database, Table table, TupleDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        if (replicationService.isEnabled(database, table)) {
            createTupleWithTimestamps(database, table, data);
            return;
        }
        log.trace("create tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawCreateQuery(
                    database.getInternalName(), table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                bindTupleValue(statement,
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
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            replicationService.prepare(connection, database, table);
            connection.setAutoCommit(false);
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawCreateQuery(
                    database.getInternalName(), table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                bindTupleValue(statement,
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
            final UUID eventId = replicationService.enqueue(connection, tuple, database, table, HttpMethod.POST);
            connection.commit();
            replicationService.dispatchCommitted(database, eventId == null ? List.of() : List.of(eventId));
            log.info("Created tuple with timestamps in table: {}.{}", database.getInternalName(),
                    table.getInternalName());
            return tuple;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create tuple with timestamps: {}", e.getMessage());
            throw new QueryMalformedException("Failed to create tuple with timestamps: " + e.getMessage(), e);
        } catch (RuntimeException | QueryMalformedException | TableMalformedException
                 | StorageUnavailableException | StorageNotFoundException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.close();
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_upsert_tuple_with_timestamps", description = "Time spent upserting a table tuple with replication timestamps", histogram = true)
    public TupleWithTimestampsDto upsertTupleWithTimestamps(Database database, Table table, TupleDto data)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException {
        log.trace("upsert tuple with timestamps: {}", data);
        ensureReplicationKey(table, data);
        final Object replicationKey = data.getData().get("replication_key");
        final Optional<TupleWithTimestampsDto> existing = findCurrentTupleWithTimestamps(database, table,
                replicationKeyLookup(replicationKey));
        if (existing.isPresent()) {
            return updateReplicationTupleWithTimestamps(database, table, data, existing.get());
        }
        try {
            return createTupleWithTimestamps(database, table, data);
        } catch (QueryMalformedException e) {
            if (!isDuplicateKey(e)) {
                throw e;
            }
            final Optional<TupleWithTimestampsDto> duplicate = findCurrentTupleWithTimestamps(database, table,
                    replicationKeyLookup(replicationKey));
            if (duplicate.isEmpty()) {
                throw e;
            }
            return updateReplicationTupleWithTimestamps(database, table, data, duplicate.get());
        }
    }

    @Override
    @Timed(value = "dbrepo_data_update_tuple", description = "Time spent updating a table tuple", histogram = true)
    public void updateTuple(Database database, Table table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        try (var mutation = activation == null ? null : activation.beginMutation(database, table)) {
            updateTupleUnlocked(mutation == null ? database : mutation.database(),
                    mutation == null ? table : mutation.table(), data);
        }
    }

    private void updateTupleUnlocked(Database database, Table table, TupleUpdateDto data) throws SQLException,
            QueryMalformedException, TableMalformedException, StorageUnavailableException, StorageNotFoundException {
        if (replicationService.isEnabled(database, table)) {
            updateTupleWithTimestamps(database, table, data);
            return;
        }
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
                bindTupleValue(statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            /* set key(s) */
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                bindTupleValue(statement,
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
        if (data.getData().containsKey("replication_key")) {
            throw new QueryMalformedException("The replication key cannot be changed");
        }
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            replicationService.prepare(connection, database, table);
            connection.setAutoCommit(false);
            final List<String> affectedKeys = lockReplicationKeys(connection, database, table, data.getKeys());
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawUpdateQuery(
                    database.getInternalName(), table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                bindTupleValue(statement,
                        getColumnType(table.getColumns(), entry.getKey()), idx[0], entry.getKey(), entry.getValue());
                idx[0]++;
            }
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                bindTupleValue(statement,
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
            final List<UUID> eventIds = new ArrayList<>();
            final TupleWithTimestampsDto tuple = enqueueChangedTuples(connection, database, table, affectedKeys,
                    HttpMethod.PUT, eventIds);
            connection.commit();
            replicationService.dispatchCommitted(database, eventIds);
            log.info("Updated tuple with timestamps in table: {}.{}", database.getInternalName(),
                    table.getInternalName());
            return tuple;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update tuple with timestamps: {}", e.getMessage());
            throw new QueryMalformedException("Failed to update tuple with timestamps: " + e.getMessage(), e);
        } catch (RuntimeException | QueryMalformedException | TableMalformedException
                 | StorageUnavailableException | StorageNotFoundException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.close();
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
            replicationService.prepare(connection, database, table);
            connection.setAutoCommit(false);
            final List<String> affectedKeys = lockReplicationKeys(connection, database, table, data.getKeys());
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawDeleteQuery(
                    database.getInternalName(), table, data));
            for (String column : data.getKeys().keySet()) {
                bindTupleValue(statement,
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
            final List<UUID> eventIds = new ArrayList<>();
            final TupleWithTimestampsDto tuple = enqueueChangedTuples(connection, database, table, affectedKeys,
                    HttpMethod.DELETE, eventIds);
            connection.commit();
            replicationService.dispatchCommitted(database, eventIds);
            log.info("Deleted tuple with timestamps from table: {}.{}", database.getInternalName(),
                    table.getInternalName());
            return tuple;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete tuple with timestamps: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete tuple with timestamps: " + e.getMessage(), e);
        } catch (RuntimeException | QueryMalformedException | TableMalformedException
                 | StorageUnavailableException | StorageNotFoundException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.close();
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_get_replication_data", description = "Time spent paging table data for replication bootstrap", histogram = true)
    public ReplicationSynchronisationDataDto getReplicationData(Database database, Table table, long page, long size,
                                                               String siteUrl)
            throws SQLException, QueryMalformedException, TableMalformedException {
        requireReplicationKeyColumn(table);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            try (var session = connection.createStatement()) {
                session.execute("SET time_zone = '+00:00'");
            }
            final long start = System.currentTimeMillis();
            final List<String> columns = table.getColumns()
                    .stream()
                    .map(Column::getInternalName)
                    .distinct()
                    .toList();
            final PreparedStatement statement = connection.prepareStatement(replicationDataSelectQuery(database,
                    table, columns));
            statement.setLong(1, size);
            statement.setLong(2, page * size);
            final ResultSet resultSet = statement.executeQuery();
            final List<TupleWithTimestampsDto> tuples = new ArrayList<>();
            final List<TupleReplicationTimestampDto> timestamps = new ArrayList<>();
            while (resultSet.next()) {
                final TupleWithTimestampsDto tuple = tupleWithTimestamps(resultSet, columns);
                if (tuple.getReplicationKey() == null || tuple.getReplicationKey().isBlank()) {
                    throw new QueryMalformedException("Replication data contains tuple without replication_key");
                }
                tuples.add(tuple);
                timestamps.add(TupleReplicationTimestampDto.builder()
                        .siteUrl(normalizeSiteUrl(siteUrl))
                        .replicationId(tuple.getReplicationKey())
                        .databaseId(database.getId())
                        .tableId(table.getId())
                        .rowStart(tuple.getInsertedAt())
                        .rowEnd(tuple.getDeletedAt())
                        .build());
            }
            log.atDebug()
                    .setMessage("get replication data: " + table.getInternalName() + "."
                            + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "table_get_replication_data")
                    .log();
            connection.commit();
            log.info("Found {} replication tuple(s) in table {}.{}", tuples.size(), database.getInternalName(),
                    table.getInternalName());
            return ReplicationSynchronisationDataDto.builder()
                    .tuples(tuples)
                    .replicationTimestamps(timestamps)
                    .build();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find replication data from table {}.{}: {}", database.getInternalName(),
                    table.getInternalName(), e.getMessage());
            throw new QueryMalformedException("Failed to find replication data from table "
                    + database.getInternalName() + "." + table.getInternalName() + ": " + e.getMessage(), e);
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

    private void bindTupleValue(PreparedStatement statement, ColumnType type, int index, String name, Object value)
            throws SQLException, StorageUnavailableException, StorageNotFoundException {
        switch (type) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB -> {
                // Source strings are S3 keys; replication endpoints must decode wire base64 to byte[].
                if (value == null) {
                    statement.setNull(index, java.sql.Types.BLOB);
                } else if (value instanceof byte[] bytes) {
                    statement.setBytes(index, bytes);
                } else if (value instanceof String key) {
                    statement.setBytes(index, storageService.getBytes(key));
                } else {
                    throw new IllegalArgumentException("BLOB value must be an object key or byte array: " + name);
                }
            }
            default -> mariaDbMapper.prepareStatementWithColumnTypeObject(storageService, statement, type, index,
                    name, value);
        }
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

    private Map<String, Object> replicationKeyLookup(Object replicationKey) {
        final Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("replication_key", replicationKey);
        return keys;
    }

    private TupleWithTimestampsDto updateReplicationTupleWithTimestamps(Database database, Table table, TupleDto data,
                                                                       TupleWithTimestampsDto existing)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException {
        final Map<String, Object> update = new LinkedHashMap<>(data.getData());
        update.remove("replication_key");
        if (update.isEmpty()) {
            return existing;
        }
        return updateTupleWithTimestamps(database, table, TupleUpdateDto.builder()
                .keys(replicationKeyLookup(data.getData().get("replication_key")))
                .data(update)
                .build());
    }

    private Optional<TupleWithTimestampsDto> findCurrentTupleWithTimestamps(Database database, Table table,
                                                                           Map<String, Object> keys)
            throws SQLException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final Optional<TupleWithTimestampsDto> tuple = selectCurrentTupleWithTimestamps(connection, database, table,
                    keys);
            connection.commit();
            return tuple;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to select current tuple with timestamps from table {}.{}: {}",
                    database.getInternalName(), table.getInternalName(), e.getMessage());
            throw new QueryMalformedException("Failed to select current tuple with timestamps from table "
                    + database.getInternalName() + "." + table.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    private boolean isDuplicateKey(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && ("23000".equals(sqlException.getSQLState()) || sqlException.getErrorCode() == 1062)) {
                return true;
            }
            final String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate")
                    && message.toLowerCase().contains("key")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void requireReplicationKeyColumn(Table table) throws TableMalformedException {
        if (!hasColumn(table, "replication_key")) {
            throw new TableMalformedException("Table is missing the replication_key column");
        }
    }

    private List<String> lockReplicationKeys(Connection connection, Database database, Table table,
                                              Map<String, Object> keys)
            throws SQLException, QueryMalformedException, TableMalformedException, StorageUnavailableException,
            StorageNotFoundException {
        requireReplicationKeyColumn(table);
        if (keys == null || keys.isEmpty()) {
            throw new QueryMalformedException("Tuple mutation requires lookup keys");
        }
        final List<String> predicates = new ArrayList<>();
        for (String key : keys.keySet()) {
            getColumnType(table.getColumns(), key);
            predicates.add("`" + key.replace("`", "``") + "` <=> ?");
        }
        final String sql = "SELECT replication_key FROM `" + database.getInternalName().replace("`", "``")
                + "`.`" + table.getInternalName().replace("`", "``") + "` WHERE "
                + String.join(" AND ", predicates) + " ORDER BY replication_key FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Map.Entry<String, Object> key : keys.entrySet()) {
                bindTupleValue(statement,
                        getColumnType(table.getColumns(), key.getKey()), index++, key.getKey(), key.getValue());
            }
            final List<String> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(rows.getString(1));
                }
            }
            return result;
        }
    }

    private TupleWithTimestampsDto enqueueChangedTuples(Connection connection, Database database, Table table,
                                                         List<String> keys, HttpMethod method, List<UUID> eventIds)
            throws SQLException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        TupleWithTimestampsDto first = null;
        for (String key : keys) {
            final TupleWithTimestampsDto tuple = selectTupleWithTimestamps(connection, database, table,
                    replicationKeyLookup(key));
            final UUID eventId = replicationService.enqueue(connection, tuple, database, table, method);
            if (eventId != null) {
                eventIds.add(eventId);
            }
            if (first == null) {
                first = tuple;
            }
        }
        return first;
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
        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            int bind = 1;
            for (Map.Entry<String, Object> entry : keys.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                bindTupleValue(statement,
                        getColumnType(table.getColumns(), entry.getKey()), bind++, entry.getKey(), entry.getValue());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new QueryMalformedException("Failed to select tuple with timestamps");
                }
                return tupleWithTimestamps(resultSet, columns);
            }
        }
    }

    private Optional<TupleWithTimestampsDto> selectCurrentTupleWithTimestamps(Connection connection, Database database,
                                                                             Table table, Map<String, Object> keys)
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
                .append("` WHERE ");
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
            bindTupleValue(statement,
                    getColumnType(table.getColumns(), entry.getKey()), bind++, entry.getKey(), entry.getValue());
        }
        final ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            return Optional.empty();
        }
        return Optional.of(tupleWithTimestamps(resultSet, columns));
    }

    private String replicationDataSelectQuery(Database database, Table table, List<String> columns) {
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
                .append("` ORDER BY `replication_key` ASC LIMIT ? OFFSET ?;");
        return query.toString();
    }

    private TupleWithTimestampsDto tupleWithTimestamps(ResultSet resultSet, List<String> columns) throws SQLException {
        final Map<String, Object> data = new LinkedHashMap<>();
        final var metadata = resultSet.getMetaData();
        for (String column : columns) {
            final String type = metadata.getColumnTypeName(resultSet.findColumn(column)).toUpperCase(Locale.ROOT);
            data.put(column, switch (type) {
                case "DATE", "TIME", "TIMESTAMP", "DATETIME" -> resultSet.getString(column);
                case "BIT", "BINARY", "VARBINARY", "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB" ->
                        resultSet.getBytes(column);
                default -> resultSet.getObject(column);
            });
        }
        final Instant insertedAt = timestampToInstant(resultSet.getString("inserted_at"));
        final Instant deletedAt = normaliseRowEnd(timestampToInstant(resultSet.getString("deleted_at")));
        return TupleWithTimestampsDto.builder()
                .data(data)
                .insertedAt(insertedAt)
                .deletedAt(deletedAt)
                .replicationKey(data.get("replication_key") != null ? String.valueOf(data.get("replication_key")) : null)
                .build();
    }

    private String normalizeSiteUrl(String siteUrl) {
        if (siteUrl == null) {
            return null;
        }
        String normalized = siteUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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
