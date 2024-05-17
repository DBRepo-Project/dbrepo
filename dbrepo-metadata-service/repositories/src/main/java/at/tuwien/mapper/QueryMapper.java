package at.tuwien.mapper;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.ViewColumn;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.mapstruct.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring", imports = {LinkedList.class})
public interface QueryMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryMapper.class);

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
        final List<ViewColumn> allColumns = Stream.of(database.getViews()
                                .stream()
                                .map(View::getColumns)
                                .flatMap(List::stream),
                        database.getTables()
                                .stream()
                                .map(Table::getColumns)
                                .flatMap(List::stream)
                                .map(c -> ViewColumn.builder()
                                        .column(c)
                                        .alias(c.getAlias())
                                        .ordinalPosition(c.getOrdinalPosition())
                                        .build())
                )
                .flatMap(i -> i)
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
            final String columnName = column.getColumnName().replace("`", "");
            final String tableOrView = optional.get().getName().replace("`", "");
            final List<ViewColumn> filteredColumns = allColumns.stream()
                    .filter(c -> (c.getAlias() != null && c.getAlias().equals(columnName)) || c.getColumn().getInternalName().equals(columnName))
                    .toList();
            final Optional<ViewColumn> optionalColumn = filteredColumns.stream()
                    .filter(c -> columnMatches(c, tableOrView))
                    .findFirst();
            if (optionalColumn.isEmpty()) {
                log.error("Failed to find column with name {} of table/view {} in {}", columnName, tableOrView, filteredColumns.stream().map(c -> c.getColumn().getTable().getInternalName() + "." + c.getColumn().getInternalName()).toList());
                throw new JSQLParserException("Failed to find column with name " + columnName + " of table/view " + tableOrView);
            }
            final ViewColumn resultColumn = optionalColumn.get();
            if (item.getAlias() != null) {
                resultColumn.getColumn().setAlias(item.getAlias().getName().replace("`", ""));
            }
            log.trace("found column with internal name {} and alias {}", resultColumn.getColumn().getInternalName(), resultColumn.getAlias());
            columns.add(resultColumn.getColumn());
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
    default boolean columnMatches(ViewColumn column, String tableOrView) {
        if (column.getView() != null && column.getView().getInternalName().equals(tableOrView)) {
            log.trace("view {} found in column table", tableOrView);
            return true;
        }
        if (column.getColumn().getTable().getInternalName().equals(tableOrView)) {
            log.trace("table {} found in column table", tableOrView);
            return true;
        }
        if (column.getColumn().getViews() == null) {
            log.trace("table/view {} not found among column views: empty list", tableOrView);
            return false;
        }
        /* maybe matches one of the other views */
        final boolean found = column.getColumn()
                .getViews()
                .stream()
                .anyMatch(v -> v.getInternalName().equals(tableOrView));
        if (!found) {
            log.trace("table/view {} not found among column views: {}", tableOrView, column.getColumn().getViews().stream().map(View::getInternalName).toList());
        }
        return found;
    }


}
