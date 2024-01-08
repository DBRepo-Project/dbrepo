package at.tuwien.mapper;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.querystore.Query;
import org.mapstruct.Mapper;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreMapper.class);

    DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
            .withZone(ZoneId.of("UTC"));

    default CallableStatement queryStoreRawInsertQuery(Connection connection, User user, ExecuteStatementDto data)
            throws QueryStoreException {
        final String statement = "{call _store_query(?, ?, ?, ?)}";
        log.trace("statement={}", statement);
        /* timestamp */
        if (data.getTimestamp() == null) {
            data.setTimestamp(Instant.now());
            log.trace("timestamp is null: set timestamp to {}", data.getTimestamp());
        }
        try {
            final CallableStatement ps = connection.prepareCall(statement);
            ps.setString(1, user.getUsername());
            log.trace("param 1={}", user.getUsername());
            ps.setString(2, data.getStatement());
            log.trace("param 2={}", data.getStatement());
            ps.setTimestamp(3, Timestamp.from(data.getTimestamp()));
            log.trace("param 3={}", Timestamp.from(data.getTimestamp()));
            ps.registerOutParameter(4, Types.BIGINT);
            return ps;
        } catch (SQLException e) {
            log.error("failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement '" + statement + "'", e);
        }
    }

    default PreparedStatement queryStoreRawSelectAllQuery(Connection connection, Boolean persisted) throws QueryStoreException {
        String statement = "SELECT `id`, `created`, `created_by`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted` FROM `qs_queries`";
        if (persisted != null) {
            statement += " WHERE `is_persisted` = ?";
        }
        statement += " ORDER BY `created` DESC";
        try {
            log.trace("mapped select all query '{}' to prepared statement", statement);
            final PreparedStatement preparedStatement = connection.prepareStatement(statement);
            if (persisted != null) {
                preparedStatement.setBoolean(1, persisted);
            }
            return preparedStatement;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryStoreRawDeleteStaleQueries(Connection connection) throws QueryStoreException {
        final String statement = "DELETE FROM `qs_queries` WHERE `is_persisted` = false AND ABS(DATEDIFF(`created`, NOW())) >= 1";
        try {
            log.trace("mapped select all query '{}' to prepared statement", statement);
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryStoreRawSelectOneQuery(Connection connection, Long queryId) throws QueryStoreException {
        final String statement = "SELECT `id`, `created`, `created_by`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted` FROM `qs_queries` q WHERE q.`id` = ?";
        try {
            log.trace("mapped select one query '{}' to prepared statement", statement);
            final PreparedStatement pstmt = connection.prepareStatement(statement);
            log.trace("queryId={}", queryId);
            pstmt.setLong(1, queryId);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {},   reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default Query resultSetToQuery(ResultSet data) throws SQLException {
        final String created = data.getString(2);
        final Instant createdInst = LocalDateTime.parse(created, mariaDbFormatter)
                .atZone(ZoneId.of("UTC"))
                .toInstant();
        log.trace("query created {} parsed as Instant {}", created, createdInst);
        return Query.builder()
                .id(data.getLong(1))
                .created(createdInst)
                .createdBy(data.getString(3))
                .query(data.getString(4))
                .queryHash(data.getString(5))
                .resultHash(data.getString(6))
                .resultNumber(data.getLong(7))
                .isPersisted(data.getBoolean(8))
                .build();
    }

    default PreparedStatement queryStoreRawPersistQuery(Connection connection, Boolean persisted, Long queryId) throws QueryStoreException {
        final String statement = "UPDATE `qs_queries` SET `is_persisted` = ? WHERE `id` = ?";
        try {
            final PreparedStatement ps = connection.prepareStatement(statement);
            ps.setBoolean(1, persisted);
            /* where */
            ps.setLong(2, queryId);
            log.trace("mapped persist query {} to prepared statement {}", statement, ps);
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default Long resultSetToId(ResultSet data) throws TableMalformedException, QueryStoreException {
        try {
            if (!data.next()) {
                log.error("Failed to map id");
                throw new TableMalformedException("Failed to map id");
            }
            return data.getLong(1);
        } catch (SQLException e) {
            log.error("Failed to retrieve id");
            throw new QueryStoreException("Failed to retrieve id");
        }
    }

}
