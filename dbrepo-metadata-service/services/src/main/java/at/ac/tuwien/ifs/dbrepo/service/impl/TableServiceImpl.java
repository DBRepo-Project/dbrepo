package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.LocalTableIdDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaTableLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnEnum;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.ColumnSet;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.cache.TableCacheRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.TableRepository;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    private final RabbitConfig rabbitConfig;
    private final MetadataMapper metadataMapper;
    private final DataServiceGateway dataServiceGateway;
    private final DatabaseRepository databaseRepository;
    private final TableRepository tableRepository;
    private final SearchServiceGateway searchServiceGateway;
    private final DatabaseCacheRepository databaseCacheRepository;
    private final TableCacheRepository tableCacheRepository;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    @Autowired
    public TableServiceImpl(RabbitConfig rabbitConfig, MetadataMapper metadataMapper,
                            DataServiceGateway dataServiceGateway, DatabaseRepository databaseRepository,
                            TableRepository tableRepository,
                            SearchServiceGateway searchServiceGateway,
                            DatabaseCacheRepository databaseCacheRepository, TableCacheRepository tableCacheRepository) {
        this.rabbitConfig = rabbitConfig;
        this.metadataMapper = metadataMapper;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.tableRepository = tableRepository;
        this.searchServiceGateway = searchServiceGateway;
        this.databaseCacheRepository = databaseCacheRepository;
        this.tableCacheRepository = tableCacheRepository;
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
        return createTable(database, data, principal, null);
    }

    @Override
    @Transactional
    public Table createTable(Database database, CreateTableDto data, Principal principal, UUID creationId)
            throws DataServiceException, DataServiceConnectionException, TableNotFoundException,
            DatabaseNotFoundException, TableExistsException, SearchServiceException,
            SearchServiceConnectionException, MalformedException {
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
                .replicaUrls(replicaTableLocations(database, data, creationId))
                .creationLocation(data.getCreationLocation() == null ? baseUrl : data.getCreationLocation())
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

    private List<ReplicaTableLocation> replicaTableLocations(Database database, CreateTableDto data, UUID creationId) {
        if (database.getReplicaUrls() == null) {
            return new LinkedList<>();
        }
        return database.getReplicaUrls()
                .stream()
                .map(location -> ReplicaTableLocation.builder()
                        .url(location.getUrl())
                        .replicaTableId(location.getUrl().equals(data.getCreationLocation()) ? creationId : null)
                        .build())
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Table table) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceConnectionException, SearchServiceException {
        // Keep the physical table, history and dependency graph for persisted queries and citations.
        if (table.getArchivedAt() == null) {
            table.setArchivedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        }
        final Database database = databaseRepository.save(table.getDatabase());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidateTableCache(table);
                }
            });
        } else {
            invalidateTableCache(table);
        }
        searchServiceGateway.update(database);
        log.info("Archived table with id {} at {}", table.getId(), table.getArchivedAt());
    }

    private void invalidateTableCache(Table table) {
        databaseCacheRepository.deleteById(table.getDatabase().getId());
        tableCacheRepository.deleteById(table.getId());
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

    @Override
    @Transactional
    public Table updateReplicationUrl(UUID tableId, TableUpdateReplicationUrlDto data) throws TableNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DatabaseNotFoundException,
            MalformedException {
        final Optional<Table> table = tableRepository.findById(tableId);
        if (table.isEmpty()) {
            log.error("Failed to find table with id {}", tableId);
            throw new TableNotFoundException("Failed to find table with id " + tableId);
        }
        final Optional<ReplicaTableLocation> replicaLocation = table.get().getReplicaUrls()
                .stream()
                .filter(location -> location.getUrl().equals(data.getReplicaUrl()))
                .findFirst();
        if (replicaLocation.isEmpty()) {
            log.error("Failed to find replica URL {} for table {}", data.getReplicaUrl(), tableId);
            throw new MalformedException("Failed to find replica URL for table");
        }
        replicaLocation.get()
                .setReplicaTableId(data.getReplicaTableId());
        final Database database = databaseRepository.save(table.get().getDatabase());
        databaseCacheRepository.deleteById(database.getId());
        searchServiceGateway.update(database);
        log.info("Updated replica table id for table {}", tableId);
        return table.get();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalTableIdDto findLocalTableIdByReplicaTableId(UUID replicaTableId) throws TableNotFoundException {
        final Optional<Table> table = tableRepository.findByReplicaTableId(replicaTableId);
        if (table.isEmpty()) {
            log.error("Failed to find table with replica table id {}", replicaTableId);
            throw new TableNotFoundException("Failed to find table with replica table id " + replicaTableId);
        }
        return new LocalTableIdDto(table.get().getId(), replicaTableId);
    }

}
