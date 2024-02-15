package at.tuwien.mapper;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyCreateDto;
import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreignKey.ReferenceTypeDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKeyReference;
import at.tuwien.entities.database.table.constraints.foreignKey.ReferenceType;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import at.tuwien.exception.ImageNotSupportedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableMalformedException;
import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {IdentifierMapper.class, UserMapper.class})
public interface TableMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableMapper.class);

    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(target = "name", expression = "java(data.getName())"),
            @Mapping(target = "internalName", expression = "java(data.getInternalName())")
    })
    TableBriefDto tableToTableBriefDto(Table data);

    @Mappings({
            @Mapping(target = "name", expression = "java(data.getName())"),
            @Mapping(target = "internalName", expression = "java(data.getInternalName())"),
            @Mapping(target = "queueName", expression = "java(data.getQueueName())"),
            @Mapping(target = "routingKey", expression = "java(data.getRoutingKey())"),
            @Mapping(source = "description", target = "description")
    })
    TableDto tableToTableDto(Table data);

    @Mappings({
            @Mapping(target = "table", ignore = true),
    })
    UniqueDto uniqueToUniqueDto(Unique data);

    /* keep */
    @Mappings({
            @Mapping(target = "tableId", source = "table.id"),
            @Mapping(target = "databaseId", source = "table.database.id"),
            @Mapping(target = "isPublic", source = "table.database.isPublic"),
    })
    ColumnDto tableColumnToColumnDto(TableColumn data);

    ConceptDto tableColumnConceptToConceptDto(TableColumnConcept data);

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);

    ColumnTypeDto columnTypeToColumnTypeDto(TableColumnType data);

    @Mappings({
            @Mapping(target = "constraints", ignore = true),
            @Mapping(target = "processedConstraints", expression = "java(false)"),
    })
    Table tableCreateDtoToTable(TableCreateDto data);

    @Mappings({
            @Mapping(source = "label", target = "name")
    })
    TableColumnConcept entityDtoToTableColumnConcept(EntityDto data);

    @Mappings({
            @Mapping(source = "label", target = "name")
    })
    TableColumnUnit entityDtoToTableColumnUnit(EntityDto data);

    default TableColumn columnNameToTableColumn(Table table, String name) throws TableMalformedException {
        String internalName = nameToInternalName(name);
        for (TableColumn column : table.getColumns()) {
            if (column.getInternalName().equals(internalName)) {
                return column;
            }
        }
        throw new TableMalformedException("Could not find column in table.");
    }

    default List<TableColumn> columnNameListToTableColumn(Table table, List<String> names) throws TableMalformedException {
        List<TableColumn> columns = new ArrayList<>();
        for (String name : names) {
            columns.add(columnNameToTableColumn(table, name));
        }
        return columns;
    }

    default List<ColumnDto> uniqueToColumnList(Unique unique) {
        return unique.getColumns().stream().map(this::tableColumnToColumnDto).collect(Collectors.toList());
    }

    default Unique columnNameListToUnique(Table table, List<String> names) throws TableMalformedException {
        return Unique.builder()
                .table(table)
                .name("UK_" + String.join("_", names))
                .columns(columnNameListToTableColumn(table, names))
                .build();
    }

    ReferenceType referenceTypeDtoToReferenceType(ReferenceTypeDto dto);

    @Transactional(readOnly = true)
    default ForeignKey foreignKeyCreateDtoToForeignKey(Table table, ForeignKeyCreateDto data, Integer index) throws TableMalformedException {
        final String referencedTableInternalName = nameToInternalName(data.getReferencedTable());
        final Optional<Table> optional = table.getDatabase()
                .getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(referencedTableInternalName))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find referenced table with internal name {} in database with id {}", referencedTableInternalName, table.getDatabase().getId());
            throw new TableMalformedException("Failed to find referenced table with internal name " + referencedTableInternalName + " in database with id " + table.getDatabase().getId());
        }
        final ForeignKey foreignKey = ForeignKey.builder()
                .name("fk_" + table.getInternalName() + "_" + (index + 1))
                .table(table)
                .onUpdate(referenceTypeDtoToReferenceType(data.getOnUpdate()))
                .onDelete(referenceTypeDtoToReferenceType(data.getOnDelete()))
                .referencedTable(optional.get())
                .build();
        final List<TableColumn> columns = columnNameListToTableColumn(table, data.getColumns());
        final List<TableColumn> referencedColumns = columnNameListToTableColumn(optional.get(), data.getReferencedColumns());
        if (columns.isEmpty()) {
            log.error("Foreign key does not have any referenced columns");
            throw new TableMalformedException("Foreign key does not have any referenced columns");
        }
        if (columns.size() != referencedColumns.size()) {
            log.error("There have to be equally as many columns and referenced columns in a foreign key");
            throw new TableMalformedException("There have to be equally as many columns and referenced columns in a foreign key");
        }
        final List<ForeignKeyReference> references = new ArrayList<>();
        foreignKey.setReferences(references);
        for (int i = 0; i < columns.size(); i++) {
            TableColumn column = columns.get(i);
            TableColumn referencedColumn = referencedColumns.get(i);
            references.add(ForeignKeyReference.builder()
                    .foreignKey(foreignKey)
                    .column(column)
                    .referencedColumn(referencedColumn)
                    .build());
        }
        return foreignKey;
    }

    ReferenceTypeDto referenceTypeDtoToReferenceType(ReferenceType data);

    default ForeignKeyDto foreignKeyCreateDtoToForeignKey(ForeignKey data) {
        if (data == null) {
            return null;
        }
        final ForeignKeyDto foreignKey = ForeignKeyDto.builder()
                .name(data.getName())
                .columns(new LinkedList<>())
                .referencedColumns(new LinkedList<>())
                .referencedTable(tableToTableBriefDto(data.getReferencedTable()))
                .onDelete(referenceTypeDtoToReferenceType(data.getOnDelete()))
                .onUpdate(referenceTypeDtoToReferenceType(data.getOnUpdate()))
                .build();
        for (ForeignKeyReference reference : data.getReferences()) {
            foreignKey.getColumns().add(tableColumnToColumnDto(reference.getColumn()));
            foreignKey.getReferencedColumns().add(tableColumnToColumnDto(reference.getReferencedColumn()));
        }

        return foreignKey;
    }

    @Transactional(readOnly = true)
    default Constraints constraintsCreateDtoToConstraints(Table table, ConstraintsCreateDto data)
            throws TableMalformedException {
        if (data == null) {
            return null;
        }
        final Constraints.ConstraintsBuilder builder = Constraints.builder();
        if (data.getUniques() != null) {
            final List<Unique> uniques = new ArrayList<>();
            for (List<String> columns : data.getUniques()) {
                uniques.add(columnNameListToUnique(table, columns));
            }
            builder.uniques(uniques);
        }
        if (data.getForeignKeys() != null) {
            final List<ForeignKey> foreignKeys = new ArrayList<>();
            for (int i = 0; i < data.getForeignKeys().size(); i++) {
                foreignKeys.add(foreignKeyCreateDtoToForeignKey(table, data.getForeignKeys().get(i), i));
            }
            builder.foreignKeys(foreignKeys);
        }
        return builder.build();
    }

    @Mappings({
            @Mapping(source = "table", target = "table"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "autoGenerated", expression = "java(data.getInternalName() == \"id\" && generatedSequence)"),
            @Mapping(source = "data.name", target = "name"),
            @Mapping(source = "data.internalName", target = "internalName"),
            @Mapping(source = "data.created", target = "created"),
            @Mapping(source = "data.dateFormat", target = "dateFormat"),
            @Mapping(source = "data.lastModified", target = "lastModified"),
    })
    TableColumn tableColumnToTableColumn(Table table, TableColumn data, Boolean generatedSequence);

    @Named("internalMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.length() == 0) {
            return data;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(data).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("_")
                .replaceAll("-", "_");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    @Mappings({
            @Mapping(target = "isPrimaryKey", source = "data.primaryKey"),
            @Mapping(target = "columnType", source = "data.type"),
            @Mapping(target = "isNullAllowed", source = "data.nullAllowed"),
            @Mapping(target = "name", source = "data.name"),
            @Mapping(target = "autoGenerated", expression = "java(false)"),
            @Mapping(target = "internalName", expression = "java(nameToInternalName(data.getName()))"),
            @Mapping(target = "dateFormat", expression = "java(dateFormatIdToContainerImageDate(data.getDfid(), image))"),
    })
    TableColumn columnCreateDtoToTableColumn(ColumnCreateDto data, ContainerImage image);

    default String columnCreateDtoToPrimaryKeyLengthSpecification(ColumnCreateDto data) {
        if (!data.getPrimaryKey()) {
            throw new IllegalArgumentException("Not a primary key");
        }
        if (EnumSet.of(ColumnTypeDto.BLOB, ColumnTypeDto.TEXT).contains(data.getType())) {
            return "(" + Objects.requireNonNullElse(data.getIndexLength(), 255) + ")";
        }
        return "";
    }

    default ContainerImageDate dateFormatIdToContainerImageDate(Long dateFormatId, ContainerImage image) {
        if (dateFormatId == null) {
            return null;
        }
        log.trace("image has {} date formats", image.getDateFormats().size());
        final Optional<ContainerImageDate> optional = image.getDateFormats()
                .stream()
                .filter(i -> dateFormatId.equals(i.getId()))
                .findFirst();
        optional.ifPresentOrElse(containerImageDate -> log.trace("mapped date format to {}", containerImageDate), () -> log.warn("dfid {} was not found in {}", dateFormatId, image.getDateFormats().stream().map(ContainerImageDate::getId).toList()));
        return optional.orElse(null);
    }

    /**
     * Maps the desired data type to a MySQL string with the default MySQL 8 values for each
     *
     * @param data The column definition.
     * @return The MySQL string.
     */
    default String columnTypeDtoToDataType(ColumnCreateDto data) {
        return switch (data.getType()) {
            case CHAR -> "CHAR(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case VARCHAR -> "VARCHAR(" + Objects.requireNonNullElse(data.getSize(), "255") + ")";
            case BINARY -> "BINARY(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case VARBINARY -> "VARBINARY(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case ENUM -> "ENUM(" + String.join(",", data.getEnums().stream().map(e -> ("'" + e + "'")).toList()) + ")";
            case SET -> "SET(" + String.join(",", data.getSets().stream().map(e -> ("'" + e + "'")).toList()) + ")";
            case BIT -> "BIT(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case TINYINT -> "TINYINT(" + Objects.requireNonNullElse(data.getSize(), "10") + ")";
            case SMALLINT -> "SMALLINT(" + Objects.requireNonNullElse(data.getSize(), "10") + ")";
            case MEDIUMINT -> "MEDIUMINT(" + Objects.requireNonNullElse(data.getSize(), "10") + ")";
            case INT -> "INT(" + Objects.requireNonNullElse(data.getSize(), "255") + ")";
            case BIGINT -> "BIGINT(" + Objects.requireNonNullElse(data.getSize(), "255") + ")";
            case FLOAT -> "FLOAT(" + Objects.requireNonNullElse(data.getSize(), "24") + ")";
            case DOUBLE ->
                    "DOUBLE(" + Objects.requireNonNullElse(data.getSize(), "25") + "," + Objects.requireNonNullElse(data.getD(), "0") + ")";
            case DECIMAL ->
                    "DECIMAL(" + Objects.requireNonNullElse(data.getSize(), "10") + "," + Objects.requireNonNullElse(data.getD(), "0") + ")";
            default -> data.getType().getType().toUpperCase();
        };
    }

    /**
     * Map the table to a drop table query
     *
     * @param connection The connection
     * @param data       The table that should be dropped.
     */
    default void tableToDropTableRawQuery(Connection connection, Table data) throws ImageNotSupportedException, QueryMalformedException {
        if (!data.getDatabase().getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final StringBuilder sequence = new StringBuilder();
        if (data.getColumns().stream().anyMatch(TableColumn::getAutoGenerated)) {
            log.debug("table with id {} has sequence generated which needs to be dropped too", data.getId());
            sequence.append("DROP SEQUENCE IF EXISTS `")
                    .append(tableToSequenceName(data))
                    .append("`;");
        }
        final StringBuilder table = new StringBuilder("DROP TABLE IF EXISTS `")
                .append(data.getInternalName())
                .append("`;");
        final StringBuilder view = new StringBuilder("DROP VIEW IF EXISTS `hs_")
                .append(data.getInternalName())
                .append("`;");
        try {
            final Statement statement = connection.createStatement();
            if (!sequence.isEmpty()) {
                statement.execute(sequence.toString());
            }
            statement.execute(table.toString());
            log.trace("mapped drop table statement {}", table);
            statement.execute(view.toString());
            log.trace("mapped drop view statement {}", table);
        } catch (SQLException e) {
            log.error("Failed to drop table or sequence: {}", e.getMessage());
            throw new QueryMalformedException("Failed to drop table or sequence: " + e.getMessage(), e);
        }
    }

    /**
     * Map the table to a create table and eventual create sequence query.
     *
     * @param data The table
     * @return True if a sequence has been generated, false otherwise.
     */
    default Boolean tableToCreateTableRawQuery(Connection connection, TableCreateDto data)
            throws TableMalformedException, QueryMalformedException {
        final StringBuilder sequence = new StringBuilder();
        final StringBuilder table = new StringBuilder("CREATE TABLE `")
                .append(nameToInternalName(data.getName()))
                .append("` (");
        /* internal checks */
        final boolean primaryColumnExists = data.getColumns()
                .stream()
                .filter(c -> Objects.nonNull(c.getPrimaryKey()))
                .anyMatch(ColumnCreateDto::getPrimaryKey);
        /* create columns */
        if (!primaryColumnExists) {
            log.trace("primary key column does not exist");
            final ColumnCreateDto idColumn = ColumnCreateDto.builder()
                    .name("id")
                    .primaryKey(true)
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .build();
            log.trace("attempt to create id column {}", idColumn);
            if (data.getColumns().stream().anyMatch(c -> c.getName().equals("id"))) {
                log.error("Cannot create id column: it already exists");
                throw new TableMalformedException("Cannot create id column: it already exists");
            }
            /* metadata */
            final List<ColumnCreateDto> columns = new LinkedList<>();
            columns.add(idColumn);
            columns.addAll(data.getColumns());
            data.setColumns(columns);
            /* data */
            final String sequenceName = tableCreateDtoToSequenceName(data);
            log.debug("create sequence with name {}", sequenceName);
            sequence.append("CREATE SEQUENCE `")
                    .append(sequenceName)
                    .append("` START WITH 1 INCREMENT BY 1 NOCACHE; ");
        }
        final int[] idx = {0};
        for (ColumnCreateDto column : data.getColumns()) {
            table.append(idx[0]++ > 0 ? ", " : "")
                    .append("`")
                    .append(nameToInternalName(column.getName()))
                    .append("` ")
                    /* data type */
                    .append(columnTypeDtoToDataType(column))
                    /* null expressions */
                    .append(column.getNullAllowed() != null && column.getNullAllowed() ? " NULL" : " NOT NULL")
                    /* default expressions */
                    .append(!primaryColumnExists && column.getName().equals(
                            "id") ? " DEFAULT NEXTVAL(`" + tableCreateDtoToSequenceName(data) + "`)" : "");
        }
        /* create primary key index */
        table.append(", PRIMARY KEY (")
                .append(String.join(",", data.getColumns()
                        .stream()
                        .filter(c -> Objects.nonNull(c.getPrimaryKey()))
                        .filter(ColumnCreateDto::getPrimaryKey)
                        .map(c -> "`" + nameToInternalName(
                                c.getName()) + "`" + columnCreateDtoToPrimaryKeyLengthSpecification(c))
                        .toArray(String[]::new)))
                .append(")");
        if (data.getConstraints() != null) {
            log.trace("constraints are {}", data.getConstraints());
            if (data.getConstraints().getUniques() != null) {
                /* create unique indices */
                data.getConstraints().getUniques()
                        .forEach(u -> table.append(", ")
                                .append("UNIQUE KEY (`")
                                .append(u.stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                .append("`)"));
            }
            if (data.getConstraints().getForeignKeys() != null) {
                /* create foreign key indices */
                data.getConstraints().getForeignKeys()
                        .forEach(fk -> {
                            table.append(", FOREIGN KEY (`")
                                    .append(fk.getColumns().stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                    .append("`) REFERENCES `")
                                    .append(nameToInternalName(fk.getReferencedTable()))
                                    .append("` (`")
                                    .append(fk.getReferencedColumns().stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                    .append("`)");
                            if (fk.getOnDelete() != null) {
                                table.append(" ON DELETE ").append(fk.getOnDelete());
                            }
                            if (fk.getOnUpdate() != null) {
                                table.append(" ON UPDATE ").append(fk.getOnUpdate());
                            }
                        });
            }
            if (data.getConstraints().getChecks() != null) {
                /* create check constraints */
                data.getConstraints().getChecks()
                        .forEach(ck -> table.append(", ")
                                .append("CHECK (")
                                .append(ck)
                                .append(")"));
            }
        }
        table.append(") WITH SYSTEM VERSIONING;");
        log.trace("create table query built with {} columns and system versioning", data.getColumns().size());
        try {
            final Statement statement = connection.createStatement();
            if (!sequence.isEmpty()) {
                log.trace("mapped create sequence statement: {}", sequence);
                statement.execute(sequence.toString());
            }
            log.trace("mapped create table statement: {}", table);
            statement.execute(table.toString());
            return !sequence.isEmpty();
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", table, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default String tableCreateDtoToSequenceName(TableCreateDto data) {
        final String name = "seq_" + nameToInternalName(data.getName()) + "_id";
        log.trace("mapped table name {} to sequence name {}", data.getName(), name);
        return name;
    }

    default String tableToSequenceName(Table data) {
        final String name = "seq_" + data.getInternalName() + "_id";
        log.trace("mapped table to sequence name {}", name);
        return name;
    }

    default PreparedStatement tableToCreateHistoryViewRawQuery(Connection connection, Table data) throws QueryMalformedException {
        final StringBuilder view = new StringBuilder("CREATE VIEW IF NOT EXISTS `hs_")
                .append(data.getInternalName())
                .append("` AS SELECT * FROM (SELECT ROW_START AS inserted_at, IF(ROW_END > NOW(), NULL, ROW_END) AS deleted_at, COUNT(*) as total FROM `")
                .append(data.getInternalName())
                .append("` FOR SYSTEM_TIME ALL GROUP BY inserted_at, deleted_at ORDER BY deleted_at DESC LIMIT 50) AS v ORDER BY v.inserted_at, v.deleted_at ASC");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(view.toString());
            log.trace("prepared create view statement {}", view);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement: {}", e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    @Transactional(readOnly = true)
    default List<Table> resultListToTableList(ResultSet resultSet, Database database) throws SQLException {
        final List<Table> tables = new LinkedList<>();
        while (resultSet.next()) {
            final String tableType = resultSet.getString(2);
            if (!List.of("SYSTEM VERSIONED", "BASE TABLE").contains(tableType)) {
                log.trace("table is not of type system versioned or base table: {}", tableType);
                continue;
            }
            final Table table = Table.builder()
                    .name(resultSet.getString(1))
                    .internalName(resultSet.getString(1))
                    .isVersioned(resultSet.getString(2).equals("SYSTEM VERSIONED"))
                    .numRows(resultSet.getLong(3))
                    .avgRowLength(resultSet.getLong(4))
                    .dataLength(resultSet.getLong(5))
                    .maxDataLength(resultSet.getLong(6))
                    .database(database)
                    .tdbid(database.getId())
                    .queueName("dbrepo")
                    .routingKey("dbrepo." + database.getInternalName() + "." + resultSet.getString(1))
                    .creator(database.getOwner())
                    .createdBy(database.getOwner().getId())
                    .owner(database.getOwner())
                    .ownedBy(database.getOwner().getId())
                    .build();
            if (resultSet.getString(7) != null && !resultSet.getString(7).isEmpty()) {
                table.setCreated(Timestamp.valueOf(resultSet.getString(7))
                        .toInstant());
            }
            if (resultSet.getString(8) != null && !resultSet.getString(8).isEmpty()) {
                table.setLastModified(Timestamp.valueOf(resultSet.getString(8))
                        .toInstant());
            }
            log.trace("mapped result set to table {}", table);
            tables.add(table);
        }
        return tables;
    }

    @Transactional(readOnly = true)
    default List<View> resultListToViewList(ResultSet resultSet, Database database) throws SQLException {
        final List<View> views = new LinkedList<>();
        while (resultSet.next()) {
            final String tableType = resultSet.getString(2);
            if (!tableType.equals("VIEW")) {
                log.trace("table is not of type view: {}", tableType);
                continue;
            }
            final View view = View.builder()
                    .name(resultSet.getString(1))
                    .internalName(resultSet.getString(1))
                    .database(database)
                    .vdbid(database.getId())
                    .createdBy(database.getOwner().getId())
                    .isInitialView(false)
                    .isPublic(database.getIsPublic())
                    .query(resultSet.getString(9))
                    .queryHash(new DigestUtils("SHA-256").digestAsHex(resultSet.getString(9)))
                    .build();
            if (resultSet.getString(7) != null && !resultSet.getString(7).isEmpty()) {
                view.setCreated(Timestamp.valueOf(resultSet.getString(7))
                        .toInstant());
            }
            if (resultSet.getString(8) != null && !resultSet.getString(8).isEmpty()) {
                view.setLastModified(Timestamp.valueOf(resultSet.getString(8))
                        .toInstant());
            }
            log.trace("mapped result set to view {}", view);
            views.add(view);
        }
        return views;
    }

    default Table resultSetTableToObtainedMetadata(ResultSet resultSet,
                                                   Table data,
                                                   ContainerImageDate defaultDateFormat,
                                                   ContainerImageDate defaultTimestampFormat) throws SQLException {
        final List<TableColumn> columns = new LinkedList<>();
        while (resultSet.next()) {
            final TableColumn column = TableColumn.builder()
                    .table(data)
                    .ordinalPosition(resultSet.getInt(1) - 1) /* start at zero */
//                    .default()
                    .autoGenerated(resultSet.getString(2) != null && resultSet.getString(2).startsWith("nextval"))
                    .isNullAllowed(resultSet.getString(3).equals("YES"))
                    .columnType(TableColumnType.valueOf(resultSet.getString(4).toUpperCase()))
                    .d(resultSet.getString(7) != null ? resultSet.getLong(7) : null)
                    .isPrimaryKey(resultSet.getString(9) != null && resultSet.getString(9).equals("PRI"))
                    .name(resultSet.getString(10))
                    .internalName(resultSet.getString(10))
                    .build();
            /* fix boolean and set size for others */
            if (resultSet.getString(8).equalsIgnoreCase("tinyint(1)")) {
                column.setColumnType(TableColumnType.BOOL);
            } else if (resultSet.getString(5) != null) {
                column.setSize(resultSet.getLong(5));
            } else if (resultSet.getString(6) != null) {
                column.setSize(resultSet.getLong(6));
            }
            if (column.getColumnType().equals(TableColumnType.TIMESTAMP) || column.getColumnType().equals(TableColumnType.DATETIME)) {
                column.setDateFormat(defaultTimestampFormat);
            } else if (column.getColumnType().equals(TableColumnType.DATE)) {
                column.setDateFormat(defaultDateFormat);
            }
            log.trace("mapped result set to column {}", column);
            columns.add(column);
        }
        data.setColumns(columns);
        return data;
    }

}
