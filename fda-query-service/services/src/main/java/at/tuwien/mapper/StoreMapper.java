package at.tuwien.mapper;

import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.exception.DatabaseConnectionException;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.querystore.Query;
import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.Mapper;

import java.sql.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
        return DigestUtils.sha256Hex(data.getResult().toString());
    }

    default PreparedStatement queryStoreRawInsertQuery(Connection connection, Query data) throws QueryStoreException {
        final String statement = "INSERT INTO `qs_queries` (`cid`, `dbid`, `query`, `query_normalized`, `query_hash`, `result_number`, `result_hash`, `execution`, `created`, `created_by`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING `id`";
        try {
            final PreparedStatement ps = connection.prepareStatement(statement);
            ps.setLong(1, data.getContainerId());
            ps.setLong(2, data.getDatabaseId());
            ps.setString(3, data.getQuery());
            ps.setString(4, data.getQueryNormalized());
            ps.setString(5, data.getQueryHash());
            if (data.getResultNumber() == null) {
                ps.setNull(6, Types.NULL);
            } else {
                ps.setLong(6, data.getResultNumber());
            }
            if (data.getResultHash() == null) {
                ps.setNull(7, Types.NULL);
            } else {
                ps.setString(7, data.getResultHash());
            }
            ps.setTimestamp(8, Timestamp.from(data.getExecution()));
            ps.setTimestamp(9, Timestamp.from(Instant.now()));
            ps.setLong(10, data.getCreatedBy());
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryStoreRawSelectAllQuery(Connection connection) throws QueryStoreException {
        final String statement = "SELECT `id`, `cid`, `created`, `created_by`, `dbid`, `execution`, `last_modified`, `query`, `query_hash`, `result_hash`, `result_number` FROM `qs_queries`";
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryStoreRawSelectOneQuery(Connection connection, Long containerId, Long databaseId, Long queryId) throws QueryStoreException {
        final String statement = "SELECT `id`, `cid`, `created`, `created_by`, `dbid`, `execution`, `last_modified`, `query`, `query_hash`, `result_hash`, `result_number` FROM `qs_queries` q WHERE q.`cid` = ? AND q.`dbid` = ? AND q.`id` = ?";
        try {
            final PreparedStatement ps = connection.prepareStatement(statement);
            ps.setLong(1, containerId);
            ps.setLong(2, databaseId);
            ps.setLong(3, queryId);
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryStoreRawUpdateQuery(Connection connection, Query data) throws QueryStoreException {
        final String statement = "UPDATE `qs_queries` SET `execution` = ?, `last_modified` = ?, `query` = ?, `query_hash` = ?, `result_hash` = ?, `result_number` = ?, `query_normalized` = ? WHERE `cid` = ? AND `dbid` = ? AND `id` = ?";
        try {
            final PreparedStatement ps = connection.prepareStatement(statement);
            ps.setTimestamp(1, Timestamp.from(data.getExecution()));
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, data.getQuery());
            ps.setString(4, data.getQueryHash());
            ps.setString(5, data.getResultHash());
            ps.setLong(6, data.getResultNumber());
            ps.setString(7, data.getQueryNormalized());
            /* where */
            ps.setLong(8, data.getContainerId());
            ps.setLong(9, data.getDatabaseId());
            ps.setLong(10, data.getId());
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

    default List<Query> resultSetToQueryList(ResultSet data) throws TableMalformedException {
        final List<Query> list = new LinkedList<>();
        try {
            while (data.next()) {
                list.add(resultSetToQuery(data));
            }
        } catch (SQLException e) {
            log.error("Failed to map queries");
            throw new TableMalformedException("Failed to map queries", e);
        }
        return list;
    }

    default Query resultSetToQuery(ResultSet data) throws SQLException {
        return resultSetToQuery(data, false);
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

    /**
     * Maps a result set row to an entity.
     *
     * @param data The result set row.
     * @return The query.
     * @throws SQLException The mapping does not exist
     */
    default Query resultSetToQuery(ResultSet data, Boolean next) throws SQLException {
        if (next && !data.next()) {
            throw new SQLException("Tuple does not exist");
        }
        return Query.builder()
                .id(data.getLong(1))
                .containerId(data.getLong(2))
                .created(data.getTimestamp(3)
                        .toInstant())
                .createdBy(data.getLong(4))
                .databaseId(data.getLong(5))
                .execution(data.getTimestamp(6)
                        .toInstant())
                .lastModified(data.getTimestamp(7) != null ? data.getTimestamp(7)
                        .toInstant() : null)
                .query(data.getString(8))
                .queryNormalized(data.getString(8))
                .queryHash(data.getString(9))
                .resultHash(data.getString(10) != null ? data.getString(10) : null)
                .resultNumber(data.getLong(11))
                .build();
    }

}
