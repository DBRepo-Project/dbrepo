package at.tuwien.mapper;

import at.tuwien.api.database.query.*;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
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

    @Mappings({
            @Mapping(target = "createdBy", ignore = true)
    })
    QueryDto queryToQueryDto(Query data);

    @Mappings({
            @Mapping(target = "createdBy", ignore = true)
    })
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
        final String name = slug.toLowerCase(Locale.ENGLISH);
        log.trace("mapped name {} to name {}", data, name);
        return name;
    }

    default QueryResultDto resultListToQueryResultDto(List<TableColumn> columns, ResultSet result) throws SQLException {
        log.trace("mapping result list to query result, columns={}, result={}", columns, result);
        final List<Map<String, Object>> resultList = new LinkedList<>();
        while (result.next()) {
            /* map the result set to the columns through the stored metadata in the metadata database */
            int[] idx = new int[]{1};
            final Map<String, Object> map = new HashMap<>();
            for (final TableColumn column : columns) {
                final Object object = dataColumnToObject(result.getObject(idx[0]++), column);
                if (object == null) {
                    log.warn("result set for column {} is empty (=null)", column.getInternalName());
                }
                final String columnOrAlias = column.getAlias() != null ? column.getAlias() : column.getInternalName();
                map.put(columnOrAlias, object);
            }
            resultList.add(map);
        }
        log.trace("mapped result list {} to result map {}", result, resultList);
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
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped create table {} to prepared statement {}", table.getName(), pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement dropTemporaryTableSQL(Connection connection, Table table) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("DROP TABLE IF EXISTS `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`;");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped drop temporary table {} to prepared statement {}", table.getName(), pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
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
                        log.trace("import column is auto generated, skip");
                        return;
                    }
                    statement.append(idx[0] != 0 ? "," : "");
                    /* format as variable */
                    statement.append("@")
                            .append(column.getInternalName());
                    if (column.getDateFormat() != null) {
                        log.trace("import column has date format, need to format it differently");
                        /* reformat dates */
                        columnToDateSet(data, column, set);
                    } else if (column.getColumnType().equals(TableColumnType.BOOLEAN)) {
                        log.trace("import column has boolean format, need to format it differently");
                        /* reformat booleans */
                        columnToBoolSet(data, column, set);
                    } else {
                        log.trace("import column has text format");
                        /* reformat others */
                        columnToTextSet(data, column, set);
                    }
                    idx[0]++;
                });
        statement.append(")")
                .append(set.length() != 0 ? (" SET " + set) : "")
                .append(";");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped drop temporary table {} to prepared statement {}", table.getName(), pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default void columnToBoolSet(ImportDto data, TableColumn column, StringBuilder set) {
        log.trace("mapping column to bool set, data={}, column={}, set=(generated)", data, column);
        set.append(set.length() != 0 ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = ");
        if (data.getNullElement() != null) {
            log.trace("import has null element present");
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getNullElement())
                    .append("'),NULL,");
            columnToBoolSet2(data, column, set);
            set.append(")");
            return;
        }
        columnToBoolSet2(data, column, set);
    }

    default void columnToBoolSet2(ImportDto data, TableColumn column, StringBuilder set) {
        log.trace("mapping column to inner bool set, data={}, column={}, set=(generated)", data, column);
        if (data.getTrueElement() != null) {
            log.trace("import has true element present");
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getTrueElement())
                    .append("'),TRUE,");
            if (data.getFalseElement() != null) {
                log.trace("import has false element present (both true and false)");
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
            log.trace("import has false element present");
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getFalseElement())
                    .append("'),FALSE,");
            if (data.getTrueElement() != null) {
                log.trace("import has true element present (both true and false)");
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

    default void columnToTextSet(ImportDto data, TableColumn column, StringBuilder set) {
        log.trace("mapping column to text set");
        set.append(set.length() != 0 ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = ");
        if (data.getNullElement() != null) {
            log.trace("import has null element present");
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

    default void columnToDateSet(ImportDto data, TableColumn column, StringBuilder set) {
        log.trace("mapping column to date set");
        set.append(set.length() != 0 ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = STR_TO_DATE(");
        if (data.getNullElement() != null) {
            log.trace("import has null element present");
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

    default PreparedStatement tableToRawExportQuery(Connection connection, Table table, Instant timestamp,
                                                    String filename) throws QueryMalformedException {
        log.trace("mapping table to raw export query, table={}, timestamp={}, filename={}", table, timestamp, filename);
        final StringBuilder statement = new StringBuilder("SELECT ");
        int[] idx = new int[]{0};
        table.getColumns()
                .forEach(column -> {
                    statement.append(idx[0] != 0 ? "," : "")
                            .append("'")
                            .append(column.getInternalName())
                            .append("'");
                    idx[0]++;
                });
        statement.append(" UNION ALL SELECT ");
        int[] jdx = new int[]{0};
        table.getColumns()
                .forEach(column -> {
                    statement.append(jdx[0] != 0 ? "," : "")
                            .append("`")
                            .append(column.getInternalName())
                            .append("`");
                    jdx[0]++;
                });
        statement.append(" FROM `")
                .append(table.getInternalName())
                .append("`");
        if (timestamp != null) {
            log.trace("export has timestamp present");
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        statement.append(" INTO OUTFILE '/tmp/")
                .append(filename)
                .append("' CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"';");
        statement.append(";");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped export query {} to prepared statement {}", table.getName(), pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement queryToRawExportQuery(Connection connection, Query query, String filename)
            throws QueryMalformedException {
        log.trace("mapping query to export query, query={}, filename={}", query, filename);
        if (query.getQuery().contains(";")) {
            log.trace("remove ending semicolon from statement");
            query.setQuery(query.getQuery().substring(0, query.getQuery().indexOf(";")));
        }
        /* insert the FOR SYSTEM_TIME ... part after the FROM in the query */
        final StringBuilder versionPart = new StringBuilder(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                .append(mariaDbFormatter.format(query.getCreated()))
                .append("' ");
        final Pattern pattern = Pattern.compile("from `?[a-zA-Z0-9_-]+`?", Pattern.CASE_INSENSITIVE) /* https://mariadb.com/kb/en/columnstore-naming-conventions/ */;
        final Matcher matcher = pattern.matcher(query.getQuery());
        if (!matcher.find()) {
            log.error("Failed to find 'from' clause in query");
            throw new QueryMalformedException("Failed to find from clause");
        }
        log.trace("found group from {} to {} in '{}'", matcher.start(), matcher.end(), query.getQuery());
        final StringBuilder statement = new StringBuilder(query.getQuery().substring(0, matcher.end(0)))
                .append(versionPart)
                .append(query.getQuery().substring(matcher.end(0)))
                .append(" INTO OUTFILE '/tmp/")
                .append(filename)
                .append("' CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"';");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped export query {} to prepared statement {}", statement, pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableCsvDtoToRawInsertQuery(Connection connection, Table table, TableCsvDto data)
            throws TableMalformedException, ImageNotSupportedException, QueryMalformedException {
        log.trace("mapping table csv to insert query, table={}, data={}", table, data);
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
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped insert query {} to prepared statement {}", statement, pstmt);
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
                    log.error("Failed to map column name {}, known names: {}", column.getInternalName(), data.getData().keySet());
                    throw new TableMalformedException("Failed to map column names: not all columns are present in the tuple!");
                }
                prepareStatementWithColumnTypeObject(pstmt, column.getColumnType(), idx[0]++, tuple.get().getValue());
            }
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableCsvDtoToRawDeleteQuery(Connection connection, Table table, TableCsvDeleteDto data)
            throws TableMalformedException, ImageNotSupportedException, QueryMalformedException {
        log.trace("table csv to delete query, table={}, data={}", table, data);
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
                .forEach((key, value) -> statement.append(idx[0]++ == 0 ? "" : " AND ")
                        .append("`")
                        .append(key)
                        .append("` ")
                        .append(value == null ? "IS" : "=")
                        .append(" ?"));
        /* prepare */
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped delete query {} to prepared statement {}", statement, pstmt);
            for (Map.Entry<String, Object> entry : data.getKeys().entrySet()) {
                final Optional<TableColumn> optional = table.getColumns()
                        .stream()
                        .filter(c -> c.getInternalName().equals(entry.getKey()))
                        .findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find column with name {}, available names: {}", entry.getKey(), data.getKeys().keySet());
                    throw new QueryMalformedException("Failed to find column");
                }
                prepareStatementWithColumnTypeObject(pstmt, optional.get().getColumnType(), i++, entry.getValue());
            }
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableCsvDtoToRawUpdateQuery(Connection connection, Table table, TableCsvUpdateDto data)
            throws TableMalformedException, ImageNotSupportedException, QueryMalformedException {
        log.trace("mapping table csv to update query, table={}, data={}", table, data);
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
                            .append("` ");
                    if (value == null) {
                        statement.append(" IS NULL");
                    } else {
                        statement.append(" = '")
                                .append(value)
                                .append("'");
                    }
                    jdx[0]++;
                });
        statement.append(";");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                if (entry.getValue() == null) {
                    log.trace("entry is null, preparing null");
                    pstmt.setNull(i++, Types.NULL);
                } else if (entry.getValue().equals(true) || entry.getValue().equals(false)) {
                    log.trace("entry is not null, preparing boolean");
                    pstmt.setBoolean(i++, Boolean.parseBoolean(String.valueOf(entry.getValue())));
                } else {
                    log.trace("entry is not null, preparing string");
                    pstmt.setString(i++, String.valueOf(entry.getValue()));
                }
            }
            log.trace("mapped update query {} to prepared statement {}", statement, pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default String tableColumnsToSelection(List<TableColumn> data) {
        final StringBuilder selection = new StringBuilder();
        final int[] idx = {0};
        data.forEach(column -> selection.append(idx[0]++ == 0 ? "" : ", ")
                .append("`")
                .append(column.getInternalName())
                .append("`"));
        log.trace("mapped columns {} to selection {}", data, selection);
        return selection.toString();
    }

    default String tableToRawCountAllQuery(Table table, Instant timestamp)
            throws ImageNotSupportedException {
        log.trace("mapping table to raw count query, table={}, timestamp={}", table, timestamp);
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Image not supported.");
        }
        if (timestamp == null) {
            log.trace("timestamp is null, setting it to now");
            timestamp = Instant.now();
        }
        return columnsToRawCountAllQuery(table.getInternalName(), timestamp);
    }

    default String viewToRawCountAllQuery(View view)
            throws ImageNotSupportedException {
        log.trace("mapping table to raw count query, view={}", view);
        /* check image */
        if (!view.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Image not supported.");
        }
        return columnsToRawCountAllQuery(view.getInternalName(), null);
    }

    default String columnsToRawCountAllQuery(String tableName, Instant timestamp) {
        final StringBuilder statement = new StringBuilder("SELECT COUNT(*) FROM `")
                .append(nameToInternalName(tableName))
                .append("`");
        if (timestamp != null) {
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC")))
                    .append("'");
        }
        statement.append(";");
        return statement.toString();
    }

    default String queryToRawTimestampedQuery(String query, Database database, Instant timestamp, Boolean selection, Long page, Long size)
            throws ImageNotSupportedException, QueryMalformedException {
        log.trace("mapping query to timestamped query, query={}, database={}, timestamp={}, selection={}, page={}, size={}",
                query, database, timestamp, selection, page, size);
        /* param check */
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (timestamp == null) {
            log.error("Timestamp is null");
            throw new IllegalArgumentException("Please provide a timestamp before");
        }
        if (page == null) {
            log.warn("page is null, default to 0");
            page = 0L;
        }
        if (size == null) {
            log.warn("size is null, default to 100");
            size = 100L;
        }
        query = query.toLowerCase(Locale.ROOT)
                .trim();
        if (query.matches(";$")) {
            /* remove last semicolon */
            query = query.substring(0, query.length() - 1);
        }
        /* query check (this is enforced by the db also) */
        if (Stream.of("count").anyMatch(query::contains)) {
            log.error("Query contains unsupported operation, one of {}", List.of("COUNT"));
        }
        if (Stream.of("delete", "update", "truncate", "create", "drop").anyMatch(query::contains)) {
            log.error("Query attempts to modify the database");
            throw new QueryMalformedException("Query attempts to modify the database");
        }
        final StringBuilder sb = new StringBuilder();
        if (selection) {
            /* is not a count query */
            sb.append("SELECT * FROM (");
        } else {
            sb.append("SELECT COUNT(*) FROM (");
        }
        /* insert statement */
        sb.append(query);
        /* system time */
        sb.append(") FOR SYSTEM_TIME AS OF TIMESTAMP '")
                .append(LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC")))
                .append("' as tbl");
        /* pagination */
        log.trace("pagination size/limit of {}", size);
        sb.append(" LIMIT ")
                .append(size);
        log.trace("pagination page/offset of {}", page);
        sb.append(" OFFSET ")
                .append(page * size);
        sb.append(";");
        return sb.toString();
    }

    default String tableToRawFindAllQuery(Table table, Instant timestamp, Long size, Long page)
            throws ImageNotSupportedException {
        log.trace("mapping table to find all query, table={}, timestamp={}, size={}, page={}",
                table, timestamp, size, page);
        /* param check */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
            log.trace("no timestamp provided, default to {}", timestamp);
        } else {
            log.trace("timestamp provided {}", timestamp);
        }
        return columnsToRawFindAllQuery(table.getInternalName(), table.getColumns(), timestamp, size, page);
    }

    default String viewToRawFindAllQuery(View view, Long size, Long page)
            throws ImageNotSupportedException {
        log.trace("mapping view to find all query, view={}, size={}, page={}", view, size, page);
        /* param check */
        if (!view.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        return columnsToRawFindAllQuery(view.getInternalName(), view.getColumns(), null, size, page);
    }

    private String columnsToRawFindAllQuery(String tableName, List<TableColumn> columns, Instant timestamp, Long size, Long page) {
        final int[] idx = new int[]{0};
        final StringBuilder statement = new StringBuilder("SELECT ");
        columns.forEach(column -> statement.append(idx[0]++ > 0 ? "," : "")
                .append("`")
                .append(column.getInternalName())
                .append("`"));
        statement.append(" FROM `")
                .append(nameToInternalName(tableName))
                .append("`");
        if (timestamp != null) {
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(LocalDateTime.ofInstant(timestamp, ZoneId.of("UTC")))
                    .append("'");
        }
        if (size != null && page != null) {
            log.trace("pagination size/limit of {}", size);
            statement.append(" LIMIT ")
                    .append(size);
            log.trace("pagination page/offset of {}", page);
            statement.append(" OFFSET ")
                    .append(page * size)
                    .append(";");
        }
        return statement.toString();
    }

    @Transactional(readOnly = true)
    default PreparedStatement historyRawQuery(Connection connection, Table data) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("SELECT")
                .append(" IF(`deleted_at` IS NULL, `inserted_at`, `deleted_at`) as `timestamp`")
                .append(", IF(`deleted_at` IS NULL, 'INSERT', 'DELETE') as `event`")
                .append(", `total` FROM `hs_")
                .append(data.getInternalName())
                .append("`;");
        log.trace("mapped find all from history view query [{}]", statement);
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped select history query {} to prepared statement {}", statement, pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {} reason: {}", statement, e.getMessage());
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
        log.trace("mapped result set {} to history {}", data, history);
        return history;
    }

    @Transactional(readOnly = true)
    default Object dataColumnToObject(Object data, TableColumn column) throws DateTimeException {
        if (data == null) {
            return null;
        }
        switch (column.getColumnType()) {
            case BLOB -> {
                log.trace("mapping {} to blob", data);
                return new MariaDbBlob((byte[]) data);
            }
            case DATE -> {
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
            }
            case TIMESTAMP -> {
                if (column.getDateFormat() == null) {
                    log.error("Missing date format for column {} of table {}", column.getId(),
                            column.getTable().getId());
                    throw new IllegalArgumentException("Missing date format");
                }
                log.trace("mapping {} to timestamp with format '{}'", data, column.getDateFormat());
                return Timestamp.valueOf(data.toString())
                        .toInstant();
            }
            case ENUM, TEXT, STRING -> {
                log.trace("mapping {} to character array", data);
                return String.valueOf(data);
            }
            case NUMBER -> {
                log.trace("mapping {} to non-decimal number", data);
                return new BigInteger(String.valueOf(data));
            }
            case DECIMAL -> {
                log.trace("mapping {} to decimal number", data);
                return Double.valueOf(String.valueOf(data));
            }
            case BOOLEAN -> {
                log.trace("mapping {} to boolean", data);
                return Boolean.valueOf(String.valueOf(data));
            }
            default -> {
                log.warn("column type {} is not known", column.getColumnType());
                throw new IllegalArgumentException("Column type not known");
            }
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
            log.error("Failed to retrieve number: {}", e.getMessage());
            throw new QueryStoreException("Failed to retrieve number", e);
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

    default PreparedStatement generateInsertFromTemporaryTableSQL(Connection connection, Table table)
            throws QueryMalformedException {
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
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped generate insert from temporary table {} to prepared statement {}", statement, pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement ps, TableColumnType columnType, int idx, Object value) throws SQLException {
        switch (columnType) {
            case TEXT:
            case STRING:
                log.trace("prepare statement idx {} string {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.VARCHAR);
                    break;
                }
                ps.setString(idx, String.valueOf(value));
                break;
            case DATE:
                log.trace("prepare statement idx {} date {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DATE);
                    break;
                }
                ps.setDate(idx, Date.valueOf(String.valueOf(value)));
                break;
            case NUMBER:
                log.trace("prepare statement idx {} number {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.BIGINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case DECIMAL:
                log.trace("prepare statement idx {} decimal {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DECIMAL);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case BOOLEAN:
                log.trace("prepare statement idx {} boolean {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.BOOLEAN);
                    break;
                }
                ps.setBoolean(idx, Boolean.parseBoolean(String.valueOf(value)));
                break;
            case TIMESTAMP:
                log.trace("prepare statement idx {} timestamp {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIMESTAMP);
                    break;
                }
                ps.setTimestamp(idx, Timestamp.valueOf(String.valueOf(value)));
                break;
            default:
                log.error("Failed to map column type {} at index {} for value {}", columnType, idx, value);
                throw new IllegalArgumentException("Failed to map column type " + columnType);
        }
    }

}
