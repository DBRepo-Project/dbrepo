package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.api.Result;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.PostgresMapper;
import at.ac.tuwien.ifs.dbrepo.service.DataService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;


@Slf4j
@Service
public class DataServicePostgresImpl extends DataConnector implements DataService {

    private final PostgresMapper postgresMapper;

    @Autowired
    public DataServicePostgresImpl(PostgresMapper postgresMapper) {
        this.postgresMapper = postgresMapper;
    }

    @Override
    public Result getTableData(Database database, String tableName, Instant timestamp, Long page, Long size)
            throws SQLException, DatabaseMalformedException {
        final Optional<Table> optional = database.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(tableName))
                .findFirst();
        if (optional.isEmpty()) {
            /* should never happen */
            throw new SQLException("Failed to find table: " + tableName);
        }
        final List<String> headers = optional.get()
                .getColumns()
                .stream()
                .map(Column::getInternalName)
                .toList();
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            long start = System.currentTimeMillis();
            final PreparedStatement statement;
            statement = connection.prepareStatement(postgresMapper.defaultRawTableSelectQuery(tableName, timestamp, page, size));
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
                for (String header : headers) {
                    row.put(header, resultSet.getString(idx[0]++));
                }
                data.add(row);
            }
            return Result.builder()
                    .headers(headers)
                    .data(data)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to get data: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get data: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Result getViewData(Database database, String viewName, Instant timestamp, Long page, Long size)
            throws SQLException, DatabaseMalformedException {
        final Optional<View> optional = database.getViews()
                .stream()
                .filter(v -> v.getInternalName().equals(viewName))
                .findFirst();
        if (optional.isEmpty()) {
            /* should never happen */
            throw new SQLException("Failed to find view: " + viewName);
        }
        final List<String> headers = optional.get()
                .getColumns()
                .stream()
                .map(ViewColumn::getInternalName)
                .toList();
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        final String target = DigestUtils.sha3_224Hex(viewName);
        try {
            long start = System.currentTimeMillis();
            final PreparedStatement statement1;
            /* create temporary table */
            statement1 = connection.prepareStatement(postgresMapper.defaultCreateTemporaryTableFromSourceQuery(target, viewName));
            statement1.execute();
            long duration = System.currentTimeMillis() - start;
            log.atDebug()
                    .setMessage("executed query statement in " + duration + "ms")
                    .addKeyValue(Constants.DURATION, duration)
                    .addKeyValue(Constants.ACTION, "create_temporary_table")
                    .log();
            /* select from temporary table as of time */
            final PreparedStatement statement2;
            statement2 = connection.prepareStatement(postgresMapper.defaultRawTableSelectQuery(target, timestamp, page, size));
            final ResultSet resultSet = statement2.executeQuery();
            duration = System.currentTimeMillis() - start;
            log.atDebug()
                    .setMessage("executed query statement in " + duration + "ms")
                    .addKeyValue(Constants.DURATION, duration)
                    .addKeyValue(Constants.ACTION, "execute")
                    .log();
            final List<Map<String, Object>> data = new ArrayList<>();
            while (resultSet.next()) {
                final int[] idx = new int[]{1};
                final Map<String, Object> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, resultSet.getString(idx[0]++));
                }
                data.add(row);
            }
            return Result.builder()
                    .headers(headers)
                    .data(data)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to get data: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get data: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

}
