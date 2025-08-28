package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import com.google.common.hash.Hashing;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
public class DatabaseServiceMariaDbImpl extends DataConnector implements DatabaseService {

    private final DataMapper dataMapper;
    private final MariaDbMapper mariaDbMapper;
    private final TableService tableService;

    @Autowired
    public DatabaseServiceMariaDbImpl(DataMapper dataMapper, MariaDbMapper mariaDbMapper, TableService tableService) {
        this.dataMapper = dataMapper;
        this.mariaDbMapper = mariaDbMapper;
        this.tableService = tableService;
    }

    @Override
    public ViewDto inspectView(DatabaseDto database, String viewName) throws SQLException, ViewNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* obtain only view metadata */
            long start = System.currentTimeMillis();
            final PreparedStatement statement1 = connection.prepareStatement(mariaDbMapper.databaseTableSelectRawQuery());
            statement1.setString(1, database.getInternalName());
            statement1.setString(2, viewName);
            log.trace("1={}, 2={}", database.getInternalName(), viewName);
            final ResultSet resultSet1 = statement1.executeQuery();
            log.atDebug()
                    .setMessage("inspected view: " + viewName + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_view_schema")
                    .log();
            if (!resultSet1.next()) {
                throw new ViewNotFoundException("Failed to find view in the information schema");
            }
            final ViewDto view = dataMapper.schemaResultSetToView(database, resultSet1);
            view.setDatabaseId(database.getId());
            view.setOwner(database.getOwner());
            /* obtain view columns */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, view.getInternalName());
            log.trace("1={}, 2={}", database.getInternalName(), view.getInternalName());
            final ResultSet resultSet2 = statement2.executeQuery();
            log.atDebug()
                    .setMessage("inspect view columns: " + viewName + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_view_columns")
                    .log();
            TableDto tmp = TableDto.builder()
                    .columns(new LinkedList<>())
                    .build();
            while (resultSet2.next()) {
                tmp = dataMapper.resultSetToTable(resultSet2, tmp);
            }
            view.setColumns(tmp.getColumns()
                    .stream()
                    .map(dataMapper::columnDtoToViewColumnDto)
                    .toList());
            view.getColumns()
                    .forEach(column -> column.setDatabaseId(database.getId()));
            log.debug("obtained metadata for view {}.{}", database.getInternalName(), view.getInternalName());
            return view;
        } finally {
            dataSource.close();
        }
    }

    @Override
    public TableDto createTable(DatabaseDto database, CreateTableDto data) throws SQLException,
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
        return inspectTable(database, tableName);
    }

    @Override
    public ViewDto createView(DatabaseDto database, String viewName, String query) throws SQLException,
            ViewMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        ViewDto view = ViewDto.builder()
                .name(viewName)
                .internalName(mariaDbMapper.nameToInternalName(viewName))
                .query(query)
                .queryHash(Hashing.sha256()
                        .hashString(query, StandardCharsets.UTF_8)
                        .toString())
                .isPublic(database.getIsPublic())
                .owner(database.getOwner())
                .identifiers(new LinkedList<>())
                .isInitialView(false)
                .databaseId(database.getId())
                .columns(new LinkedList<>())
                .build();
        try {
            /* create view if not exists */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.viewCreateRawQuery(view.getInternalName(), query))
                    .execute();
            log.atDebug()
                    .setMessage("created view: " + view.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_view")
                    .log();
            /* select view columns */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, view.getInternalName());
            final ResultSet resultSet2 = statement2.executeQuery();
            log.atDebug()
                    .setMessage("created view: " + view.getInternalName() + "." + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_view_columns")
                    .log();
            while (resultSet2.next()) {
                view = dataMapper.resultSetToTable(resultSet2, view);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to create view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created view with name {}", view.getName());
        return view;
    }

    @Override
    public List<ViewDto> exploreViews(DatabaseDto database) throws SQLException, DatabaseMalformedException,
            ViewNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<ViewDto> views = new LinkedList<>();
        try {
            /* inspect tables before views */
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.databaseViewsSelectRawQuery());
            statement.setString(1, database.getInternalName());
            final long start = System.currentTimeMillis();
            final ResultSet resultSet1 = statement.executeQuery();
            log.atDebug()
                    .setMessage("explored views in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "select_views")
                    .log();
            while (resultSet1.next()) {
                final String viewName = resultSet1.getString(1);
                if (viewName.length() == 64) {
                    log.trace("view {}.{} seems to be a subset view (name length = 64), skip.", database.getInternalName(), viewName);
                    continue;
                }
                if (database.getViews().stream().anyMatch(v -> v.getInternalName().equals(viewName))) {
                    log.trace("view {}.{} already known to metadata database, skip.", database.getInternalName(), viewName);
                    continue;
                }
                if (database.getTables().stream().noneMatch(t -> t.getInternalName().equals(viewName))) {
                    views.add(inspectView(database, viewName));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get view schemas: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get view schemas: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Found {} view schema(s)", views.size());
        return views;
    }

    @Override
    public List<TableDto> exploreTables(DatabaseDto database) throws SQLException, TableNotFoundException,
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
            while (resultSet1.next()) {
                final String tableName = resultSet1.getString(1);
                if (database.getTables().stream().anyMatch(t -> t.getInternalName().equals(tableName))) {
                    log.trace("view {}.{} already known to metadata database, skip.", database.getInternalName(), tableName);
                    continue;
                }
                final TableDto table = inspectTable(database, tableName);
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
        log.info("Found {} table schema(s)", tables.size());
        return tables;
    }

    @Override
    public TableDto inspectTable(DatabaseDto database, String tableName) throws SQLException, TableNotFoundException {
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
            table.setOwner(database.getOwner());
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
    public void update(DatabaseDto database, UpdateUserPasswordDto data) throws SQLException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* update user password */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseSetPasswordQuery(data.getUsername(), data.getPassword()))
                    .execute();
            log.atDebug()
                    .setMessage("updated user password: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "update_user_password")
                    .log();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update user password in database: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to update user password in database: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated user password in database with id {}", database.getId());
    }

    @Override
    public java.util.Map<String, Object> checkTuplesAfterTimestamp(DatabaseDto database, 
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
        java.util.List<java.util.Map<String, Object>> tablesWithNewTuples = new java.util.ArrayList<>();
        
        if (database.getTables() != null && !database.getTables().isEmpty()) {
            log.info("Checking {} tables for new tuples...", database.getTables().size());
            
            for (TableDto table : database.getTables()) {
                try {
                    log.info("Checking table: {} ({})", table.getName(), table.getInternalName());
                    
                    // Get current tuple count (null timestamp = current time)
                    Long currentCount = tableService.getCount(database, table.getInternalName(), null);
                    log.info("Current tuple count: {}", currentCount);
                    
                    // Get tuple count at the specified timestamp
                    Long timestampCount = tableService.getCount(database, table.getInternalName(), timestamp);
                    log.info("Tuple count at timestamp {}: {}", timestamp, timestampCount);
                    
                    // Calculate new tuples
                    Long newTuplesCount = currentCount - timestampCount;
                    log.info("New tuples since timestamp: {}", newTuplesCount);
                    
                    if (newTuplesCount > 0) {
                        hasNewTuples = true;
                        log.info("✅ Table {} has {} new tuples since timestamp {}", 
                                table.getInternalName(), newTuplesCount, timestamp);
                        
                        // Add table info to the list
                        java.util.Map<String, Object> tableInfo = java.util.Map.of(
                            "tableName", table.getName(),
                            "tableInternalName", table.getInternalName(),
                            "newTuplesCount", newTuplesCount,
                            "currentCount", currentCount,
                            "timestampCount", timestampCount
                        );
                        tablesWithNewTuples.add(tableInfo);
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

        // Prepare response based on findings
        java.util.Map<String, Object> response;
        
        if (hasNewTuples) {
            log.info("🔍 Found new tuples in {} tables since timestamp {}", tablesWithNewTuples.size(), timestamp);
            
            response = java.util.Map.of(
                "status", "tuples_found",
                "databaseId", database.getId().toString(),
                "timestamp", timestamp.toString(),
                "replicaDatabaseId", replicaDatabaseId,
                "databaseName", database.getName(),
                "databaseInternalName", database.getInternalName(),
                "hasNewTuples", true,
                "tablesWithNewTuples", tablesWithNewTuples,
                "totalNewTuples", tablesWithNewTuples.stream()
                    .mapToLong(table -> (Long) table.get("newTuplesCount"))
                    .sum(),
                "todo", "Implement tuple handover mechanism to send new tuples to replica database " + replicaDatabaseId
            );
            
            log.info("TODO: Implement tuple handover to replica database {}", replicaDatabaseId);
            log.info("Tables with new tuples: {}", tablesWithNewTuples.stream()
                .map(table -> table.get("tableInternalName") + "(" + table.get("newTuplesCount") + ")")
                .collect(java.util.stream.Collectors.joining(", ")));
            
        } else {
            log.info("✅ No new tuples found since timestamp {}", timestamp);
            
            response = java.util.Map.of(
                "status", "no_tuples",
                "message", "No new tuples found after timestamp",
                "databaseId", database.getId().toString(),
                "timestamp", timestamp.toString(),
                "replicaDatabaseId", replicaDatabaseId,
                "databaseName", database.getName(),
                "databaseInternalName", database.getInternalName(),
                "hasNewTuples", false,
                "tablesWithNewTuples", new java.util.ArrayList<>(),
                "totalNewTuples", 0
            );
        }

        log.info("=== END CHECKING TUPLES AFTER TIMESTAMP (DATABASE LEVEL) ===");
        return response;
    }
}
