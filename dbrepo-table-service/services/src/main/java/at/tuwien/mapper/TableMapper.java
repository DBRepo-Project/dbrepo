package at.tuwien.mapper;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableCreateRawQuery;
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
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.*;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKeyReference;
import at.tuwien.entities.database.table.constraints.foreignKey.ReferenceType;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import at.tuwien.exception.ImageNotSupportedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.repository.mdb.TableRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TableMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableMapper.class);

    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(target = "name", expression = "java(data.getName())"),
            @Mapping(target = "internalName", expression = "java(data.getInternalName())")
    })
    TableBriefDto tableToTableBriefDto(Table data);

    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(source = "tdbid", target = "databaseId"),
            @Mapping(target = "name", expression = "java(data.getName())"),
            @Mapping(target = "internalName", expression = "java(data.getInternalName())"),
            @Mapping(target = "queueName", expression = "java(data.getQueueName())"),
            @Mapping(target = "routingKey", expression = "java(data.getRoutingKey())"),
            @Mapping(source = "description", target = "description"),
            @Mapping(source = "database.isPublic", target = "isPublic"),
            @Mapping(source = "constraints", target = "constraints"),
    })
    TableDto tableToTableDto(Table data);

    /* keep */
    @Mappings({
            @Mapping(target = "tableId", source = "tid"),
            @Mapping(target = "databaseId", source = "cdbid"),
            @Mapping(target = "isPublic", source = "table.database.isPublic"),
    })
    ColumnDto tableColumnToColumnDto(TableColumn data);

    ConceptDto tableColumnConceptToConceptDto(TableColumnConcept data);

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);

    ColumnTypeDto columnTypeToColumnTypeDto(TableColumnType data);

    @Mappings({
            @Mapping(target = "constraints", ignore = true)
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
                .tid(table.getId())
                .tdbid(table.getTdbid())
                .table(table)
                .columns(columnNameListToTableColumn(table, names))
                .build();
    }

    ReferenceType referenceTypeDtoToReferenceType(ReferenceTypeDto dto);

    default ForeignKey foreignKeyCreateDtoToForeignKey(TableRepository repo, Table table, ForeignKeyCreateDto data) throws TableMalformedException {
        ForeignKey.ForeignKeyBuilder builder = ForeignKey.builder()
                .tid(table.getId())
                .tdbid(table.getTdbid())
                .table(table)
                .onUpdate(referenceTypeDtoToReferenceType(data.getOnUpdate()))
                .onDelete(referenceTypeDtoToReferenceType(data.getOnDelete()));
        Optional<Table> referencedTable = repo.findByTdbidAndInternalName(table.getTdbid(), nameToInternalName(data.getReferencedTable()));

        if (referencedTable.isEmpty()) {
            throw new TableMalformedException("Could not find table referenced in foreign key.");
        }

        builder.rtid(table.getId())
                .rtdbid(table.getTdbid())
                .referencedTable(referencedTable.get());
        List<TableColumn> columns = columnNameListToTableColumn(table, data.getColumns());
        List<TableColumn> referencedColumns = columnNameListToTableColumn(referencedTable.get(), data.getReferencedColumns());

        if (columns.isEmpty()) {
            throw new TableMalformedException("Foreign key does not have any columns.");
        }
        if (columns.size() != referencedColumns.size()) {
            throw new TableMalformedException("There have to be equally as many columns and referenced columns in a foreign key.");
        }

        List<ForeignKeyReference> references = new ArrayList<>();
        ForeignKey foreignKey = builder.references(references).build();

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

        ForeignKeyDto dto = new ForeignKeyDto(
                new ArrayList<>(),
                tableToTableBriefDto(data.getReferencedTable()),
                new ArrayList<>(),
                referenceTypeDtoToReferenceType(data.getOnUpdate()),
                referenceTypeDtoToReferenceType(data.getOnDelete())
        );

        for (ForeignKeyReference reference : data.getReferences()) {
            dto.getColumns().add(tableColumnToColumnDto(reference.getColumn()));
            dto.getReferencedColumns().add(tableColumnToColumnDto(reference.getReferencedColumn()));
        }

        return dto;
    }

    /* keep */
    default List<String> tableColumnEnumListToStringList(List<TableColumnEnum> data) {
        return data.stream()
                .map(TableColumnEnum::getValue)
                .toList();
    }

    /* keep */
    default List<TableColumnEnum> stringListToTableColumnEnumList(List<String> data) {
        return data.stream()
                .map(s -> TableColumnEnum.builder()
                        .value(s)
                        .build())
                .toList();
    }

    /* keep */
    default List<String> tableColumnSetListToStringList(List<TableColumnSet> data) {
        return data.stream()
                .map(TableColumnSet::getValue)
                .toList();
    }

    /* keep */
    default List<TableColumnSet> stringListToTableColumnSetList(List<String> data) {
        return data.stream()
                .map(s -> TableColumnSet.builder()
                        .value(s)
                        .build())
                .toList();
    }

    default Constraints constraintsCreateDtoToConstraints(TableRepository repo, Table table, ConstraintsCreateDto data) throws TableMalformedException {
        if (data == null) {
            return null;
        }

        Constraints.ConstraintsBuilder builder = Constraints.builder();

        if (data.getUniques() != null) {
            List<Unique> uniques = new ArrayList<>();
            for (List<String> columns : data.getUniques()) {
                uniques.add(columnNameListToUnique(table, columns));
            }
            builder.uniques(uniques);
        }
        if (data.getForeignKeys() != null) {
            List<ForeignKey> foreignKeys = new ArrayList<>();
            for (ForeignKeyCreateDto foreignKeyData : data.getForeignKeys()) {
                foreignKeys.add(foreignKeyCreateDtoToForeignKey(repo, table, foreignKeyData));
            }
            builder.foreignKeys(foreignKeys);
        }

        return builder.build();
    }

    @Mappings({
            @Mapping(source = "table.id", target = "tid"),
            @Mapping(source = "table.database.id", target = "cdbid"),
            @Mapping(source = "table", target = "table"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "autoGenerated", expression = "java(data.getInternalName() == \"id\" && query.getGenerated())"),
            @Mapping(source = "data.name", target = "name"),
            @Mapping(source = "data.internalName", target = "internalName"),
            @Mapping(source = "data.created", target = "created"),
            @Mapping(source = "data.dfid", target = "dfid"),
            @Mapping(source = "data.lastModified", target = "lastModified"),
    })
    TableColumn tableColumnToTableColumn(Table table, TableColumn data, TableCreateRawQuery query);

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
        final String name = slug.toLowerCase(Locale.ENGLISH);
        log.trace("mapped name {} to internal name {}", data, name);
        return name;
    }

    @Mappings({
            @Mapping(source = "primaryKey", target = "isPrimaryKey"),
            @Mapping(source = "type", target = "columnType"),
            @Mapping(source = "nullAllowed", target = "isNullAllowed"),
            @Mapping(source = "name", target = "name"),
            @Mapping(target = "internalName", expression = "java(nameToInternalName(data.getName()))"),
    })
    TableColumn columnCreateDtoToTableColumn(ColumnCreateDto data);

    default String columnCreateDtoToPrimaryKeyLengthSpecification(ColumnCreateDto data) {
        if (!data.getPrimaryKey()) {
            throw new IllegalArgumentException("Not a primary key");
        }
        if (EnumSet.of(ColumnTypeDto.BLOB, ColumnTypeDto.TEXT).contains(data.getType())) {
            return "(" + Objects.requireNonNullElse(data.getIndexLength(), 255) + ")";
        }
        return "";
    }

    /**
     * Maps the desired data type to a MySQL string with the default MySQL 8 values for each
     *
     * @param data The column definition.
     * @return The MySQL string.
     */
    default String columnTypeDtoToDataType(ColumnCreateDto data) {
        switch (data.getType()) {
            case CHAR:
                return "CHAR(" + Objects.requireNonNullElse(data.getLength(), 1) + ")";
            case VARCHAR:
                return "TINYINT(" + data.getLength() + ")";
            case BINARY:
                return "BINARY(" + Objects.requireNonNullElse(data.getLength(), 1) + ")";
            case VARBINARY:
                return "VARBINARY(" + Objects.requireNonNullElse(data.getLength(), 255) + ")";
            case ENUM:
                return "ENUM (" + String.join(",", data.getEnums()) + ")";
            case SET:
                return "SET (" + String.join(",", data.getSets()) + ")";
            case BIT:
                return "BIT(" + Objects.requireNonNullElse(data.getLength(), 1) + ")";
        }
        return data.getType().getType().toUpperCase();
    }

    /**
     * Map the table to a drop table query
     * TODO for e.g. postgres image
     *
     * @param data The table
     * @return The drop table query
     */
    default PreparedStatement tableToDropTableRawQuery(Connection connection, Table data) throws ImageNotSupportedException, QueryMalformedException {
        if (!data.getDatabase().getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final StringBuilder statement = new StringBuilder("DROP TABLE `")
                .append(data.getInternalName())
                .append("`;");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("prepared statement {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    /**
     * Map the table to a create table query
     * TODO for e.g. postgres image
     *
     * @param database The database
     * @param data     The table
     * @return The create table query
     */
    default TableCreateRawQuery tableToCreateTableRawQuery(Connection connection, Database database, TableCreateDto data)
            throws ImageNotSupportedException, TableMalformedException, QueryMalformedException {
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (data.getName().isBlank()) {
            log.error("Failed to map create table statement: table name is blank");
            throw new TableMalformedException("Failed to map create table statement");
        }
        final StringBuilder query = new StringBuilder("CREATE TABLE `")
                .append(nameToInternalName(data.getName()))
                .append("` (");
        /* internal checks */
        final boolean primaryColumnExists = data.getColumns()
                .stream()
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
                log.error("Cannot create id column, it already exists");
                throw new TableMalformedException("Cannot create id column");
            }
            final List<ColumnCreateDto> columns = new LinkedList<>();
            columns.add(idColumn);
            columns.addAll(data.getColumns());
            data.setColumns(columns);
        }
        final int[] idx = {0};
        for (ColumnCreateDto column : data.getColumns()) {
            query.append(idx[0]++ > 0 ? ", " : "")
                    .append("`")
                    .append(nameToInternalName(column.getName()))
                    .append("` ")
                    /* data type */
                    .append(columnTypeDtoToDataType(column))
                    /* null expressions */
                    .append(column.getNullAllowed() ? " NULL" : " NOT NULL")
                    /* default expressions */
                    .append(!primaryColumnExists && column.getName().equals(
                            "id") ? " DEFAULT NEXTVAL(`" + tableCreateDtoToSequenceName(data) + "`)" : "");
        }
        /* create primary key index */
        query.append(", PRIMARY KEY (")
                .append(String.join(",", data.getColumns()
                        .stream()
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
                        .forEach(u -> query.append(", ")
                                .append("UNIQUE KEY (`")
                                .append(u.stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                .append("`)"));
            }
            if (data.getConstraints().getForeignKeys() != null) {
                /* create foreign key indices */
                data.getConstraints().getForeignKeys()
                        .forEach(fk -> {
                            query.append(", FOREIGN KEY (`")
                                    .append(fk.getColumns().stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                    .append("`) REFERENCES `")
                                    .append(nameToInternalName(fk.getReferencedTable()))
                                    .append("` (`")
                                    .append(fk.getReferencedColumns().stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                    .append("`)");
                            if (fk.getOnDelete() != null) {
                                query.append(" ON DELETE ").append(fk.getOnDelete());
                            }
                            if (fk.getOnUpdate() != null) {
                                query.append(" ON UPDATE ").append(fk.getOnUpdate());
                            }
                        });
            }
            if (data.getConstraints().getChecks() != null) {
                /* create check constraints */
                data.getConstraints().getChecks()
                        .forEach(ck -> query.append(", ")
                                .append("CHECK (")
                                .append(ck)
                                .append(")"));
            }
        }
        query.append(") WITH SYSTEM VERSIONING;");
        log.trace("create table query built with {} columns and system versioning", data.getColumns().size());
        try {
            final PreparedStatement pstmt = connection.prepareStatement(query.toString());
            log.trace("prepared create table statement {}", query);
            return TableCreateRawQuery.builder()
                    .preparedStatement(pstmt)
                    .generated(!primaryColumnExists)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", query, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default String tableCreateDtoToSequenceName(TableCreateDto data) {
        final String name = "seq_" + nameToInternalName(data.getName()) + "_id";
        log.trace("mapped name {} to internal name {}", data.getName(), name);
        return name;
    }

    default PreparedStatement tableToCreateSequenceRawQuery(Connection connection, Database database, TableCreateDto data)
            throws ImageNotSupportedException, QueryMalformedException {
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final StringBuilder statement = new StringBuilder("CREATE SEQUENCE `")
                .append(tableCreateDtoToSequenceName(data))
                .append("` START WITH 1 INCREMENT BY 1;");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("prepared create sequence statement {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableToDropSequenceRawQuery(Connection connection, Database database, TableCreateDto data)
            throws ImageNotSupportedException, QueryMalformedException {
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final StringBuilder statement = new StringBuilder("DROP SEQUENCE `")
                .append(tableCreateDtoToSequenceName(data))
                .append("`;");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("prepared drop sequence statement {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement tableToCreateHistoryViewRawQuery(Connection connection, Table data) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE VIEW `hs_")
                .append(data.getInternalName())
                .append("` AS SELECT * FROM (SELECT ");
        final int[] idx = new int[]{0};
        data.getColumns()
                .stream()
                .filter(TableColumn::getIsPrimaryKey)
                .forEach(c -> statement.append(idx[0]++ > 0 ? "," : "")
                        .append("`")
                        .append(c.getInternalName())
                        .append("`"));
        statement.append(", ROW_START AS inserted_at, IF(ROW_END > NOW(), NULL, ROW_END) AS deleted_at, COUNT(*) as total FROM `")
                .append(data.getInternalName())
                .append("` FOR SYSTEM_TIME ALL GROUP BY inserted_at, deleted_at ORDER BY deleted_at DESC LIMIT 50) AS v ORDER BY v.inserted_at, v.deleted_at ASC");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("prepared create sequence statement {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

}
