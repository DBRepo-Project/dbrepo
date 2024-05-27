package at.tuwien.mapper;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKeyReference;
import at.tuwien.entities.database.table.constraints.foreignKey.ReferenceType;
import at.tuwien.entities.database.table.constraints.primaryKey.PrimaryKey;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import org.mapstruct.*;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {IdentifierMapper.class, UserMapper.class}, imports = {Collectors.class, LinkedList.class})
public interface TableMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableMapper.class);

    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(target = "name", expression = "java(data.getName())"),
            @Mapping(target = "internalName", expression = "java(data.getInternalName())")
    })
    TableBriefDto tableToTableBriefDto(Table data);

    TableBriefDto tableDtoToTableBriefDto(TableDto data);

    /* keep */
    default UniqueDto uniqueToUniqueDto(Unique data) {
        return UniqueDto.builder()
                .uid(data.getUid())
                .name("uk")
                .columns(data.getColumns().stream().map(this::tableColumnToColumnDto).toList())
                .table(tableToTableBriefDto(data.getTable()))
                .build();
    }

    @Mappings({
            @Mapping(target = "name", expression = "java(data.getName())"),
            @Mapping(target = "internalName", expression = "java(data.getInternalName())"),
            @Mapping(target = "queueName", expression = "java(data.getQueueName())"),
            @Mapping(target = "routingKey", expression = "java(\"dbrepo.\" + data.getTdbid() + \".\" + data.getId())"),
            @Mapping(target = "isPublic", source = "database.isPublic"),
    })
    TableDto tableToTableDto(Table data);

    @Mappings({
            @Mapping(target = "table", ignore = true),
            @Mapping(target = "referencedTable", ignore = true),
    })
    ForeignKeyDto foreignKeyToForeignKeyDto(ForeignKey foreignKey);

    @Mappings({
            @Mapping(target = "foreignKey", ignore = true),
    })
    ForeignKeyReferenceDto foreignKeyReferenceToForeignKeyReferenceDto(ForeignKeyReference foreignKeyReference);

    @Mappings({
            @Mapping(target = "table", ignore = true)
    })
    TableColumn columnDtoToTableColumn(ColumnDto columnDto);

    @Mappings({
            @Mapping(target = "table", ignore = true)
    })
    Unique uniqueDtoToUnique(UniqueDto data);

    @Mappings({
            @Mapping(target = "constraints.primaryKey", expression = "java(new LinkedList<>())"),
            @Mapping(target = "ownedBy", source = "owner.id"),
    })
    Table tableDtoToTable(TableDto data);

    /* keep */
    default Constraints constraintsCreateDtoToConstraints(ConstraintsCreateDto data, Database database, Table table) {
        final int[] idx = new int[]{0, 0};
        final Constraints constrains = Constraints.builder()
                .checks(data.getChecks())
                .uniques(data.getUniques()
                        .stream()
                        .map(uniqueList -> Unique.builder()
                                .name("uk_" + table.getInternalName() + "_" + idx[0]++)
                                .table(table)
                                .columns(table.getColumns()
                                        .stream()
                                        .filter(ukColumn -> uniqueList.stream().map(this::nameToInternalName).toList().contains(nameToInternalName(ukColumn.getInternalName())))
                                        .toList())
                                .build())
                        .toList())
                .foreignKeys(data.getForeignKeys()
                        .stream()
                        .map(fk -> {
                            final Optional<Table> optional = database.getTables()
                                    .stream()
                                    .filter(t -> t.getInternalName().equals(fk.getReferencedTable()))
                                    .findFirst();
                            if (optional.isEmpty()) {
                                log.error("Failed to find foreign key referenced table {} in tables: {}", fk.getReferencedTable(), database.getTables().stream().map(Table::getInternalName).toList());
                                throw new IllegalArgumentException("Failed to find foreign key referenced table");
                            }
                            return ForeignKey.builder()
                                    .name("fk_" + table.getInternalName() + "_" + idx[1]++)
                                    .referencedTable(optional.get())
                                    .references(fk.getReferencedColumns()
                                            .stream()
                                            .map(c -> {
                                                final Optional<TableColumn> column = table.getColumns()
                                                        .stream()
                                                        .filter(cc -> cc.getInternalName().equals(c))
                                                        .findFirst();
                                                if (column.isEmpty()) {
                                                    log.error("Failed to find foreign key column {} in columns: {}", c, table.getColumns().stream().map(TableColumn::getInternalName).toList());
                                                    throw new IllegalArgumentException("Failed to find foreign key column");
                                                }
                                                final Optional<TableColumn> referencedColumn = database.getTables()
                                                        .stream()
                                                        .filter(t -> t.getInternalName().equals(fk.getReferencedTable()))
                                                        .map(Table::getColumns)
                                                        .flatMap(List::stream)
                                                        .filter(cc -> cc.getInternalName().equals(c))
                                                        .findFirst();
                                                if (referencedColumn.isEmpty()) {
                                                    log.error("Failed to find foreign key referenced column {} in referenced columns: {}", c, database.getTables().stream().filter(t -> t.getInternalName().equals(fk.getReferencedTable())).map(Table::getColumns).flatMap(List::stream).map(TableColumn::getInternalName).toList());
                                                    throw new IllegalArgumentException("Failed to find foreign key referenced column");
                                                }
                                                return ForeignKeyReference.builder()
                                                        .column(column.get())
                                                        .referencedColumn(referencedColumn.get())
                                                        .foreignKey(null) // set later
                                                        .build();
                                            })
                                            .toList())
                                    .onDelete(ReferenceType.CASCADE)
                                    .onUpdate(ReferenceType.CASCADE)
                                    .build();
                        })
                        .toList())
                .primaryKey(data.getPrimaryKey()
                        .stream()
                        .map(pk -> {
                            final Optional<TableColumn> optional = table.getColumns()
                                    .stream()
                                    .filter(c -> c.getInternalName().equals(nameToInternalName(pk)))
                                    .findFirst();
                            if (optional.isEmpty()) {
                                log.error("Failed to find primary key column '{}' in columns: {}", pk, table.getColumns().stream().map(TableColumn::getInternalName).toList());
                                throw new IllegalArgumentException("Failed to find primary key column");
                            }
                            return PrimaryKey.builder()
                                    .table(table)
                                    .column(optional.get())
                                    .build();
                        })
                        .toList())
                .build();
        constrains.getForeignKeys()
                .forEach(fk -> fk.getReferences()
                        .forEach(r -> r.setForeignKey(fk)));
        return constrains;
    }

    /* keep */
    @Mappings({
            @Mapping(target = "tableId", source = "table.id"),
            @Mapping(target = "databaseId", source = "table.database.id"),
            @Mapping(target = "isPublic", source = "table.database.isPublic"),
            @Mapping(target = "description", source = "description"),
            @Mapping(target = "table.columns", ignore = true),
            @Mapping(target = "table.constraints", ignore = true),
            @Mapping(target = "views", ignore = true)
    })
    ColumnDto tableColumnToColumnDto(TableColumn data);

    @Named("internalMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.isEmpty()) {
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
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "columnType", source = "data.type"),
            @Mapping(target = "isNullAllowed", source = "data.nullAllowed"),
            @Mapping(target = "name", source = "data.name"),
            @Mapping(target = "autoGenerated", expression = "java(false)"),
            @Mapping(target = "internalName", expression = "java(nameToInternalName(data.getName()))"),
    })
    TableColumn columnCreateDtoToTableColumn(ColumnCreateDto data, ContainerImage image);

}
