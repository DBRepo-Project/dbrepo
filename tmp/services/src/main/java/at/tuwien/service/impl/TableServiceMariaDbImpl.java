package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.StorageService;
import at.tuwien.service.TableService;
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

    private final MariaDbMapper mariaDbMapper;
    private final StorageService storageService;
    private final DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @Autowired
    public TableServiceMariaDbImpl(MariaDbMapper mariaDbMapper, StorageService storageService,
                                   DataDatabaseSidecarGateway dataDatabaseSidecarGateway) {
        this.mariaDbMapper = mariaDbMapper;
        this.storageService = storageService;
        this.dataDatabaseSidecarGateway = dataDatabaseSidecarGateway;
    }

    @Override
    public void createTable(PrivilegedDatabaseDto database, TableCreateDto data) throws SQLException,
            TableMalformedException, TableExistsException {
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
            log.error("Failed to delete table and history view: {}", e.getMessage());
            throw new QueryMalformedException("Failed to delete table and history view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted table and history view with name {}", tableName);
    }

    @Override
    public QueryResultDto getData(PrivilegedTableDto table, Instant timestamp, Long page, Long size) throws SQLException,
            TableMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final QueryResultDto queryResult;
        try {
            /* find table data */
            final ResultSet resultSet = connection.prepareStatement(
                            mariaDbMapper.selectDatasetRawQuery(table.getDatabase().getInternalName(), table.getInternalName(),
                                    table.getColumns(), timestamp, size, page))
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
        return queryResult;
    }

    @Override
    public List<TableHistoryDto> history(PrivilegedTableDto table) throws SQLException,
            TableNotFoundException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        final List<TableHistoryDto> history;
        try {
            /* find table data */
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectHistoryRawQuery(
                            table.getDatabase().getInternalName(), table.getInternalName(), 100L))
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
    public void importTuple(PrivilegedTableDto table, TupleDto data)
            throws TableMalformedException, StorageUnavailableException, StorageNotFoundException, SQLException, QueryMalformedException {
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
            data.getData().replace(key, blob);
        }
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawInsertQuery(table, data));
            for (int i = 0; i < table.getColumns().size(); i++) {
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement, table.getColumns().get(i).getColumnType(),
                        i, data.getData().get(table.getColumns().get(i).getInternalName()));
            }
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to import tuple: {}", e.getMessage());
            throw new QueryMalformedException("Failed to import tuple: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Imported tuple into table: {}.{}", table.getDatabase().getInternalName(), table.getInternalName());
    }

    @Override
    public void importDataset(PrivilegedTableDto table, ImportCsvDto data)
            throws SidecarImportException, StorageNotFoundException, SQLException, QueryMalformedException {
        /* import .csv from blob storage to sidecar */
        dataDatabaseSidecarGateway.importFile(table.getDatabase().getContainer().getSidecarHost(), table.getDatabase().getContainer().getSidecarPort(), data.getLocation());
        /* import .csv from sidecar to database */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* import tuple */
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
            for (String column : table.getConstraints().getPrimaryKey()) {
                final Optional<ColumnDto> optional = table.getColumns()
                        .stream()
                        .filter(c -> c.getInternalName().equals(column))
                        .findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find table column {}", column);
                    throw new IllegalArgumentException("Failed to find table column");
                }
                if (data.getKeys().get(column) == null) {
                    statement.setNull(idx[0]++, Types.NULL);
                } else if (data.getKeys().get(column).equals(true) || data.getKeys().get(column).equals(false)) {
                    statement.setBoolean(idx[0]++, Boolean.parseBoolean(String.valueOf(data.getKeys().get(column))));
                } else {
                    mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
                            table.getColumns().get(idx[0]).getColumnType(), idx[0], data.getKeys().get(column));
                    idx[0]++;
                }
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
            QueryMalformedException, TableMalformedException {
        log.trace("create tuple: {}", data);
        /* prepare the statement */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* create tuple */
            connection.prepareStatement(mariaDbMapper.tupleToRawCreateQuery(table, data))
                    .executeUpdate();
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
            /* import tuple */
            final int[] idx = new int[]{1};
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.tupleToRawUpdateQuery(table, data));
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                final Optional<ColumnDto> optional = table.getColumns().stream()
                        .filter(c -> c.getInternalName().equals(entry.getKey())).findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find column with name {}", entry.getKey());
                    throw new QueryMalformedException("Failed to find column with name {}" + entry.getKey());
                }
                mariaDbMapper.prepareStatementWithColumnTypeObject(statement,
                        optional.get().getColumnType(), idx[0], entry.getValue());
                statement.executeUpdate();
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

    @Override
    public ExportResourceDto exportDataset(PrivilegedTableDto table, Instant timestamp)
            throws SQLException, SidecarExportException, StorageNotFoundException, StorageUnavailableException,
            QueryMalformedException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(table.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* export to data database sidecar */
            connection.prepareStatement(mariaDbMapper.tableOrViewToRawExportQuery(table.getDatabase().getInternalName(),
                            table.getInternalName(), table.getColumns(), timestamp, filename))
                    .executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        dataDatabaseSidecarGateway.exportFile(table.getDatabase().getContainer().getSidecarHost(), table.getDatabase().getContainer().getSidecarPort(), filename);
        return storageService.getResource(filename);
    }

}
