package at.tuwien.mapper;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.query.*;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.querystore.Query;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.ImageNotSupportedException;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mariadb.jdbc.MariaDbBlob;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.sql.*;
import java.sql.Date;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface QueryMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryMapper.class);

    DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    @Deprecated
    @Mappings({
            @Mapping(source = "query", target = "statement")
    })
    ExecuteStatementDto queryDtoToExecuteStatementDto(QueryDto data);

    ExecuteStatementDto saveStatementDtoToExecuteStatementDto(SaveStatementDto data);

    QueryDto queryToQueryDto(Query data);

    QueryBriefDto queryToQueryBriefDto(Query data);

    @Named("internalMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.length() == 0) {
            return data;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(data).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    default QueryResultDto resultListToQueryResultDto(List<TableColumn> columns, ResultSet result) throws SQLException {
        final List<Map<String, Object>> resultList = new LinkedList<>();
        log.trace("result has {} columns", columns.size());
        while (result.next()) {
            /* map the result set to the columns through the stored metadata in the metadata database */
            int[] idx = new int[]{1};
            final Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                map.put(columns.get(i).getInternalName(), dataColumnToObject(result.getObject(idx[0]++), columns.get(i)));
            }
            resultList.add(map);
        }
        return QueryResultDto.builder()
                .result(resultList)
                .build();
    }

    default PreparedStatement generateTemporaryTableSQL(Connection connection, Table table) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE TABLE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`")
                .append(" LIKE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("`;");
        log.trace("mapped raw generate temporary table query [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement viewCreateDtoToRawCreateViewQuery(Connection connection, ViewCreateDto data)
            throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE VIEW `")
                .append(nameToInternalName(data.getName()))
                .append("` AS (")
                .append(data.getQuery())
                .append(")");
        log.trace("mapped raw create view query [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement dropTemporaryTableSQL(Connection connection, Table table) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("DROP TABLE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`;");
        log.trace("mapped raw drop temporary table query [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }


    default PreparedStatement pathToRawInsertQuery(Connection connection, Table table, ImportDto data) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("LOAD DATA LOCAL INFILE '")
                .append(data.getLocation())
                .append("' INTO TABLE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary")
                .append("` CHARACTER SET utf8 FIELDS TERMINATED BY '")
                .append(data.getSeparator())
                .append("'");
        if (data.getQuote() != null) {
            statement.append(" OPTIONALLY ENCLOSED BY '")
                    .append(data.getQuote())
                    .append("'");
        }
        statement.append(data.getSkipLines() != null ? (" IGNORE " + data.getSkipLines() + " LINES") : "")
                .append(" (");
        final StringBuilder set = new StringBuilder();
        int[] idx = new int[]{0};
        table.getColumns()
                .forEach(column -> {
                    if (column.getAutoGenerated()) {
                        return;
                    }
                    statement.append(idx[0] != 0 ? "," : "");
                    /* format as variable */
                    statement.append("@")
                            .append(column.getInternalName());
                    if (column.getDateFormat() != null) {
                        /* reformat dates */
                        columnToDateSet(data, table, column, set);
                    } else if (column.getColumnType().equals(TableColumnType.BOOLEAN)) {
                        /* reformat booleans */
                        columnToBoolSet(data, table, column, set);
                    } else {
                        /* reformat others */
                        columnToTextSet(data, table, column, set);
                    }
                    idx[0]++;
                });
        statement.append(")")
                .append(set.length() != 0 ? (" SET " + set) : "")
                .append(";");
        log.debug("import csv {} for table {}", data.getLocation(), table);
        log.trace("raw import query: [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default void columnToBoolSet(ImportDto data, Table table, TableColumn column, StringBuilder set) {
        set.append(set.length() != 0 ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = ");
        if (data.getNullElement() != null) {
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getNullElement())
                    .append("'),NULL,");
            columnToBoolSet2(data, table, column, set);
            set.append(")");
            return;
        }
        columnToBoolSet2(data, table, column, set);
    }

    default void columnToBoolSet2(ImportDto data, Table table, TableColumn column, StringBuilder set) {
        if (data.getTrueElement() != null) {
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getTrueElement())
                    .append("'),TRUE,");
            if (data.getFalseElement() != null) {
                /* can map both true/false */
                set.append("IF(!STRCMP(@")
                        .append(column.getInternalName())
                        .append(",'")
                        .append(data.getFalseElement())
                        .append("'),FALSE,@")
                        .append(column.getInternalName())
                        .append("))");
            } else {
                /* can only map true */
                set.append("@")
                        .append(column.getInternalName())
                        .append(")");
            }
            return;
        }
        if (data.getFalseElement() != null) {
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getFalseElement())
                    .append("'),FALSE,");
            if (data.getTrueElement() != null) {
                /* can map both true/false */
                set.append("IF(!STRCMP(@")
                        .append(column.getInternalName())
                        .append(",'")
                        .append(data.getTrueElement())
                        .append("'),TRUE,@")
                        .append(column.getInternalName())
                        .append("))");
            } else {
                /* can only map true */
                set.append("@")
                        .append(column.getInternalName())
                        .append(")");
            }
            return;
        }
        set.append("@")
                .append(column.getInternalName());
    }

    default void columnToTextSet(ImportDto data, Table table, TableColumn column, StringBuilder set) {
        set.append(set.length() != 0 ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = ");
        if (data.getNullElement() != null) {
            set.append("IF(STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getNullElement())
                    .append("'), @")
                    .append(column.getInternalName())
                    .append(", NULL)");
            return;
        }
        set.append("@")
                .append(column.getInternalName());
    }

    default void columnToDateSet(ImportDto data, Table table, TableColumn column, StringBuilder set) {
        set.append(set.length() != 0 ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = STR_TO_DATE(");
        if (data.getNullElement() != null) {
            set.append("IF(STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getNullElement())
                    .append("'), @")
                    .append(column.getInternalName())
                    .append(", NULL), '")
                    .append(column.getDateFormat()
                            .getDatabaseFormat()
                            .replace('\'', '\\'))
                    .append("')");
            return;
        }
        set.append("@")
                .append(column.getInternalName())
                .append(", '")
                .append(column.getDateFormat()
                        .getDatabaseFormat()
                        .replace('\'', '\\'))
                .append("')");
    }

    default PreparedStatement tableToRawExportQuery(Connection connection, Table table, Instant timestamp, String filename) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("SELECT ");
        int[] idx = new int[]{0};
        table.getColumns()
                .forEach(column -> {
                    statement.append(idx[0] != 0 ? "," : "")
                            .append("`")
                            .append(column.getInternalName())
                            .append("`");
                    idx[0]++;
                });
        statement.append("FROM `")
                .append(table.getInternalName())
                .append("`");
        if (timestamp != null) {
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        statement.append(" INTO OUTFILE '/tmp/")
                .append(filename)
                .append("' CHARACTER SET utf8");
        statement.append(";");
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryToRawExportQuery(Connection connection, Query query, String filename) throws QueryMalformedException {
        if (query.getQuery().contains(";")) {
            log.trace("Remove ending ; from statement [{}]", query.getQuery());
            query.setQuery(query.getQuery().substring(0, query.getQuery().indexOf(";")));
        }
        /* insert the FOR SYSTEM_TIME ... part after the FROM in the query */
        final StringBuilder versionPart = new StringBuilder(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                .append(mariaDbFormatter.format(query.getExecution()))
                .append("' ");
        final Pattern pattern = Pattern.compile("from `?[a-zA-Z0-9_-]+`?", Pattern.CASE_INSENSITIVE) /* https://mariadb.com/kb/en/columnstore-naming-conventions/ */;
        final Matcher matcher = pattern.matcher(query.getQuery());
        if (!matcher.find()) {
            log.error("Failed to find 'from' clause in query");
            throw new QueryMalformedException("Failed to find from clause");
        }
        log.debug("found group from {} to {} in '{}'", matcher.start(), matcher.end(), query.getQuery());
        final StringBuilder statement = new StringBuilder(query.getQuery().substring(0, matcher.end(0)))
                .append(versionPart)
                .append(query.getQuery().substring(matcher.end(0)))
                .append(" INTO OUTFILE '/tmp/")
                .append(filename)
                .append("' CHARACTER SET utf8 FIELDS TERMINATED BY ',';");
        log.trace("raw export query: [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableCsvDtoToRawInsertQuery(Connection connection, Table table, TableCsvDto data)
            throws TableMalformedException, ImageNotSupportedException, QueryMalformedException {
        if (table.getColumns().size() == 0) {
            log.error("Column size is zero");
            throw new TableMalformedException("Columns are not known");
        }
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Image not supported.");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("INSERT INTO `")
                .append(table.getInternalName())
                .append("` (")
                .append(table.getColumns()
                        .stream()
                        .filter(column -> !column.getAutoGenerated())
                        .map(column -> "`" + column.getInternalName() + "`")
                        .collect(Collectors.joining(",")))
                .append(") VALUES (");
        final int[] idx = new int[]{1, 0};
        table.getColumns()
                .stream()
                .filter(c -> !c.getAutoGenerated())
                .forEach(c -> statement.append(idx[1]++ > 0 ? "," : "")
                        .append("?"));
        statement.append(");");
        /* map all columns that are non-auto generated */
        try {
            final PreparedStatement ps = connection.prepareStatement(statement.toString());
            for (int i = 0; i < table.getColumns().size(); i++) {
                final TableColumn column = table.getColumns()
                        .get(i);
                if (column.getAutoGenerated()) {
                    log.trace("column is auto-generated, skip.");
                    continue;
                }
                final Optional<Map.Entry<String, Object>> tuple = data.getData()
                        .entrySet()
                        .stream()
                        .filter(d -> d.getKey().equals(column.getInternalName()))
                        .findFirst();
                if (tuple.isEmpty()) {
                    log.error("Failed to map column names");
                    log.debug("failed to map column names, tuple contains columns names that are not present in the database, tuple column names are {}", data.getData().keySet());
                    throw new TableMalformedException("Failed to map column names");
                }
                prepareStatementWithColumnTypeObject(ps, column.getColumnType(), idx[0]++, tuple.get().getValue());
            }
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableCsvDtoToRawDeleteQuery(Connection connection, Table table, TableCsvDeleteDto data)
            throws TableMalformedException, ImageNotSupportedException, QueryMalformedException {
        int i = 1;
        if (table.getColumns().size() == 0) {
            log.error("Column size is zero");
            throw new TableMalformedException("Columns are not known");
        }
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Image not supported.");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("DELETE FROM `")
                .append(table.getInternalName())
                .append("` WHERE ");
        final int[] idx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> statement.append(idx[0]++ == 0 ? "" : ", ")
                        .append("`")
                        .append(key)
                        .append("` = ?"));
        /* debug */
        log.trace("raw delete query: [{}] with data {}", statement, data.getKeys().values());
        /* prepare */
        try {
            final PreparedStatement ps = connection.prepareStatement(statement.toString());
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                final Optional<TableColumn> optional = table.getColumns()
                        .stream()
                        .filter(c -> c.getInternalName().equals(entry.getKey()))
                        .findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find column");
                    log.debug("failed to find column with internal name {} in table {}", entry.getKey(), table);
                    throw new QueryMalformedException("Failed to find column");
                }
                prepareStatementWithColumnTypeObject(ps, optional.get().getColumnType(), i++, entry.getValue());
            }
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableCsvDtoToRawUpdateQuery(Connection connection, Table table, TableCsvUpdateDto data)
            throws TableMalformedException, ImageNotSupportedException, QueryMalformedException {
        int i = 1;
        if (table.getColumns().size() == 0) {
            log.error("Column size is zero");
            throw new TableMalformedException("Columns are not known");
        }
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Image not supported.");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("UPDATE `")
                .append(table.getInternalName())
                .append("` SET ");
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    statement.append(idx[0]++ == 0 ? "" : ", ")
                            .append("`")
                            .append(key)
                            .append("` = ?");
                });
        statement.append(" WHERE ");
        final int[] jdx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> {
                    statement.append(jdx[0] == 0 ? "" : ", ")
                            .append("`")
                            .append(key)
                            .append("` = '")
                            .append(value)
                            .append("'");
                    jdx[0]++;
                });
        statement.append(";");
        /* debug */
        log.trace("raw update query: [{}] with data {}", statement, data.getData().values());
        try {
            final PreparedStatement ps = connection.prepareStatement(statement.toString());
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                ps.setString(i++, String.valueOf(entry.getValue()));
            }
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableToRawCountAllQuery(Connection connection, Table table, Instant timestamp) throws ImageNotSupportedException, QueryMalformedException {
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Image not supported.");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        final StringBuilder statement = new StringBuilder("SELECT COUNT(*) FROM `")
                .append(nameToInternalName(table.getName()))
                .append("` FOR SYSTEM_TIME AS OF TIMESTAMP '")
                .append(LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC")))
                .append("';");
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryToRawTimestampedCountQuery(Connection connection, String query, Database database, Instant timestamp)
            throws ImageNotSupportedException, QueryMalformedException {
        /* param check */
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must be provided");
        }
        query = query.toLowerCase(Locale.ROOT)
                .split("from")[1];
        final StringBuilder original = new StringBuilder();
        original.append("SELECT COUNT(*) AS `total` FROM");
        if (!query.contains("where")) {
            /* treat join queries as normal queries */
            original.append(query);
        } else {
            original.append(query.split("where")[0]);
        }
        if (query.contains("join")) {
            /* put timestamp after "join" and each "on" (but before alias) */
        } else {
            original.append("FOR SYSTEM_TIME AS OF TIMESTAMP '");
            original.append(LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC")));
            original.append("' ");
        }
        if (query.contains("where")) {
            original.append("where");
            original.append(query.split("where")[1]);
        }
        original.append(";");
        /* replace timestamp for join query */
        String statement = original.toString();
        if (query.contains("join")) {
            statement = statement.replaceFirst("from ([`a-z0-9_]+) ", "from $1 FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC"))
                    + "' ");
            statement = statement.replaceAll("join ([`a-z0-9_]+) ", "join $1 FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC"))
                    + "' ");
        }
        log.debug("mapped raw view-only query [{}]", statement);
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryToRawTimestampedQuery(Connection connection,
                                                         String query, Database database, Instant timestamp, Long page,
                                                         Long size) throws ImageNotSupportedException,
            QueryMalformedException {
        /* param check */
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Please provide a timestamp before");
        }
        query = query.toLowerCase(Locale.ROOT)
                .trim();
        if (query.matches(";$")) {
            /* remove last semicolon */
            query = query.substring(0, query.length() - 1);
        }
        /* query check (this is enforced by the db also) */
        final String query_ = query;
        if (Stream.of("delete", "update", "truncate", "create", "drop").anyMatch(query_::startsWith)) {
            log.error("Query attempts to modify the database");
            log.debug("query attempts to modify the database [{}]", query_);
            throw new QueryMalformedException("Query attempts to modify the database");
        }
        final StringBuilder sb = new StringBuilder();
        if (!query.contains("where")) {
            /* treat join queries as normal queries */
            sb.append(query);
        } else {
            sb.append(query.split("where")[0]);
        }
        if (query.contains("join")) {
            /* put timestamp after "join" and each "on" (but before alias) */
        } else {
            sb.append(" FOR SYSTEM_TIME AS OF TIMESTAMP '");
            sb.append(LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC")));
            sb.append("' ");
        }
        if (query.contains("where")) {
            sb.append("where");
            sb.append(query.split("where")[1]);
        }
        if (size != null && page != null && size > 0 && page >= 0) {
            sb.append(" LIMIT " + size + " OFFSET " + (page * size));
        }
        sb.append(";");
        /* replace timestamp for join query */
        String statement = sb.toString();
        if (query.contains("join")) {
            statement = statement.replaceFirst("from ([`a-z0-9_]+) ", "from $1 FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC"))
                    + "' ");
            statement = statement.replaceAll("join ([`a-z0-9_]+) ", "join $1 FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC"))
                    + "' ");
        }
        log.debug("mapped raw view-only query [{}]", statement);
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableToRawFindAllQuery(Connection connection, Table table, Instant timestamp, Long size, Long page)
            throws ImageNotSupportedException, QueryMalformedException {
        /* param check */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
            log.debug("no timestamp provided, default to {}", timestamp);
        } else {
            log.debug("timestamp provided {}", timestamp);
        }
        final int[] idx = new int[]{0};
        final StringBuilder statement = new StringBuilder("SELECT ");
        table.getColumns()
                .forEach(column -> statement.append(idx[0]++ > 0 ? "," : "")
                        .append("`")
                        .append(column.getInternalName())
                        .append("`"));
        statement.append(" FROM `")
                .append(nameToInternalName(table.getName()))
                .append("` FOR SYSTEM_TIME AS OF TIMESTAMP '")
                .append(LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC")))
                .append("'");
        if (size != null && page != null) {
            log.trace("pagination size/limit of {}", size);
            statement.append(" LIMIT ")
                    .append(size);
            log.trace("pagination page/offset of {}", page);
            statement.append(" OFFSET ")
                    .append(page * size)
                    .append(";");

        }
        log.trace("raw select table query: [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default QueryResultDto queryTableToQueryResultDto(ResultSet result, Table table) throws DateTimeException, SQLException {
        final List<Map<String, Object>> queryResult = new LinkedList<>();
        while (result.next()) {
            /* map the result set to the columns through the stored metadata in the metadata database */
            int[] idx = new int[]{1};
            final Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < table.getColumns().size(); i++) {
                map.put(table.getColumns().get(i).getInternalName(), dataColumnToObject(result.getObject(idx[0]++), table.getColumns().get(i)));
            }
            queryResult.add(map);
        }
        log.info("Selected {} records from table id {}", queryResult.size(), table.getId());
        log.trace("table {} contains {} records", table, queryResult.size());
        return QueryResultDto.builder()
                .result(queryResult)
                .build();
    }

    @Transactional(readOnly = true)
    default PreparedStatement historyRawQuery(Connection connection, Table data) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("SELECT")
                .append(" IF(`deleted_at` IS NULL, `inserted_at`, `deleted_at`) as `timestamp`")
                .append(", IF(`deleted_at` IS NULL, 'INSERT', 'DELETE') as `event`")
                .append(", COUNT(`inserted_at`) as `total` FROM `hs_")
                .append(data.getInternalName())
                .append("` GROUP BY `inserted_at`, `deleted_at` ORDER BY `timestamp` ASC;");
        log.trace("mapped find all from history view query [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default List<TableHistoryDto> resultListToTableHistoryDto(ResultSet data) throws SQLException {
        final List<TableHistoryDto> history = new LinkedList<>();
        while (data.next()) {
            history.add(TableHistoryDto.builder()
                    .timestamp(data.getTimestamp(1)
                            .toInstant())
                    .event(data.getString(2))
                    .total(data.getLong(3))
                    .build());
        }
        return history;
    }

    @Transactional(readOnly = true)
    default Object dataColumnToObject(Object data, TableColumn column) throws DateTimeException {
        if (data == null) {
            return null;
        }
        log.trace("map data {} to table column {}", data, column);
        switch (column.getColumnType()) {
            case BLOB:
                log.trace("mapping {} to blob", data);
                return new MariaDbBlob((byte[]) data);
            case DATE:
                if (column.getDateFormat() == null) {
                    log.error("Missing date format for column {} of table {}", column.getId(),
                            column.getTable().getId());
                    throw new IllegalArgumentException("Missing date format");
                }
                log.trace("mapping {} to date with format '{}'", data, column.getDateFormat());
                final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive() /* case insensitive to parse JAN and FEB */
                        .appendPattern(column.getDateFormat().getUnixFormat())
                        .toFormatter(Locale.ENGLISH);
                final LocalDate date = LocalDate.parse(String.valueOf(data), formatter);
                return date.atStartOfDay(ZoneId.of("UTC"))
                        .toInstant();
            case TIMESTAMP:
                if (column.getDateFormat() == null) {
                    log.error("Missing date format for column {} of table {}", column.getId(),
                            column.getTable().getId());
                    throw new IllegalArgumentException("Missing date format");
                }
                log.trace("mapping {} to timestamp with format '{}'", data, column.getDateFormat());
                return Timestamp.valueOf(data.toString())
                        .toInstant();
            case ENUM:
            case TEXT:
            case STRING:
                log.trace("mapping {} to character array", data);
                return String.valueOf(data);
            case NUMBER:
                log.trace("mapping {} to non-decimal number", data);
                return new BigInteger(String.valueOf(data));
            case DECIMAL:
                log.trace("mapping {} to decimal number", data);
                return Double.valueOf(String.valueOf(data));
            case BOOLEAN:
                log.trace("mapping {} to boolean", data);
                return Boolean.valueOf(String.valueOf(data));
            default:
                throw new IllegalArgumentException("Column type not known");
        }
    }

    @Named("EscapedString")
    default String stringToEscapedString(String name) {
        if (name != null && !name.startsWith("`") && !name.endsWith("`")) {
            return "`" + name + "`";
        }
        return name;
    }

    default Long resultSetToNumber(ResultSet data) throws TableMalformedException, QueryStoreException {
        try {
            if (!data.next()) {
                log.error("Failed to map number");
                throw new TableMalformedException("Failed to map number");
            }
            return data.getLong(1);
        } catch (SQLException e) {
            log.error("Failed to retrieve number");
            throw new QueryStoreException("Failed to retrieve number");
        }
    }

    default String selectItemToEscapedString(SelectItem data) {
        final String item = data.toString();
        final int idx = item.indexOf('.');
        if (idx == -1) {
            return "`" + item + "`";
        }
        return "`" + item.substring(idx + 1) + "`";
    }

    /**
     * Generates an insert statement so that the data from the temporary table is inserted in the original one.
     *
     * @param table
     * @return
     */
    default PreparedStatement generateInsertFromTemporaryTableSQL(Connection connection, Table table) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("INSERT INTO `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("` SELECT ");
        for (TableColumn tc : table.getColumns()) {
            statement.append("`");
            statement.append(tc.getInternalName()).append("`,");
        }

        statement.deleteCharAt(statement.length() - 1);
        statement.append(" FROM `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`");

        statement.append(" ON DUPLICATE KEY UPDATE ");
        for (TableColumn tc : table.getColumns())
            statement.append("`")
                    .append(tc.getInternalName())
                    .append("`")
                    .append("=")
                    .append("VALUES(`")
                    .append(tc.getInternalName())
                    .append("`),");
        statement.deleteCharAt(statement.length() - 1);
        statement.append(";");
        log.trace("mapped raw insert query [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement ps, TableColumnType columnType, int idx, Object value) throws SQLException {
        switch (columnType) {
            case TEXT:
            case STRING:
                log.trace("prepare statement idx {} string {}", idx, value);
                if (value == null) {
                    ps.setNull(idx, Types.VARCHAR);
                    break;
                }
                ps.setString(idx, String.valueOf(value));
                break;
            case DATE:
                log.trace("prepare statement idx {} date {}", idx, value);
                if (value == null) {
                    ps.setNull(idx, Types.DATE);
                    break;
                }
                ps.setDate(idx, Date.valueOf(String.valueOf(value)));
                break;
            case NUMBER:
                log.trace("prepare statement idx {} number {}", idx, value);
                if (value == null) {
                    ps.setNull(idx, Types.BIGINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case DECIMAL:
                log.trace("prepare statement idx {} decimal {}", idx, value);
                if (value == null) {
                    ps.setNull(idx, Types.DECIMAL);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case BOOLEAN:
                log.trace("prepare statement idx {} boolean {}", idx, value);
                if (value == null) {
                    ps.setNull(idx, Types.BOOLEAN);
                    break;
                }
                ps.setBoolean(idx, Boolean.parseBoolean(String.valueOf(value)));
                break;
            case TIMESTAMP:
                log.trace("prepare statement idx {} timestamp {}", idx, value);
                if (value == null) {
                    ps.setNull(idx, Types.TIMESTAMP);
                    break;
                }
                ps.setTimestamp(idx, Timestamp.valueOf(String.valueOf(value)));
            default:
                log.error("Failed to map column type {}", columnType);
                log.debug("failed to map column type {} at index {} for value {}", columnType, idx, value);
                log.trace("prepare statement idx {} other {}", idx, value);
                throw new IllegalArgumentException("Failed to map column type");
        }
    }

}
