package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintAddDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnEnum;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnSet;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKeyReference;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    private final RabbitConfig rabbitConfig;
    private final MetadataMapper metadataMapper;
    private final DataServiceGateway dataServiceGateway;
    private final DatabaseRepository databaseRepository;
    private final ColumnDependencyRepository columnDependencyRepository;
    private final ForeignKeyRepository foreignKeyRepository;
    private final UniqueRepository uniqueRepository;
    private final SearchServiceGateway searchServiceGateway;
    private final DatabaseCacheRepository databaseCacheRepository;

    @Autowired
    public TableServiceImpl(RabbitConfig rabbitConfig, MetadataMapper metadataMapper,
                            DataServiceGateway dataServiceGateway, DatabaseRepository databaseRepository,
                            ColumnDependencyRepository columnDependencyRepository,
                            ForeignKeyRepository foreignKeyRepository,
                            UniqueRepository uniqueRepository,
                            SearchServiceGateway searchServiceGateway,
                            DatabaseCacheRepository databaseCacheRepository) {
        this.rabbitConfig = rabbitConfig;
        this.metadataMapper = metadataMapper;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.columnDependencyRepository = columnDependencyRepository;
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
    public Table syncTableSchema(Database database, Table table) throws DataServiceException,
            DataServiceConnectionException, SearchServiceConnectionException, SearchServiceException,
            DatabaseNotFoundException, TableNotFoundException, MalformedException {
        final TableDto inspected = dataServiceGateway.getTableSchema(database.getId(), table.getId());
        if (inspected == null) {
            log.warn("Table schema inspection returned no result for table with id {}, skip schema sync.", table.getId());
            return table;
        }
        return syncConstraintMetadata(table, inspected);
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
        return syncTableSchema(database, optional.get());
    }

    @Override
    @Transactional
    public Table addConstraint(Table table, ConstraintAddDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException {
        final TableDto inspected = dataServiceGateway.addTableConstraint(table.getDatabase().getId(), table.getId(), data);
        return syncConstraintMetadata(table, inspected);
    }

    @Override
    @Transactional
    public Table deleteConstraint(Table table, ConstraintDeleteDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException {
        final TableDto inspected = dataServiceGateway.deleteTableConstraint(table.getDatabase().getId(), table.getId(), data);
        return syncConstraintMetadata(table, inspected);
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

    private Table syncConstraintMetadata(Table table, TableDto inspected) throws SearchServiceConnectionException,
            SearchServiceException, DatabaseNotFoundException, TableNotFoundException, MalformedException {
        updateNullableFlags(table, inspected);
        if (table.getConstraints() == null) {
            table.setConstraints(Constraints.builder()
                    .primaryKey(new LinkedList<>())
                    .foreignKeys(new LinkedList<>())
                    .uniques(new LinkedList<>())
                    .checks(new LinkedHashSet<>())
                    .build());
        }
        final ConstraintsDto inspectedConstraints = inspected.getConstraints();
        if (inspectedConstraints == null) {
            table.getConstraints().setChecks(new LinkedHashSet<>());
            table.getConstraints().setUniques(new LinkedList<>());
            table.getConstraints().setForeignKeys(new LinkedList<>());
        } else {
            try {
                table.getConstraints().setChecks(inspectedConstraints.getChecks() == null ?
                        new LinkedHashSet<>() : new LinkedHashSet<>(inspectedConstraints.getChecks()));
                syncUniques(table, inspectedConstraints.getUniques());
                syncForeignKeys(table, inspectedConstraints.getForeignKeys());
            } catch (IllegalArgumentException e) {
                throw new MalformedException(e);
            }
        }
        final Database database = databaseRepository.save(table.getDatabase());
        databaseCacheRepository.deleteById(database.getId());
        searchServiceGateway.update(database);
        return findById(database, table.getId());
    }

    private void updateNullableFlags(Table table, TableDto inspected) {
        if (inspected.getColumns() == null) {
            return;
        }
        final Map<String, ColumnDto> columns = inspected.getColumns()
                .stream()
                .collect(Collectors.toMap(ColumnDto::getInternalName, Function.identity(), (a, b) -> a));
        table.getColumns()
                .forEach(column -> {
                    final ColumnDto inspectedColumn = columns.get(column.getInternalName());
                    if (inspectedColumn != null) {
                        column.setIsNullAllowed(inspectedColumn.getIsNullAllowed());
                    }
                });
    }

    private void syncUniques(Table table, List<UniqueDto> inspectedUniques) throws MalformedException {
        final List<UniqueDto> uniqueDtos = inspectedUniques == null ? List.of() : inspectedUniques;
        final List<Unique> existingUniques = table.getConstraints().getUniques() == null ?
                List.of() : table.getConstraints().getUniques();
        final Map<String, Unique> existing = existingUniques
                .stream()
                .collect(Collectors.toMap(Unique::getName, Function.identity(), (a, b) -> a));
        final Set<String> incomingNames = uniqueDtos.stream()
                .map(UniqueDto::getName)
                .collect(Collectors.toSet());
        final List<UUID> removed = existingUniques
                .stream()
                .filter(unique -> !incomingNames.contains(unique.getName()))
                .map(Unique::getId)
                .filter(Objects::nonNull)
                .toList();
        final List<Unique> uniques = uniqueDtos.stream()
                .map(uniqueDto -> syncUnique(table, existing.get(uniqueDto.getName()), uniqueDto))
                .toList();
        table.getConstraints().setUniques(new LinkedList<>(uniques));
        removed.forEach(uniqueRepository::deleteById);
    }

    private Unique syncUnique(Table table, Unique unique, UniqueDto data) {
        final Unique entity = unique == null ? Unique.builder()
                .name(data.getName())
                .table(table)
                .build() : unique;
        entity.setTable(table);
        entity.setColumns(data.getColumns()
                .stream()
                .map(column -> findColumn(table, column.getInternalName()))
                .collect(Collectors.toCollection(LinkedList::new)));
        return entity;
    }

    private void syncForeignKeys(Table table, List<ForeignKeyDto> inspectedForeignKeys) throws MalformedException {
        final List<ForeignKeyDto> foreignKeyDtos = inspectedForeignKeys == null ? List.of() : inspectedForeignKeys;
        final List<ForeignKey> existingForeignKeys = table.getConstraints().getForeignKeys() == null ?
                List.of() : table.getConstraints().getForeignKeys();
        final Map<String, ForeignKey> existing = existingForeignKeys
                .stream()
                .collect(Collectors.toMap(ForeignKey::getName, Function.identity(), (a, b) -> a));
        final Set<String> incomingNames = foreignKeyDtos.stream()
                .map(ForeignKeyDto::getName)
                .collect(Collectors.toSet());
        final List<UUID> removed = existingForeignKeys
                .stream()
                .filter(foreignKey -> !incomingNames.contains(foreignKey.getName()))
                .map(ForeignKey::getId)
                .filter(Objects::nonNull)
                .toList();
        final List<ForeignKey> foreignKeys = foreignKeyDtos.stream()
                .map(foreignKeyDto -> syncForeignKey(table, existing.get(foreignKeyDto.getName()), foreignKeyDto))
                .toList();
        table.getConstraints().setForeignKeys(new LinkedList<>(foreignKeys));
        removed.forEach(id -> {
            foreignKeyRepository.deleteReferencesByForeignKeyId(id);
            foreignKeyRepository.deleteById(id);
        });
    }

    private ForeignKey syncForeignKey(Table table, ForeignKey foreignKey, ForeignKeyDto data) {
        final Table referencedTable = findReferencedTable(table.getDatabase(), data);
        final ForeignKey entity = foreignKey == null ? ForeignKey.builder()
                .name(data.getName())
                .table(table)
                .build() : foreignKey;
        entity.setTable(table);
        entity.setReferencedTable(referencedTable);
        entity.setOnUpdate(metadataMapper.referenceTypeDtoToReferenceType(data.getOnUpdate()));
        entity.setOnDelete(metadataMapper.referenceTypeDtoToReferenceType(data.getOnDelete()));
        if (entity.getId() != null) {
            foreignKeyRepository.deleteReferencesByForeignKeyId(entity.getId());
        }
        final ForeignKey managed = entity.getId() == null ? foreignKeyRepository.save(entity) : entity;
        final List<ForeignKeyReference> references = data.getReferences()
                .stream()
                .map(reference -> syncForeignKeyReference(managed, table, referencedTable, reference))
                .collect(Collectors.toCollection(LinkedList::new));
        managed.setReferences(references);
        managed.getReferences().forEach(reference -> reference.setForeignKey(managed));
        return managed;
    }

    private ForeignKeyReference syncForeignKeyReference(ForeignKey foreignKey, Table table, Table referencedTable,
                                                       ForeignKeyReferenceDto data) {
        return ForeignKeyReference.builder()
                .foreignKey(foreignKey)
                .column(findColumn(table, data.getColumn().getInternalName()))
                .referencedColumn(findColumn(referencedTable, data.getReferencedColumn().getInternalName()))
                .build();
    }

    private Table findReferencedTable(Database database, ForeignKeyDto data) {
        return database.getTables()
                .stream()
                .filter(table -> (data.getReferencedTable().getId() != null &&
                        table.getId().equals(data.getReferencedTable().getId())) ||
                        table.getInternalName().equals(data.getReferencedTable().getInternalName()))
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

}
