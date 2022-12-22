package at.tuwien.mapper;

import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.querystore.Query;
import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.Mapper;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreMapper.class);

    default Long queryResultDtoToLong(QueryResultDto data) {
        if (data == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(data.getResult().size()));
    }

    default String queryResultDtoToString(QueryResultDto data) {
        if (data == null) {
            return null;
        }
        final String hash = DigestUtils.sha256Hex(data.getResult().toString());
        log.trace("mapped query result {} to hash {}", data, hash);
        return hash;
    }

    default PreparedStatement queryStoreRawInsertQuery(Connection connection, User user, Query data)
            throws QueryStoreException {
        final String statement = "CALL _store_query(?, ?, @queryId)";
        try {
            final PreparedStatement ps = connection.prepareStatement(statement);
            ps.setString(1, user.getUsername());
            ps.setString(2, data.getQuery());
            log.trace("mapped insert query {} to prepared statement {}", statement, ps);
            return ps;
        } catch (SQLException e) {
            log.error("failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryStoreRawSelectAllQuery(Connection connection, Boolean persisted) throws QueryStoreException {
        String statement = "SELECT `id`, `created`, `created_by`, `last_modified`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted` FROM `qs_queries`";
        if (persisted != null) {
            statement += " WHERE `is_persisted` = " + persisted;
        }
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement);
            log.trace("mapped select all query {} to prepared statement {}", statement, pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryStoreRawSelectOneQuery(Connection connection, Long queryId) throws QueryStoreException {
        final String statement = "SELECT `id`, `created`, `created_by`, `last_modified`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted` FROM `qs_queries` q WHERE q.`id` = ?";
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement);
            pstmt.setLong(1, queryId);
            log.trace("mapped select one query {} to prepared statement {}", statement, pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
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
