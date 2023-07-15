package at.tuwien.mapper;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ColumnTypeMalformedException;
import org.mapstruct.Mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring")
public interface QueryMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryMapper.class);

    default String findAllViewsQuery(Database database) {
        return "SELECT TABLE_NAME as internal_name FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + database.getInternalName() + "' AND TABLE_TYPE = 'VIEW';";
    }

    default String findAllTablesQuery(Database database) {
        return "SELECT TABLE_NAME as internal_name FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + database.getInternalName() + "' AND TABLE_TYPE = 'TABLE';";
    }

    default String findColumnsForTable(Database database, String name) {
        return "SHOW COLUMNS FROM `" + name + "`;";
    }

    default List<ViewBriefDto> resultSetToViewDtoList(ResultSet result) throws SQLException {
        log.trace("mapping result list to view list result, result={}", result);
        final List<ViewBriefDto> views = new LinkedList<>();
        while (result.next()) {
            final ViewBriefDto view = ViewBriefDto.builder()
                    .name(result.getString("internal_name"))
                    .internalName(result.getString("internal_name"))
                    .build();
            views.add(view);
        }
        log.trace("mapped result list {} to view list {}", result, views);
        return views;
    }

    default List<TableBriefDto> resultSetToTableDtoList(ResultSet result) throws SQLException {
        log.trace("mapping result list to table list result, result={}", result);
        final List<TableBriefDto> tables = new LinkedList<>();
        while (result.next()) {
            final TableBriefDto table = TableBriefDto.builder()
                    .name(result.getString("internal_name"))
                    .internalName(result.getString("internal_name"))
                    .build();
            tables.add(table);
        }
        log.trace("mapped result list {} to table list {}", result, tables);
        return tables;
    }

    default ViewDto resultSetToViewDto(ResultSet result, String name) throws SQLException,
            ColumnTypeMalformedException {
        log.trace("mapping result list to view result, result={}", result);
        final ViewDto view = ViewDto.builder()
                .name(name)
                .internalName(name)
                .build();
        final List<ColumnDto> columns = new LinkedList<>();
        while (result.next()) {
            final ColumnDto column = ColumnDto.builder()
                    .name(result.getString("Field"))
                    .internalName(result.getString("Field"))
                    .columnType(typetoColumnTypeDto(result.getString("Type")))
                    .isNullAllowed(nullToBoolean(result.getString("Null")))
                    .build();
            columns.add(column);
        }
        view.setColumns(columns);
        log.trace("mapped result list {} to view {}", result, view);
        return view;
    }

    default TableDto resultSetToTableDto(ResultSet result, String name) throws SQLException,
            ColumnTypeMalformedException {
        log.trace("mapping result list to table result, result={}", result);
        final TableDto table = TableDto.builder()
                .name(name)
                .internalName(name)
                .build();
        final List<ColumnDto> columns = new LinkedList<>();
        while (result.next()) {
            final ColumnDto column = ColumnDto.builder()
                    .name(result.getString("Field"))
                    .internalName(result.getString("Field"))
                    .columnType(typetoColumnTypeDto(result.getString("Type")))
                    .isNullAllowed(nullToBoolean(result.getString("Null")))
                    .build();
            columns.add(column);
        }
        table.setColumns(columns);
        log.trace("mapped result list {} to table {}", result, table);
        return table;
    }

    default ColumnTypeDto typetoColumnTypeDto(String data) throws ColumnTypeMalformedException {
        if (data.toUpperCase().startsWith("TINYINT(1)")) {
            /* boolean in MySQL */
            return ColumnTypeDto.BOOL;
        }
        final Matcher matcher = Pattern.compile("([A-Z]+)")
                .matcher(data.toUpperCase());
        if (!matcher.find()) {
            log.error("Failed to map type: does not match expected format");
            throw new ColumnTypeMalformedException("Failed to map type: does not match expected format");
        }
        final String type = matcher.group(1);
        try {
            return ColumnTypeDto.valueOf(type);
        } catch (IllegalArgumentException e) {
            if (type.startsWith("TINYINT")) {
                /* boolean in MySQL */
                return ColumnTypeDto.BOOL;
            } else if (type.startsWith("BOOL")) {
                /* boolean */
                return ColumnTypeDto.BOOL;
            } else if (type.startsWith("DOUBLE")) {
                /* double precision */
                return ColumnTypeDto.DOUBLE;
            } else if (type.startsWith("INT")) {
                /* integer synonym */
                return ColumnTypeDto.INT;
            } else if (type.startsWith("DEC")) {
                /* decimal synonym */
                return ColumnTypeDto.DECIMAL;
            } else if (type.startsWith("ENUM")) {
                return ColumnTypeDto.ENUM;
            } else if (type.startsWith("SET")) {
                return ColumnTypeDto.SET;
            }
        }
        log.error("Failed to map data {} and type {}", data, type);
        throw new ColumnTypeMalformedException("Failed to map data " + data + " and type " + type);
    }

    default Boolean nullToBoolean(String data) {
        return data.equalsIgnoreCase("yes");
    }

}
