package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.service.QueueService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class QueueServiceRabbitMqImpl extends DataConnector implements QueueService {

    private final DataMapper dataMapper;

    @Autowired
    public QueueServiceRabbitMqImpl(DataMapper dataMapper) {
        this.dataMapper = dataMapper;
    }

    @Override
    public void insert(DatabaseDto database, TableDto table, Map<String, Object> data) throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final int[] idx = new int[]{1};
            final PreparedStatement preparedStatement = connection.prepareStatement(
                    dataMapper.rabbitMqTupleToInsertOrUpdateQuery(database.getInternalName(),
                            dataMapper.tableDtoToTableDto(table), data));
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                final Optional<ColumnDto> optional = table.getColumns().stream().filter(c -> c.getInternalName().equals(entry.getKey())).findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find column with name {} in table with name {}, available columns are {}", entry.getKey(), table.getInternalName(), table.getColumns().stream().map(ColumnDto::getInternalName).toList());
                    continue;
                }
                dataMapper.prepareStatementWithColumnTypeObject(preparedStatement, optional.get().getColumnType(), idx[0]++,
                        entry.getValue());
            }
            final long start = System.currentTimeMillis();
            preparedStatement.executeUpdate();
            log.atDebug()
                    .setMessage("successfully inserted tuple")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "insert_tuple")
                    .log();
        } finally {
            dataSource.close();
        }
    }

    @Override
    public TupleWithTimestampsDto insertWithTimestamps(DatabaseDto database, TableDto table, Map<String, Object> data) throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        Map<String, Object> createdTupleWithTimestamps = null;
        try {
            // Insert (or upsert) the tuple
            final int[] idx = new int[]{1};
            final PreparedStatement insert = connection.prepareStatement(
                    dataMapper.rabbitMqTupleToInsertOrUpdateQuery(
                            database.getInternalName(),
                            dataMapper.tableDtoToTableDto(table),
                            data));
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                final Optional<ColumnDto> optional = table.getColumns().stream().filter(c -> c.getInternalName().equals(entry.getKey())).findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find column with name {} in table with name {}, available columns are {}", entry.getKey(), table.getInternalName(), table.getColumns().stream().map(ColumnDto::getInternalName).toList());
                    continue;
                }
                dataMapper.prepareStatementWithColumnTypeObject(insert, optional.get().getColumnType(), idx[0]++, entry.getValue());
            }
            final long start = System.currentTimeMillis();
            insert.executeUpdate();
            log.atDebug()
                    .setMessage("insert (with ts) tuple")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "insert_tuple_with_ts")
                    .log();

            // Build SELECT FOR SYSTEM_TIME AS OF NOW() by primary key to read resulting timestamps
            final java.util.List<String> primaryKeyColumns = table.getConstraints() != null && table.getConstraints().getPrimaryKey() != null
                    ? table.getConstraints().getPrimaryKey().stream().map(pk -> pk.getColumn().getInternalName()).toList()
                    : java.util.Collections.emptyList();

            if (primaryKeyColumns.isEmpty()) {
                // Fallback: try to read by unique id if present in payload
                log.warn("Primary key metadata missing for table {}.{}; timestamps will not be returned", database.getInternalName(), table.getInternalName());
                connection.commit();
                return null;
            }

            final StringBuilder select = new StringBuilder("SELECT ");
            final int[] colIdx = new int[]{0};
            for (ColumnDto c : table.getColumns()) {
                select.append(colIdx[0]++ == 0 ? "" : ", ")
                        .append("`")
                        .append(c.getInternalName())
                        .append("`");
            }
            select.append(", `replication_key`");
            select.append(", ROW_START AS inserted_at, ROW_END AS deleted_at FROM `")
                    .append(database.getInternalName())
                    .append("`.`")
                    .append(table.getInternalName())
                    .append("` FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(DataMapper.mariaDbFormatter.format(java.time.Instant.now()))
                    .append("' WHERE ");
            final int[] whereIdx = new int[]{0};
            for (String col : primaryKeyColumns) {
                select.append(whereIdx[0]++ == 0 ? "" : " AND ")
                        .append("`").append(col).append("` = ?");
            }
            select.append(" LIMIT 1;");

            final PreparedStatement selectStmt = connection.prepareStatement(select.toString());
            int bind = 1;
            for (String col : primaryKeyColumns) {
                final Object value = data.get(col);
                final Optional<ColumnDto> optional = table.getColumns().stream().filter(c -> c.getInternalName().equals(col)).findFirst();
                if (optional.isEmpty()) {
                    continue;
                }
                dataMapper.prepareStatementWithColumnTypeObject(selectStmt, optional.get().getColumnType(), bind++, col, value);
            }
            final ResultSet rs = selectStmt.executeQuery();
            if (rs.next()) {
                createdTupleWithTimestamps = new java.util.HashMap<>();
                for (ColumnDto c : table.getColumns()) {
                    createdTupleWithTimestamps.put(c.getInternalName(), rs.getObject(c.getInternalName()));
                }
                putIfColumnExists(rs, createdTupleWithTimestamps, "replication_key");
                putIfColumnExists(rs, createdTupleWithTimestamps, "inserted_at");
                putIfColumnExists(rs, createdTupleWithTimestamps, "deleted_at");
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            dataSource.close();
        }

        if (createdTupleWithTimestamps == null) {
            return null;
        }

        // Normalize timestamp fields
        formatTimestampsWithMicrosecondPrecision(createdTupleWithTimestamps);

        return TupleWithTimestampsDto.builder()
                .data(createdTupleWithTimestamps)
                .insertedAt(parseTimestampToInstant(createdTupleWithTimestamps.get("inserted_at")))
                .deletedAt(parseTimestampToInstant(createdTupleWithTimestamps.get("deleted_at")))
                .replicationKey(createdTupleWithTimestamps.get("replication_key") != null ? createdTupleWithTimestamps.get("replication_key").toString() : null)
                .build();
    }

    private void putIfColumnExists(ResultSet rs, Map<String, Object> map, String column) {
        try {
            Object v = rs.getObject(column);
            map.put(column, v);
        } catch (Exception ignored) {
        }
    }

    private void formatTimestampsWithMicrosecondPrecision(Map<String, Object> tuple) {
        if (tuple == null) return;
        if (tuple.get("inserted_at") instanceof java.sql.Timestamp ts) {
            java.time.LocalDateTime ldt = ts.toLocalDateTime();
            String formatted = DataMapper.mariaDbFormatter.format(ldt) + "+00:00";
            tuple.put("inserted_at", formatted);
        }
        if (tuple.get("deleted_at") instanceof java.sql.Timestamp ts) {
            java.time.LocalDateTime ldt = ts.toLocalDateTime();
            String formatted = DataMapper.mariaDbFormatter.format(ldt) + "+00:00";
            tuple.put("deleted_at", formatted);
        }
    }

    private java.time.Instant parseTimestampToInstant(Object timestamp) {
        if (timestamp == null) return null;
        if (timestamp instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) timestamp).toInstant();
        } else if (timestamp instanceof java.time.Instant) {
            return (java.time.Instant) timestamp;
        } else if (timestamp instanceof String s) {
            try {
                if (s.contains("T") && s.endsWith("Z")) {
                    return java.time.Instant.parse(s);
                } else if (s.contains("+") || (s.contains("-") && s.lastIndexOf('-') > 10)) {
                    String withoutTz = s.substring(0, s.length() - 6);
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(withoutTz,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                    return ldt.atZone(java.time.ZoneOffset.UTC).toInstant();
                } else {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
                    return ldt.atZone(java.time.ZoneOffset.UTC).toInstant();
                }
            } catch (Exception e) {
                log.debug("failed to parse timestamp string: {}", s);
                return null;
            }
        }
        return null;
    }

}
