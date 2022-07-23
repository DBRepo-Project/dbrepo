package at.tuwien.mapper;

import at.tuwien.InsertTableRawQuery;
import at.tuwien.api.database.query.*;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.exception.QueryMalformedException;
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
import java.sql.Timestamp;
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

    @Mappings({
            @Mapping(target = "creator", ignore = true)
    })
    QueryDto queryToQueryDto(Query data);

    List<QueryDto> queryListToQueryDtoList(List<Query> data);

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

    default QueryResultDto resultListToQueryResultDto(List<TableColumn> columns, List<?> result) {
        final Iterator<?> iterator = result.iterator();
        final List<Map<String, Object>> resultList = new LinkedList<>();
        log.trace("result has {} columns and {} rows", columns.size(), result.size());
        while (iterator.hasNext()) {
            /* map the result set to the columns through the stored metadata in the metadata database */
            int[] idx = new int[]{0};
            final Object[] data;
            if (columns.size() == 1) {
                data = new Object[]{iterator.next()};
            } else {
                data = (Object[]) iterator.next();
            }
            final Map<String, Object> map = new HashMap<>();
            columns
                    .forEach(column -> map.put(column.getName(),
                            dataColumnToObject(data[idx[0]++], column)));
            resultList.add(map);
        }
        return QueryResultDto.builder()
                .result(resultList)
                .build();
    }

    default String generateTemporaryTableSQL(Table table) {
        final StringBuilder generateTable = new StringBuilder("CREATE TABLE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`")
                .append(" LIKE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("`;");
        log.debug(generateTable.toString());
        return generateTable.toString();
    }


    default String dropTemporaryTableSQL(Table table) {
        final StringBuilder t = new StringBuilder("DROP TABLE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`;");
        log.debug(t.toString());
        return t.toString();
    }


    default InsertTableRawQuery pathToRawInsertQuery(Table table, ImportDto data) {
        final StringBuilder query = new StringBuilder("LOAD DATA LOCAL INFILE '")
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
            query.append(" OPTIONALLY ENCLOSED BY '")
                    .append(data.getQuote())
                    .append("'");
        }
        query.append(data.getSkipLines() != null ? (" IGNORE " + data.getSkipLines() + " LINES") : "")
                .append(" (");
        final StringBuilder set = new StringBuilder();
        int[] idx = new int[]{0};
        table.getColumns()
                .forEach(column -> {
                    if (column.getAutoGenerated()) {
                        return;
                    }
                    query.append(idx[0] != 0 ? "," : "");
                    /* format as variable */
                    query.append("@")
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
        query.append(")")
                .append(set.length() != 0 ? (" SET " + set) : "")
                .append(";");
        log.debug("import csv {} for table {}", data.getLocation(), table);
        log.trace("raw import query: [{}]", query);
        return InsertTableRawQuery.builder()
                .query(query.toString())
                .build();
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
                            .getDatabaseFormat())
                    .append("')");
            return;
        }
        set.append("@")
                .append(column.getInternalName())
                .append(", '")
                .append(column.getDateFormat()
                        .getDatabaseFormat())
                .append("')");
    }

    default String tableToRawExportQuery(Table table, Instant timestamp, String filename) {
        final StringBuilder query = new StringBuilder("SELECT ");
        int[] idx = new int[]{0};
        table.getColumns()
                .forEach(column -> {
                    query.append(idx[0] != 0 ? "," : "")
                            .append("`")
                            .append(column.getInternalName())
                            .append("`");
                    idx[0]++;
                });
        query.append("FROM `")
                .append(table.getInternalName())
                .append("`");
        if (timestamp != null) {
            query.append(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        query.append(" INTO OUTFILE '/tmp/")
                .append(filename)
                .append("' CHARACTER SET utf8");
        query.append(";");
        return query.toString();
    }

    default String queryToRawExportQuery(Query query, String filename) throws QueryMalformedException {
        if (query.getQuery().contains(";")) {
            log.trace("Remove ending ; from statement [{}]", query.getQuery());
            query.setQuery(query.getQuery().substring(0, query.getQuery().indexOf(";")));
        }
        /* insert the FOR SYSTEM_TIME ... part after the FROM in the query */
        final StringBuilder versionPart = new StringBuilder(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                .append(mariaDbFormatter.format(query.getExecution()))
                .append("' ");
        final Pattern pattern = Pattern.compile("from `?[a-zA-Z0-9_]+`?", Pattern.CASE_INSENSITIVE) /* https://mariadb.com/kb/en/columnstore-naming-conventions/ */;
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
        return statement.toString();
    }

    default InsertTableRawQuery tableCsvDtoToRawInsertQuery(Table table, TableCsvDto data)
            throws TableMalformedException, ImageNotSupportedException {
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
        final StringBuilder query = new StringBuilder("INSERT INTO `")
                .append(table.getInternalName())
                .append("` (")
                .append(table.getColumns()
                        .stream()
                        .filter(column -> !column.getAutoGenerated())
                        .map(column -> "`" + column.getInternalName() + "`")
                        .collect(Collectors.joining(",")))
                .append(") VALUES (?1);");
        /* map all columns that are non-auto generated */
        final Collection<Object> values = table.getColumns()
                .stream()
                .filter(c -> !c.getAutoGenerated())
                .map(c -> {
                    final Optional<Map.Entry<String, Object>> tuple = data.getData()
                            .entrySet()
                            .stream()
                            .filter(d -> d.getKey().equals(c.getInternalName()))
                            .findFirst();
                    if (tuple.isEmpty()) {
                        log.error("Tuple contains columns names that are not present in the database");
                        log.debug("tuple column names are {}", data.getData().keySet());
                        return null;
                    }
                    return dataColumnToObject(tuple.get()
                            .getValue(), c);
                })
                .collect(Collectors.toList());
        log.trace("raw insert query: [{}] with data {}", query, values);
        return InsertTableRawQuery.builder()
                .query(query.toString())
                .data(values)
                .build();
    }

    default String tableCsvDtoToRawDeleteQuery(Table table, TableCsvDeleteDto data)
            throws TableMalformedException, ImageNotSupportedException {
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
        final StringBuilder query = new StringBuilder("DELETE FROM `")
                .append(table.getInternalName())
                .append("` WHERE ");
        final int[] idx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> query.append(idx[0] == 0 ? "" : ", ")
                        .append("`")
                        .append(key)
                        .append("` = ?")
                        .append(idx[0]++));
        /* debug */
        log.trace("raw delete query: [{}] with data {}", query, data.getKeys().values());
        return query.toString();
    }

    default InsertTableRawQuery tableCsvDtoToRawUpdateQuery(Table table, TableCsvUpdateDto data)
            throws TableMalformedException, ImageNotSupportedException {
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
        final StringBuilder query = new StringBuilder("UPDATE `")
                .append(table.getInternalName())
                .append("` SET ");
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    query.append(idx[0] == 0 ? "" : ", ")
                            .append("`")
                            .append(key)
                            .append("` = ?")
                            .append(idx[0]);
                    idx[0]++;
                });
        query.append(" WHERE ");
        final int[] jdx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> {
                    query.append(jdx[0] == 0 ? "" : ", ")
                            .append("`")
                            .append(key)
                            .append("` = '")
                            .append(value)
                            .append("'");
                    jdx[0]++;
                });
        query.append(";");
        /* debug */
        log.trace("raw update query: [{}] with data {}", query, data.getData().values());
        return InsertTableRawQuery.builder()
                .query(query.toString())
                .data(data.getData().values())
                .build();
    }

    default String tableToRawCountAllQuery(Table table, Instant timestamp) throws ImageNotSupportedException {
        /* check image */
        if (!table.getDatabase().getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Image not supported.");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        return "SELECT COUNT(*) FROM `" + nameToInternalName(table.getName()) +
                "` FOR SYSTEM_TIME AS OF TIMESTAMP '" +
                LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna")) +
                "';";
    }

    default String queryToRawTimestampedCountQuery(String query, Database database, Instant timestamp)
            throws ImageNotSupportedException {
        /* param check */
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must be provided");
        }
        query = query.toLowerCase(Locale.ROOT)
                .split("from")[1];
        final StringBuilder sb = new StringBuilder();
        sb.append("select count(*) from");
        if (!query.contains("where")) {
            /* treat join queries as normal queries */
            sb.append(query);
        } else {
            sb.append(query.split("where")[0]);
        }
        if (query.contains("join")) {
            /* put timestamp after "join" and each "on" (but before alias) */
        } else {
            sb.append("FOR SYSTEM_TIME AS OF TIMESTAMP '");
            sb.append(LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna")));
            sb.append("' ");
        }
        if (query.contains("where")) {
            sb.append("where");
            sb.append(query.split("where")[1]);
        }
        sb.append(";");
        /* replace timestamp for join query */
        String statement = sb.toString();
        if (query.contains("join")) {
            statement = statement.replaceFirst("from ([`a-z0-9_]+) ", "from $1 FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna"))
                    + "' ");
            statement = statement.replaceAll("join ([`a-z0-9_]+) ", "join $1 FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna"))
                    + "' ");
        }
        log.debug("mapped raw view-only query [{}]", statement);
        return statement;
    }

    default String queryToRawTimestampedQuery(String query, Database database, Instant timestamp, Long page, Long size)
            throws ImageNotSupportedException, QueryMalformedException {
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
            sb.append(LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna")));
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
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna"))
                    + "' ");
            statement = statement.replaceAll("join ([`a-z0-9_]+) ", "join $1 FOR SYSTEM_TIME AS OF TIMESTAMP '"
                    + LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna"))
                    + "' ");
        }
        log.debug("mapped raw view-only query [{}]", statement);
        return statement;
    }

    default String tableToRawFindAllQuery(Table table, Instant timestamp, Long size, Long page)
            throws ImageNotSupportedException {
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
        final StringBuilder query = new StringBuilder("SELECT ");
        table.getColumns()
                .forEach(column -> query.append(idx[0]++ > 0 ? "," : "")
                        .append("`")
                        .append(column.getInternalName())
                        .append("`"));
        query.append(" FROM `")
                .append(nameToInternalName(table.getName()))
                .append("` FOR SYSTEM_TIME AS OF TIMESTAMP '")
                .append(LocalDateTime.ofInstant(timestamp, ZoneId.of("Europe/Vienna")))
                .append("'");
        if (size != null && page != null) {
            log.trace("pagination size/limit of {}", size);
            query.append(" LIMIT ")
                    .append(size);
            log.trace("pagination page/offset of {}", page);
            query.append(" OFFSET ")
                    .append(page * size)
                    .append(";");

        }
        log.trace("raw select table query: [{}]", query);
        return query.toString();
    }

    default QueryResultDto queryTableToQueryResultDto(List<?> result, Table table) throws DateTimeException {
        final Iterator<?> iterator = result.iterator();
        final List<Map<String, Object>> queryResult = new LinkedList<>();
        while (iterator.hasNext()) {
            /* map the result set to the columns through the stored metadata in the metadata database */
            int[] idx = new int[]{0};
            final Object[] data = (Object[]) iterator.next();
            final Map<String, Object> map = new HashMap<>();
            table.getColumns()
                    .forEach(column -> map.put(column.getInternalName(), dataColumnToObject(data[idx[0]++], column)));
            queryResult.add(map);
        }
        log.info("Selected {} records from table id {}", queryResult.size(), table.getId());
        log.trace("table {} contains {} records", table, queryResult.size());
        return QueryResultDto.builder()
                .result(queryResult)
                .build();
    }

    @Transactional(readOnly = true)
    default String historyRawQuery(Table data) {
        final StringBuilder builder = new StringBuilder("SELECT")
                .append(" IF(`deleted_at` IS NULL, `inserted_at`, `deleted_at`) as `timestamp`")
                .append(", IF(`deleted_at` IS NULL, 'INSERT', 'DELETE') as `event`")
                .append(", COUNT(`inserted_at`) as `total` FROM `hs_")
                .append(data.getInternalName())
                .append("` GROUP BY `inserted_at`, `deleted_at` ORDER BY `timestamp` ASC;");
        log.trace("mapped find all from history view query [{}]", builder);
        return builder.toString();
    }

    @Transactional(readOnly = true)
    default List<TableHistoryDto> resultListToTableHistoryDto(Table table, List<?> resultList) {
        final Iterator<?> iterator = resultList.iterator();
        final List<TableHistoryDto> history = new LinkedList<>();
        while (iterator.hasNext()) {
            final int[] idx = new int[]{0};
            final Map<String, Object> primaryKeys = new HashMap<>();
            final Object[] row = (Object[]) iterator.next();
            history.add(TableHistoryDto.builder()
                    .timestamp(objectToInstant(row[idx[0]++]))
                    .event(String.valueOf(row[idx[0]++]))
                    .total(Long.parseLong(String.valueOf(row[idx[0]++])))
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
    default String generateInsertFromTemporaryTableSQL(Table table) {
        final StringBuilder generateTable = new StringBuilder("INSERT INTO `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("` SELECT ");
        for (TableColumn tc : table.getColumns()) {
            generateTable.append("`");
            generateTable.append(tc.getInternalName()).append("`,");
        }

        generateTable.deleteCharAt(generateTable.length() - 1);
        generateTable.append(" FROM `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("_temporary`");

        generateTable.append(" ON DUPLICATE KEY UPDATE ");
        for (TableColumn tc : table.getColumns())
            generateTable.append("`")
                    .append(tc.getInternalName())
                    .append("`")
                    .append("=")
                    .append("VALUES(`")
                    .append(tc.getInternalName())
                    .append("`),");
        generateTable.deleteCharAt(generateTable.length() - 1);
        generateTable.append(";");
        log.debug("Insert Query: {}", generateTable);
        return generateTable.toString();
    }

    default Instant objectToInstant(Object data) {
        if (data == null) {
            return null;
        }
        final String str = String.valueOf(data);
        log.trace("mapping string {} to instant", str);
        final Instant out;
        if (str.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
            /* e.g. 2022-07-04T11:31:46Z */
            log.trace("format ISO 8601 matches, e.g. 2022-07-04T11:31:46Z, want to parse '{}'", str);
            out = Instant.parse(str);
        } else {
            /* e.g. 2022-06-20 09:08:13.416567, 2022-06-20 09:08:13.41656 */
            final String timestamp = str.substring(0, 19);
            log.trace("want to parse '{}'", timestamp);
            out = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH))
                    .atZone(ZoneId.of("UTC"))
                    .toInstant();
        }
        log.trace("instant is {}", out);
        return out;
    }

}
