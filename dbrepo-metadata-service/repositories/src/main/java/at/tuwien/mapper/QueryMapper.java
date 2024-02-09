package at.tuwien.mapper;

import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.exception.ImageNotSupportedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.querystore.Query;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {LinkedList.class})
public interface QueryMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryMapper.class);

    DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    @Mappings({
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "identifiers", expression = "java(new LinkedList())")
    })
    QueryDto queryToQueryDto(Query data);

    @Mappings({
            @Mapping(target = "identifiers", expression = "java(new LinkedList())")
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
        return slug.toLowerCase(Locale.ENGLISH);
    }

    default QueryResultDto resultListToQueryResultDto(List<TableColumn> columns, ResultSet result) throws SQLException {
        log.trace("mapping result list to query result, columns={}, result={}", columns, result);
        final List<Map<String, Object>> resultList = new LinkedList<>();
        while (result.next()) {
            /* map the result set to the columns through the stored metadata in the metadata database */
            int[] idx = new int[]{1};
            final Map<String, Object> map = new HashMap<>();
            for (final TableColumn column : columns) {
                final String columnOrAlias;
                if (column.getAlias() != null) {
                    log.debug("column {} has alias {}", column.getInternalName(), column.getAlias());
                    columnOrAlias = column.getAlias();
                } else {
                    columnOrAlias = column.getInternalName();
                }
                if (List.of(TableColumnType.BLOB, TableColumnType.TINYBLOB, TableColumnType.MEDIUMBLOB, TableColumnType.LONGBLOB).contains(column.getColumnType())) {
                    log.debug("column {} is of type blob", columnOrAlias);
                    final Blob blob = result.getBlob(idx[0]++);
                    map.put(columnOrAlias, Hex.encodeHexString(blob.getBytes(1, (int) blob.length())).toUpperCase());
                    continue;
                }
                final Object object = dataColumnToObject(result.getObject(idx[0]++), column);
                if (object == null) {
                    log.warn("result set for column {} is empty (=null)", column.getInternalName());
                }
                map.put(columnOrAlias, object);
            }
            resultList.add(map);
        }
        final int[] idx = new int[]{0};
        final List<Map<String, Integer>> headers = columns.stream()
                .map(c -> (Map<String, Integer>) new LinkedHashMap<String, Integer>() {{
                    put(c.getAlias() != null ? c.getAlias() : c.getInternalName(), idx[0]++);
                }})
                .toList();
        log.trace("created ordered header list: {}", headers);
        return QueryResultDto.builder()
                .result(resultList)
                .headers(headers)
                .build();
    }

    default void importCsvQuery(Connection connection, Table table, ImportDto csv) throws SQLException {
        final Statement statement = connection.createStatement();
        final StringBuilder query0 = new StringBuilder("CREATE TABLE IF NOT EXISTS `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`")
                .append(" LIKE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("`;");
        log.trace("mapped create temporary table statement: {}", query0);
        statement.execute(query0.toString());
        final String query1 = pathToRawInsertQuery(table, csv);
        log.trace("mapped import csv statement: {}", query1);
        statement.execute(query1.toString());
        final String query2 = generateInsertFromTemporaryTableSQL(table);
        log.trace("mapped import table statement: {}", query2);
        statement.execute(query2.toString());
        final StringBuilder query3 = new StringBuilder("DROP TABLE IF EXISTS `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`;");
        log.trace("mapped drop temporary table statement: {}", query3);
        statement.execute(query3.toString());

    }

    default String pathToRawInsertQuery(Table table, ImportDto data) {
        final StringBuilder statement = new StringBuilder("LOAD DATA INFILE '/tmp/")
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
                    } else if (column.getColumnType().equals(TableColumnType.BOOL)) {
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
        return statement.toString();
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
        log.trace("mapping table data to insert query, table={}, data={}", table, data);
        if (table.getColumns().size() == 0) {
            log.error("Column size is zero");
            throw new TableMalformedException("Columns are not known");
        }
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getName().equals("mariadb")) {
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
        if (!table.getDatabase().getContainer().getImage().getName().equals("mariadb")) {
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

    default String tableToRawCountAllQuery(Table table, Instant timestamp)
            throws ImageNotSupportedException {
        log.trace("mapping table to raw count query, table={}, timestamp={}", table, timestamp);
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getName().equals("mariadb")) {
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
        if (!view.getDatabase().getContainer().getImage().getName().equals("mariadb")) {
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

    default String queryToRawTimestampedQuery(String query, Instant timestamp, Boolean selection, Long page, Long size) {
        log.trace("mapping query to timestamped query, query={}, timestamp={}, selection={}, page={}, size={}",
                query, timestamp, selection, page, size);
        /* param check */
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
        if (!table.getDatabase().getContainer().getImage().getName().equals("mariadb")) {
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

    default String viewToRawFindAllQuery(View view, Long size, Long page) {
        log.trace("mapping view to find all query, view={}, size={}, page={}", view, size, page);
        /* param check */
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
        log.trace("pagination size/limit of {}", size);
        statement.append(" LIMIT ")
                .append(size);
        log.trace("pagination page/offset of {}", page);
        statement.append(" OFFSET ")
                .append(page * size)
                .append(";");
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
            log.trace("mapped select history query {} to prepared statement", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    /**
     * Parses the stored columns from a given query.
     *
     * @param query    The query.
     * @param database The database that contains the list of tables with list of columns.
     * @return List of columns in the order they are referenced in the query.
     * @throws JSQLParserException The columns could not be extracted from the query.
     */
    @Transactional(readOnly = true)
    default List<TableColumn> parseColumns(String query, Database database) throws JSQLParserException {
        final List<TableColumn> columns = new ArrayList<>();
        final CCJSqlParserManager parserRealSql = new CCJSqlParserManager();
        final net.sf.jsqlparser.statement.Statement statement = parserRealSql.parse(new StringReader(query));
        log.debug("parse columns from query: {}", query);
        /* check */
        if (!(statement instanceof Select)) {
            log.error("Query attempts to update the dataset, not a SELECT statement");
            throw new JSQLParserException("Query attempts to update the dataset");
        }
        /* start parsing */
        final Select selectStatement = (Select) statement;
        final PlainSelect ps = (PlainSelect) selectStatement.getSelectBody();
        final List<SelectItem> clauses = ps.getSelectItems();
        log.trace("columns referenced in the from-clause: {}", clauses);
        /* Parse all tables */
        final List<FromItem> fromItems = new ArrayList<>(fromItemToFromItems(ps.getFromItem()));
        if (ps.getJoins() != null && !ps.getJoins().isEmpty()) {
            log.trace("query contains join items: {}", ps.getJoins());
            for (net.sf.jsqlparser.statement.select.Join j : ps.getJoins()) {
                if (j.getRightItem() != null) {
                    fromItems.add(j.getRightItem());
                }
            }
        }
        final List<TableColumn> allColumns = database.getTables()
                .stream()
                .map(Table::getColumns)
                .flatMap(List::stream)
                .toList();
        log.trace("columns referenced in the from-clause and join-clause(s): {}", clauses);
        /* Checking if all tables or views exist */
        log.trace("table/view/join referenced in the statement: {}", fromItems.stream().map(this::fromItemToFromItems).flatMap(List::stream).collect(Collectors.toList()));
        /* Checking if all columns exist */
        for (SelectItem clause : clauses) {
            final SelectExpressionItem item = (SelectExpressionItem) clause;
            final Column column = (Column) item.getExpression();
            final Optional<net.sf.jsqlparser.schema.Table> optional = fromItems.stream()
                    .map(t -> (net.sf.jsqlparser.schema.Table) t)
                    .filter(t -> {
                        if (column.getTable() == null) {
                            /* column does not reference a specific table, so there is only one table */
                            final String tableName = ((net.sf.jsqlparser.schema.Table) fromItems.get(0)).getName().replace("`", "");
                            return tableMatches(t, tableName);
                        }
                        final String tableName = column.getTable().getName().replace("`", "");
                        return tableMatches(t, tableName);
                    })
                    .findFirst();
            if (optional.isEmpty()) {
                log.error("Failed to find table/view {} (with designator {})", column.getTable().getName(), column.getTable().getAlias());
                throw new JSQLParserException("Failed to find table/view " + column.getTable().getName() + " (with alias " + column.getTable().getAlias() + ")");
            }
            final Optional<TableColumn> optionalColumn = allColumns.stream()
                    .filter(c -> c.getInternalName().equals(column.getColumnName().replace("`", "")))
                    .filter(c -> columnMatches(c, optional.get().getName().replace("`", "")))
                    .findFirst();
            if (optionalColumn.isEmpty()) {
                log.error("Failed to find column with name {} in {}", column.getColumnName(), allColumns.stream().map(TableColumn::getInternalName).toList());
                throw new JSQLParserException("Failed to find column with name " + column.getColumnName() + " in " + allColumns.stream().map(TableColumn::getInternalName).toList());
            }
            final TableColumn aliasColumn = optionalColumn.get();
            if (item.getAlias() != null) {
                aliasColumn.setAlias(item.getAlias().getName().replace("`", ""));
            }
            log.trace("found column with internal name {} and alias {}", aliasColumn.getInternalName(), aliasColumn.getAlias());
            columns.add(aliasColumn);
        }
        return columns;
    }

    default List<FromItem> fromItemToFromItems(FromItem data) {
        return fromItemToFromItems(data, 0);
    }

    default List<FromItem> fromItemToFromItems(FromItem data, Integer level) {
        final List<FromItem> fromItems = new LinkedList<>();
        if (data instanceof net.sf.jsqlparser.schema.Table table) {
            fromItems.add(data);
            log.trace("from-item {} is of type table: level ~> {}", table.getName(), level);
            return fromItems;
        }
        if (data instanceof SubJoin subJoin) {
            log.trace("from-item is of type sub-join: level ~> {}", level);
            for (Join join : subJoin.getJoinList()) {
                fromItems.addAll(fromItemToFromItems(join.getRightItem(), level + 1));
            }
            fromItems.addAll(fromItemToFromItems(((SubJoin) data).getLeft(), level + 1));
            return fromItems;
        }
        log.warn("unknown from-item {}", data);
        return null;
    }

    default boolean tableMatches(net.sf.jsqlparser.schema.Table table, String otherTableName) {
        final String tableName = table.getName()
                .trim()
                .replace("`", "");
        if (table.getAlias() == null) {
            /* table does not have designator */
            log.trace("table {} has no designator", tableName);
            return tableName.equals(otherTableName);
        }
        /* has designator */
        final String designator = table.getAlias()
                .getName()
                .trim()
                .replace("`", "");
        log.trace("table {} has designator {}", tableName, designator);
        return designator.equals(otherTableName);
    }

    @Transactional(readOnly = true)
    default boolean columnMatches(TableColumn column, String tableOrView) {
        if (column.getTable().getInternalName().equals(tableOrView)) {
            /* matches table name */
            return true;
        }
        if (column.getViews() == null) {
            return false;
        }
        /* maybe matches one of the views */
        return column.getViews()
                .stream()
                .anyMatch(v -> v.getInternalName().equals(tableOrView));
    }

    default PreparedStatement obtainTableMetadataRawQuery(Connection connection, String databaseName, String tableName) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("SELECT `ORDINAL_POSITION`, `COLUMN_DEFAULT`, `IS_NULLABLE`, `DATA_TYPE`, `CHARACTER_MAXIMUM_LENGTH`, `NUMERIC_PRECISION`, `NUMERIC_SCALE`, `COLUMN_TYPE`, `COLUMN_KEY`, `COLUMN_NAME` FROM `information_schema`.`COLUMNS` WHERE `TABLE_SCHEMA` = '")
                .append(databaseName)
                .append("' AND `TABLE_NAME` = '")
                .append(tableName)
                .append("'");
        log.trace("mapped obtain table metadata statement {} to prepared statement", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement databaseToDatabaseConstraintMetadata(Connection connection, String databaseName, String tableName) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("SELECT tc.`CONSTRAINT_TYPE`, cc.`CONSTRAINT_NAME`, cc.`LEVEL`, cc.`CHECK_CLAUSE`, rc.`UNIQUE_CONSTRAINT_NAME`, rc.`REFERENCED_TABLE_NAME` FROM information_schema.`TABLE_CONSTRAINTS` tc LEFT JOIN information_schema.`CHECK_CONSTRAINTS` cc ON tc.`CONSTRAINT_SCHEMA` = cc.`CONSTRAINT_SCHEMA` AND tc.`TABLE_NAME` = cc.`TABLE_NAME` AND tc.`CONSTRAINT_TYPE` = 'CHECK' LEFT JOIN information_schema.`REFERENTIAL_CONSTRAINTS` rc ON tc.`CONSTRAINT_SCHEMA` = rc.`CONSTRAINT_SCHEMA` AND tc.`TABLE_NAME` = rc.`TABLE_NAME` AND tc.`CONSTRAINT_TYPE` = 'FOREIGN KEY' WHERE tc.`TABLE_SCHEMA` = '")
                .append(databaseName)
                .append("' AND tc.`TABLE_NAME` = '")
                .append(tableName)
                .append("'");
        log.trace("statement={}", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableEnableSystemVersioning(Connection connection, String databaseName, String tableName)
            throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("ALTER TABLE `")
                .append(databaseName)
                .append("`.`")
                .append(tableName)
                .append("` ADD SYSTEM VERSIONING;");
        log.trace("mapped enable system-versioning statement {} to prepared statement", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}: {}", statement, e.getMessage());
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
            case TIMESTAMP, DATETIME -> {
                if (column.getDateFormat() == null) {
                    log.error("Missing date format for column {} of table {}", column.getId(),
                            column.getTable().getId());
                    throw new IllegalArgumentException("Missing date format");
                }
                log.trace("mapping {} to timestamp with format '{}'", data, column.getDateFormat());
                return Timestamp.valueOf(data.toString())
                        .toInstant();
            }
            case BINARY, VARBINARY, BIT -> {
                log.trace("mapping {} -> binary", data);
                return Long.parseLong(String.valueOf(data), 2);
            }
            case TEXT, CHAR, VARCHAR, TINYTEXT, MEDIUMTEXT, LONGTEXT, ENUM, SET -> {
                log.trace("mapping {} -> string", data);
                return String.valueOf(data);
            }
            case BIGINT -> {
                log.trace("mapping {} -> biginteger", data);
                return new BigInteger(String.valueOf(data));
            }
            case INT, TINYINT, SMALLINT, MEDIUMINT -> {
                log.trace("mapping {} -> integer", data);
                return Integer.parseInt(String.valueOf(data));
            }
            case DECIMAL, FLOAT, DOUBLE -> {
                log.trace("mapping {} -> double", data);
                return Double.valueOf(String.valueOf(data));
            }
            case BOOL -> {
                log.trace("mapping {} -> boolean", data);
                return Boolean.valueOf(String.valueOf(data));
            }
            case TIME -> {
                log.trace("mapping {} -> time", data);
                return String.valueOf(data);
            }
            case YEAR -> {
                final String tmp = String.valueOf(data);
                log.trace("mapping {} -> year", tmp);
                return tmp.substring(0, tmp.indexOf('-'));
            }
        }
        log.warn("column type {} is not known", column.getColumnType());
        throw new IllegalArgumentException("Column type not known");
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

    default String generateInsertFromTemporaryTableSQL(Table table) {
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
        return statement.toString();
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement ps, TableColumnType columnType, int idx, Object value) throws SQLException {
        switch (columnType) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
                log.trace("prepare statement idx {} blob", idx);
                if (value == null) {
                    ps.setNull(idx, Types.BLOB);
                    break;
                }
                try {
                    ps.setBlob(idx, FileUtils.openInputStream(new File(String.valueOf(value))));
                } catch (IOException e) {
                    log.error("Failed to set blob: {}", e.getMessage());
                    throw new SQLException("Failed to set blob: " + e.getMessage(), e);
                }
                break;
            case TEXT, CHAR, VARCHAR, TINYTEXT, MEDIUMTEXT, LONGTEXT, ENUM, SET:
                log.trace("prepare statement idx {} {} {}", idx, columnType, value);
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
            case BIGINT:
                log.trace("prepare statement idx {} bigint {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.BIGINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case INT, MEDIUMINT:
                log.trace("prepare statement idx {} {} {}", idx, columnType, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.INTEGER);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case TINYINT:
                log.trace("prepare statement idx {} tinyint {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TINYINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case SMALLINT:
                log.trace("prepare statement idx {} smallint {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.SMALLINT);
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
            case FLOAT:
                log.trace("prepare statement idx {} float {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.FLOAT);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case DOUBLE:
                log.trace("prepare statement idx {} double {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DOUBLE);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case BINARY, VARBINARY, BIT:
                log.trace("prepare statement idx {} {} {}", idx, columnType, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DECIMAL);
                    break;
                }
                ps.setBinaryStream(idx, (InputStream) value);
                break;
            case BOOL:
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
            case DATETIME:
                log.trace("prepare statement idx {} datetime {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIMESTAMP);
                    break;
                }
                ps.setTimestamp(idx, Timestamp.valueOf(String.valueOf(value)));
                break;
            case TIME:
                log.trace("prepare statement idx {} time {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIME);
                    break;
                }
                ps.setTime(idx, Time.valueOf(String.valueOf(value)));
                break;
            case YEAR:
                log.trace("prepare statement idx {} year {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIME);
                    break;
                }
                ps.setString(idx, String.valueOf(value));
                break;
            default:
                log.error("Failed to map column type {} at index {} for value {}", columnType, idx, value);
                throw new IllegalArgumentException("Failed to map column type " + columnType);
        }
    }

}
