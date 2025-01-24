package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.*;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.DatabaseService;
import com.google.common.hash.Hashing;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service
public class DatabaseServiceMariaDbImpl extends DataConnector implements DatabaseService {

    private final DataMapper dataMapper;
    private final QueryConfig queryConfig;
    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public DatabaseServiceMariaDbImpl(DataMapper dataMapper, QueryConfig queryConfig, MariaDbMapper mariaDbMapper,
                                      MetadataMapper metadataMapper) {
        this.dataMapper = dataMapper;
        this.queryConfig = queryConfig;
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public ViewDto inspectView(DatabaseDto database, String viewName) throws SQLException, ViewNotFoundException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* obtain only view metadata */
            long start = System.currentTimeMillis();
            final PreparedStatement statement1 = connection.prepareStatement(mariaDbMapper.databaseViewSelectRawQuery());
            statement1.setString(1, database.getInternalName());
            statement1.setString(2, viewName);
            log.trace("1={}, 2={}", database.getInternalName(), viewName);
            final ResultSet resultSet1 = statement1.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            if (!resultSet1.next()) {
                throw new ViewNotFoundException("Failed to find view in the information schema");
            }
            final ViewDto view = dataMapper.schemaResultSetToView(database, resultSet1);
            view.setVdbid(database.getId());
            view.setOwner(database.getOwner());
            /* obtain view columns */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, view.getInternalName());
            log.trace("1={}, 2={}", database.getInternalName(), view.getInternalName());
            final ResultSet resultSet2 = statement2.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            TableDto tmp = TableDto.builder()
                    .columns(new LinkedList<>())
                    .build();
            while (resultSet2.next()) {
                tmp = dataMapper.resultSetToTable(resultSet2, tmp);
            }
            view.setColumns(tmp.getColumns()
                    .stream()
                    .map(metadataMapper::columnDtoToViewColumnDto)
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
    public TableDto createTable(DatabaseDto database, TableCreateDto data) throws SQLException,
            TableMalformedException, TableExistsException {
        final String tableName = mariaDbMapper.nameToInternalName(data.getName());
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create table if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.tableCreateDtoToCreateTableRawQuery(data))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
        final TableDto table = metadataMapper.databaseDtoToTableDto(database);
        table.setInternalName(tableName);
        return table;
    }

    @Override
    public Boolean existsView(DatabaseDto database, String viewName) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final Boolean queryResult;
        try {
            /* find view data */
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.selectExistsTableOrViewRawQuery());
            statement.setString(1, database.getInternalName());
            statement.setString(2, viewName);
            final ResultSet resultSet = statement.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            queryResult = mariaDbMapper.resultSetToBoolean(resultSet);
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}", e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return queryResult;
    }

    @Override
    public ViewDto createView(DatabaseDto database, ViewCreateDto data) throws SQLException,
            ViewMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        ViewDto view = ViewDto.builder()
                .name(data.getName())
                .internalName(mariaDbMapper.nameToInternalName(data.getName()))
                .query(data.getQuery())
                .queryHash(Hashing.sha256()
                        .hashString(data.getQuery(), StandardCharsets.UTF_8)
                        .toString())
                .isPublic(database.getIsPublic())
                .owner(database.getOwner())
                .identifiers(new LinkedList<>())
                .isInitialView(false)
                .vdbid(database.getId())
                .columns(new LinkedList<>())
                .build();
        try {
            /* create view if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.viewCreateRawQuery(view.getInternalName(), data.getQuery()))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            /* select view columns */
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, view.getInternalName());
            final ResultSet resultSet2 = statement2.executeQuery();
            while (resultSet2.next()) {
                view = dataMapper.resultSetToTable(resultSet2, view, queryConfig);
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
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
            final PreparedStatement statement1 = connection.prepareStatement(mariaDbMapper.databaseTableSelectRawQuery());
            statement1.setString(1, database.getInternalName());
            statement1.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            TableDto table = dataMapper.schemaResultSetToTable(database, statement1.executeQuery());
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            /* obtain columns metadata */
            start = System.currentTimeMillis();
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, tableName);
            log.trace("1={}, 2={}", database.getInternalName(), tableName);
            final ResultSet resultSet2 = statement2.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            while (resultSet4.next()) {
                table = dataMapper.resultSetToConstraint(resultSet4, table);
                for (UniqueDto uk : table.getConstraints().getUniques()) {
                    uk.setTable(metadataMapper.tableDtoToTableBriefDto(table));
                    final TableDto tmpTable = table;
                    uk.getColumns()
                            .forEach(column -> {
                                column.setTableId(tmpTable.getId());
                                column.setDatabaseId(database.getId());
                            });
                }
            }
            table.setTdbid(database.getId());
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
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
}
