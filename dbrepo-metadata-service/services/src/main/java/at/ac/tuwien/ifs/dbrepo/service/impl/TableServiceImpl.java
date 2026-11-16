package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintAddDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnEnum;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnSet;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.check.CheckConstraint;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKeyReference;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.CheckConstraintRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.ColumnDependencyRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.ForeignKeyRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.UniqueRepository;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    private final RabbitConfig rabbitConfig;
    private final MetadataMapper metadataMapper;
    private final DataServiceGateway dataServiceGateway;
    private final DatabaseRepository databaseRepository;
    private final ColumnDependencyRepository columnDependencyRepository;
    private final CheckConstraintRepository checkConstraintRepository;
    private final ForeignKeyRepository foreignKeyRepository;
    private final UniqueRepository uniqueRepository;
    private final SearchServiceGateway searchServiceGateway;
    private final DatabaseCacheRepository databaseCacheRepository;

    @Autowired
    public TableServiceImpl(RabbitConfig rabbitConfig, MetadataMapper metadataMapper,
                            DataServiceGateway dataServiceGateway, DatabaseRepository databaseRepository,
                            ColumnDependencyRepository columnDependencyRepository,
                            CheckConstraintRepository checkConstraintRepository,
                            ForeignKeyRepository foreignKeyRepository,
                            UniqueRepository uniqueRepository,
                            SearchServiceGateway searchServiceGateway,
                            DatabaseCacheRepository databaseCacheRepository) {
        this.rabbitConfig = rabbitConfig;
        this.metadataMapper = metadataMapper;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.columnDependencyRepository = columnDependencyRepository;
        this.checkConstraintRepository = checkConstraintRepository;
        this.foreignKeyRepository = foreignKeyRepository;
        this.uniqueRepository = uniqueRepository;
        this.searchServiceGateway = searchServiceGateway;
        this.databaseCacheRepository = databaseCacheRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Table findById(Database database, UUID tableId) throws TableNotFoundException {
        final Optional<Table> table = database.getTables()
                .stream()
                .filter(t -> t.getId().equals(tableId))
                .findFirst();
        if (table.isEmpty()) {
            log.error("Failed to find table with id {}", tableId);
            throw new TableNotFoundException("Failed to find table with id " + tableId);
        }
        return table.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Table findByName(Database database, String internalName) throws TableNotFoundException {
        final Optional<Table> table = database.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(internalName))
                .findFirst();
        if (table.isEmpty()) {
            log.error("Failed to find table with internal name {}", internalName);
            throw new TableNotFoundException("Failed to find table with internal name " + internalName);
        }
        return table.get();
    }

    @Override
    @Transactional
    public Table createTable(Database database, CreateTableDto data, Principal principal) throws DataServiceException,
            DataServiceConnectionException, TableNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException {
        final Table table = Table.builder()
                .isVersioned(true)
                .name(data.getName())
                .internalName(metadataMapper.nameToInternalName(data.getName()))
                .description(data.getDescription())
                .queueName(rabbitConfig.getQueueName())
                .tdbid(database.getId())
                .database(database)
                .ownedBy(AuthUtil.getUsername(principal))
                .numRows(0L)
                .dataLength(0L)
                .isPublic(data.getIsPublic())
                .isSchemaPublic(data.getIsSchemaPublic())
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .build();
        try {
            /* set the ordinal position for the columns */
            final int[] idx = new int[]{0};
            for (int i = 0; i < data.getColumns().size(); i++) {
                final CreateTableColumnDto c = data.getColumns().get(i);
                final TableColumn column = metadataMapper.columnCreateDtoToTableColumn(c, database.getContainer().getImage());
                if (c.getEnums() != null) {
                    column.setEnums(c.getEnums()
                            .stream()
                            .map(e -> ColumnEnum.builder()
                                    .column(column)
                                    .value(e)
                                    .build())
                            .toList());
                }
                if (c.getSets() != null) {
                    column.setSets(c.getSets()
                            .stream()
                            .map(e -> ColumnSet.builder()
                                    .column(column)
                                    .value(e)
                                    .build())
                            .toList());
                }
                column.setOrdinalPosition(idx[0]++);
                column.setTable(table);
                column.setConceptUri(c.getConceptUri());
                column.setUnitUri(c.getUnitUri());
                table.getColumns()
                        .add(column);
            }
            /* set constraints */
            table.setConstraints(metadataMapper.constraintsCreateDtoToConstraints(data.getConstraints(), database, table));
        } catch (IllegalArgumentException e) {
            throw new MalformedException(e);
        }
        log.debug("map constraints: {}", table.getConstraints());
        for (int i = 0; i < data.getConstraints().getUniques().size(); i++) {
            if (data.getConstraints().getUniques().get(i).size() != table.getConstraints().getUniques().get(i).getColumns().size()) {
                log.error("Failed to create table: some unique constraint(s) reference non-existing table columns: {}", data.getConstraints().getUniques().get(i));
                log.debug("payload uniques: {}", data.getConstraints().getUniques());
                log.debug("mapped table uniques: {}", table.getConstraints().getUniques().stream().map(u -> List.of(u.getColumns().stream().map(TableColumn::getInternalName).toList())).toList());
                throw new MalformedException("Failed to create table: some unique constraint(s) reference non-existing table columns");
            }
        }
        database.getTables()
                .add(table);
        /* create in data service */
        dataServiceGateway.createTable(database.getId(), data);
        /* update in metadata database */
        final Database entity = databaseRepository.save(database);
        final Optional<Table> optional = entity.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(table.getInternalName()))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find created table");
            throw new TableNotFoundException("Failed to find created table");
        }
        /* update cache */
        databaseCacheRepository.deleteById(table.getDatabase().getId());
        /* update in search service */
        searchServiceGateway.update(entity);
        log.info("Created table with id {}", optional.get().getId());
        return optional.get();
    }

    @Override
    @Transactional
    public Table addConstraint(Table table, ConstraintAddDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException {
        final String type = normalizeConstraintType(data.getType());
        return switch (type) {
            case "unique" -> addUniqueConstraint(table, data);
            case "foreign_key" -> addForeignKeyConstraint(table, data);
            case "check" -> addCheckConstraint(table, data);
            case "not_null" -> addNotNullConstraint(table, data);
            default -> throw new MalformedException("Unsupported constraint type: " + data.getType());
        };
    }

    @Override
    @Transactional
    public Table deleteConstraint(Table table, ConstraintDeleteDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException {
        final String type = normalizeConstraintType(data.getType());
        return switch (type) {
            case "unique" -> deleteUniqueConstraint(table, data);
            case "foreign_key" -> deleteForeignKeyConstraint(table, data);
            case "check" -> deleteCheckConstraint(table, data);
            case "not_null" -> deleteNotNullConstraint(table, data);
            default -> throw new MalformedException("Unsupported constraint type: " + data.getType());
        };
    }

    @Override
    @Transactional
    public void deleteTable(Table table) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceConnectionException, SearchServiceException {
        /* remove metadata constraints that would otherwise keep the table columns alive */
        final int foreignKeyReferences = foreignKeyRepository.deleteReferencesByTableId(table.getId());
        final int foreignKeys = foreignKeyRepository.deleteByTableId(table.getId());
        log.debug("Deleted {} foreign key references and {} foreign keys for table {}", foreignKeyReferences,
                foreignKeys, table.getId());
        removeForeignKeysFromGraph(table);
        final String tableId = table.getId().toString();
        final int concepts = columnDependencyRepository.deleteConceptsByTableId(tableId);
        final int units = columnDependencyRepository.deleteUnitsByTableId(tableId);
        final int enums = columnDependencyRepository.deleteEnumsByTableId(tableId);
        final int sets = columnDependencyRepository.deleteSetsByTableId(tableId);
        log.debug("Deleted {} concept(s), {} unit(s), {} enum(s) and {} set(s) for table {}", concepts, units,
                enums, sets, table.getId());
        /* delete at data service */
        try {
            dataServiceGateway.deleteTable(table.getDatabase().getId(), table.getId());
        } catch (TableNotFoundException e) {
            /* ignore */
        }
        /* update in metadata database */
        table.getDatabase()
                .getTables()
                .remove(table);
        final Database database = databaseRepository.save(table.getDatabase());
        /* update cache */
        databaseCacheRepository.deleteById(table.getDatabase().getId());
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Deleted table with id {}", table.getId());
    }

    private Table addUniqueConstraint(Table table, ConstraintAddDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        requireColumns(data.getColumns(), "Unique constraint columns are required");
        final String name = nextConstraintName(table, "uk");
        dataServiceGateway.addTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintAddDto.builder()
                .type("unique")
                .name(name)
                .columns(data.getColumns())
                .build());
        table.getConstraints().getUniques().add(Unique.builder()
                .name(name)
                .table(table)
                .columns(data.getColumns().stream()
                        .map(column -> findColumn(table, metadataMapper.nameToInternalName(column)))
                        .collect(Collectors.toCollection(LinkedList::new)))
                .build());
        return saveConstraintMetadata(table);
    }

    private Table addForeignKeyConstraint(Table table, ConstraintAddDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        requireColumns(data.getColumns(), "Foreign key columns are required");
        requireColumns(data.getReferencedColumns(), "Foreign key referenced columns are required");
        if (data.getReferencedTable() == null || data.getReferencedTable().isBlank()) {
            throw new MalformedException("Foreign key referenced table is required");
        }
        final Table referencedTable = findReferencedTable(table.getDatabase(), data.getReferencedTable());
        final String name = nextConstraintName(table, "fk");
        dataServiceGateway.addTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintAddDto.builder()
                .type("foreign_key")
                .name(name)
                .columns(data.getColumns())
                .referencedTable(data.getReferencedTable())
                .referencedColumns(data.getReferencedColumns())
                .onUpdate(data.getOnUpdate())
                .onDelete(data.getOnDelete())
                .build());
        final ForeignKey foreignKey = ForeignKey.builder()
                .name(name)
                .table(table)
                .referencedTable(referencedTable)
                .onUpdate(metadataMapper.referenceTypeDtoToReferenceType(data.getOnUpdate()))
                .onDelete(metadataMapper.referenceTypeDtoToReferenceType(data.getOnDelete()))
                .references(new LinkedList<>())
                .build();
        final ForeignKey managed = foreignKeyRepository.save(foreignKey);
        for (int i = 0; i < data.getColumns().size(); i++) {
            managed.getReferences().add(ForeignKeyReference.builder()
                    .foreignKey(managed)
                    .column(findColumn(table, metadataMapper.nameToInternalName(data.getColumns().get(i))))
                    .referencedColumn(findColumn(referencedTable,
                            metadataMapper.nameToInternalName(data.getReferencedColumns().get(i))))
                    .build());
        }
        table.getConstraints().getForeignKeys().add(managed);
        return saveConstraintMetadata(table);
    }

    private Table addCheckConstraint(Table table, ConstraintAddDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        if (data.getCheck() == null || data.getCheck().isBlank()) {
            throw new MalformedException("Check constraint expression is required");
        }
        dataServiceGateway.addTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintAddDto.builder()
                .type("check")
                .check(data.getCheck())
                .build());
        table.getConstraints().getChecks().add(CheckConstraint.builder()
                .table(table)
                .check(data.getCheck())
                .build());
        return saveConstraintMetadata(table);
    }

    private Table addNotNullConstraint(Table table, ConstraintAddDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final TableColumn column;
        if (data.getId() != null) {
            column = findColumn(table, data.getId());
        } else {
            if (data.getColumn() == null || data.getColumn().isBlank()) {
                throw new MalformedException("Column is required");
            }
            column = findColumn(table, metadataMapper.nameToInternalName(data.getColumn()));
        }
        dataServiceGateway.addTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintAddDto.builder()
                .type("not_null")
                .column(column.getInternalName())
                .build());
        column.setIsNullAllowed(false);
        return saveConstraintMetadata(table);
    }

    private Table deleteUniqueConstraint(Table table, ConstraintDeleteDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final Unique unique = findUnique(table, requiredId(data));
        dataServiceGateway.deleteTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintDeleteDto.builder()
                .type("unique")
                .name(unique.getName())
                .build());
        table.getConstraints().getUniques().removeIf(item -> unique.getId().equals(item.getId()));
        uniqueRepository.deleteById(unique.getId());
        return saveConstraintMetadata(table);
    }

    private Table deleteForeignKeyConstraint(Table table, ConstraintDeleteDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final ForeignKey foreignKey = findForeignKey(table, requiredId(data));
        dataServiceGateway.deleteTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintDeleteDto.builder()
                .type("foreign_key")
                .name(foreignKey.getName())
                .build());
        table.getConstraints().getForeignKeys().removeIf(item -> foreignKey.getId().equals(item.getId()));
        foreignKeyRepository.deleteReferencesByForeignKeyId(foreignKey.getId());
        foreignKeyRepository.deleteById(foreignKey.getId());
        return saveConstraintMetadata(table);
    }

    private Table deleteCheckConstraint(Table table, ConstraintDeleteDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final CheckConstraint check = findCheck(table, requiredId(data));
        dataServiceGateway.deleteTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintDeleteDto.builder()
                .type("check")
                .check(check.getCheck())
                .build());
        table.getConstraints().getChecks().removeIf(item -> check.getId().equals(item.getId()));
        checkConstraintRepository.deleteById(check.getId());
        return saveConstraintMetadata(table);
    }

    private Table deleteNotNullConstraint(Table table, ConstraintDeleteDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final TableColumn column = findColumn(table, requiredId(data));
        dataServiceGateway.deleteTableConstraint(table.getDatabase().getId(), table.getId(), ConstraintDeleteDto.builder()
                .type("not_null")
                .column(column.getInternalName())
                .build());
        column.setIsNullAllowed(true);
        return saveConstraintMetadata(table);
    }

    private Table saveConstraintMetadata(Table table) throws DatabaseNotFoundException, TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final Database database = databaseRepository.save(table.getDatabase());
        databaseCacheRepository.deleteById(database.getId());
        searchServiceGateway.update(database);
        return findById(database, table.getId());
    }

    private String normalizeConstraintType(String type) throws MalformedException {
        if (type == null) {
            throw new MalformedException("Constraint type is required");
        }
        return type.toLowerCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
    }

    private UUID requiredId(ConstraintDeleteDto data) throws MalformedException {
        if (data.getId() == null) {
            throw new MalformedException("Constraint id is required");
        }
        return data.getId();
    }

    private void requireColumns(List<String> columns, String message) throws MalformedException {
        if (columns == null || columns.isEmpty()) {
            throw new MalformedException(message);
        }
    }

    private String nextConstraintName(Table table, String prefix) {
        final String base = prefix + "_" + table.getInternalName() + "_";
        final Set<String> existing = new HashSet<>();
        table.getConstraints().getUniques().stream().map(Unique::getName).forEach(existing::add);
        table.getConstraints().getForeignKeys().stream().map(ForeignKey::getName).forEach(existing::add);
        int idx = 0;
        while (existing.contains(base + idx)) {
            idx++;
        }
        return base + idx;
    }

    private Unique findUnique(Table table, UUID id) throws MalformedException {
        return table.getConstraints().getUniques().stream()
                .filter(unique -> id.equals(unique.getId()))
                .findFirst()
                .orElseThrow(() -> new MalformedException("Failed to find unique constraint"));
    }

    private ForeignKey findForeignKey(Table table, UUID id) throws MalformedException {
        return table.getConstraints().getForeignKeys().stream()
                .filter(foreignKey -> id.equals(foreignKey.getId()))
                .findFirst()
                .orElseThrow(() -> new MalformedException("Failed to find foreign key constraint"));
    }

    private CheckConstraint findCheck(Table table, UUID id) throws MalformedException {
        return table.getConstraints().getChecks().stream()
                .filter(check -> id.equals(check.getId()))
                .findFirst()
                .orElseThrow(() -> new MalformedException("Failed to find check constraint"));
    }

    private void removeForeignKeysFromGraph(Table table) {
        if (table.getDatabase() == null || table.getDatabase().getTables() == null) {
            return;
        }
        table.getDatabase()
                .getTables()
                .stream()
                .filter(t -> t.getConstraints() != null)
                .filter(t -> t.getConstraints().getForeignKeys() != null)
                .forEach(t -> t.getConstraints()
                        .getForeignKeys()
                        .removeIf(foreignKey -> referencesTable(foreignKey, table)));
    }

    private boolean referencesTable(ForeignKey foreignKey, Table table) {
        return foreignKey.getTable() != null && table.getId().equals(foreignKey.getTable().getId())
                || foreignKey.getReferencedTable() != null && table.getId().equals(foreignKey.getReferencedTable().getId());
    }

    @Transactional
    @Override
    public Table updateTable(Table table, TableUpdateDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* update at data service */
        dataServiceGateway.updateTable(table.getDatabase().getId(), table.getId(), data);
        /* update in metadata database */
        final Optional<Table> optional = table.getDatabase()
                .getTables()
                .stream()
                .filter(t -> t.getId().equals(table.getId()))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find table with id {}", table.getId());
            throw new TableNotFoundException("Failed to find table with id " + table.getId());
        }
        final Table tableEntity = optional.get();
        tableEntity.setIsPublic(data.getIsPublic());
        tableEntity.setIsSchemaPublic(data.getIsSchemaPublic());
        tableEntity.setDescription(data.getDescription());
        final Database database = databaseRepository.save(table.getDatabase());
        /* update the cache */
        databaseCacheRepository.deleteById(database.getId());
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Updated table with id {}", table.getId());
        return tableEntity;
    }

    @Override
    @Transactional
    public void update(TableColumn column, ColumnSemanticsUpdateDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException, OntologyNotFoundException,
            SemanticEntityNotFoundException {
        column.setConceptUri(data.getConceptUri());
        column.setUnitUri(data.getUnitUri());
        column.setDescription(data.getDescription());
        /* update in metadata database */
        final Table table = column.getTable();
        table.getColumns()
                .set(table.getColumns().indexOf(column), column);
        final Database database = databaseRepository.save(table.getDatabase());
        /* update in open search service */
        searchServiceGateway.update(database);
        log.info("Updated table column semantics");
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumn findColumnById(Table table, UUID columnId) throws MalformedException {
        final Optional<TableColumn> optional = table.getColumns()
                .stream()
                .filter(c -> c.getId().equals(columnId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column with id {}", columnId);
            throw new MalformedException("Failed to find column in metadata database");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void updateStatistics(Table table) throws SearchServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, MalformedException, TableNotFoundException,
            DataServiceException, DataServiceConnectionException {
        final TableStatisticDto statistic = dataServiceGateway.getTableStatistics(table.getTdbid(), table.getId());
        if (statistic == null) {
            log.warn("Table statistic is empty (no column can be analysed), skip.");
            return;
        }
        table.setNumRows(statistic.getTotalRows());
        table.setDataLength(statistic.getDataLength());
        table.setAvgRowLength(statistic.getAvgRowLength());
        table.setMaxDataLength(statistic.getMaxDataLength());
        for (ColumnStatisticDto columnStatistic : statistic.getColumns()) {
            final Optional<TableColumn> optional = table.getColumns().stream().filter(c -> c.getInternalName().equals(columnStatistic.getName())).findFirst();
            if (optional.isEmpty()) {
                log.error("Failed to assign table column statistic: column {} does not exist in table {}.{}", columnStatistic.getName(), table.getDatabase().getInternalName(), table.getInternalName());
                throw new MalformedException("Failed to assign table column statistic: column does not exist");
            }
            final TableColumn column = optional.get();
            column.setMean(columnStatistic.getMean());
            column.setMedian(columnStatistic.getMedian());
            column.setMin(columnStatistic.getMin());
            column.setMax(columnStatistic.getMax());
            column.setStdDev(columnStatistic.getStdDev());
        }
        /* update in metadata database */
        final Database database = table.getDatabase();
        database.getTables()
                .set(database.getTables().indexOf(table), table);
        databaseRepository.save(database);
        /* update in open search service */
        searchServiceGateway.update(database);
        log.info("Updated statistics for the table and {} column(s)", table.getColumns().size());
    }

    private Table findReferencedTable(Database database, String internalName) {
        final String normalized = metadataMapper.nameToInternalName(internalName);
        return database.getTables()
                .stream()
                .filter(table -> table.getInternalName().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find foreign key referenced table"));
    }

    private TableColumn findColumn(Table table, String internalName) {
        return table.getColumns()
                .stream()
                .filter(column -> column.getInternalName().equals(internalName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find constraint column"));
    }

    private TableColumn findColumn(Table table, UUID id) {
        return table.getColumns()
                .stream()
                .filter(column -> id.equals(column.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Failed to find constraint column"));
    }

}
