package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaTableLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.repository.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.TableRepository;
import at.ac.tuwien.ifs.dbrepo.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateReplicationUrlDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.LocalTableIdDto;

@Slf4j
@Service
public class TableServiceImpl implements TableService {

    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;

    private final UserService userService;
    private final UnitService unitService;
    private final RabbitConfig rabbitConfig;
    private final EntityService entityService;
    private final ConceptService conceptService;
    private final MetadataMapper metadataMapper;
    private final DataServiceGateway dataServiceGateway;
    private final DatabaseRepository databaseRepository;
    private final SearchServiceGateway searchServiceGateway;
    private final TableRepository tableRepository;

    @Autowired
    public TableServiceImpl(UserService userService, UnitService unitService, RabbitConfig rabbitConfig,
                            EntityService entityService, ConceptService conceptService, MetadataMapper metadataMapper,
                            DataServiceGateway dataServiceGateway, DatabaseRepository databaseRepository,
                            SearchServiceGateway searchServiceGateway, TableRepository tableRepository) {
        this.userService = userService;
        this.unitService = unitService;
        this.rabbitConfig = rabbitConfig;
        this.entityService = entityService;
        this.conceptService = conceptService;
        this.metadataMapper = metadataMapper;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.searchServiceGateway = searchServiceGateway;
        this.tableRepository = tableRepository;
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
            DataServiceConnectionException, UserNotFoundException, TableNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException,
            OntologyNotFoundException, SemanticEntityNotFoundException {
        return createTable(database, data, principal, null);
    }

    @Override
    @Transactional
    public Table createTable(Database database, CreateTableDto data, Principal principal, UUID creationId) throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, TableNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException,
            OntologyNotFoundException, SemanticEntityNotFoundException {
        final User owner = userService.findByUsername(principal.getName());
        /* map table */

        for (ReplicaLocation replica: database.getReplicaUrls()) {
            System.out.println("========");
            System.out.println(replica.getUrl());
            System.out.println(data.getCreationLocation());
            System.out.println(replica.getUrl().equals(data.getCreationLocation()));

            System.out.println("========");
        }

        final Table table = Table.builder()
                .isVersioned(true)
                .name(data.getName())
                .internalName(metadataMapper.nameToInternalName(data.getName()))
                .description(data.getDescription())
                .queueName(rabbitConfig.getQueueName())
                .tdbid(database.getId())
                .database(database)
                .ownedBy(owner.getId())
                .owner(owner)
                .numRows(0L)
                .dataLength(0L)
                .isPublic(data.getIsPublic())
                .isSchemaPublic(data.getIsSchemaPublic())
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .replicaUrls(database.getReplicaUrls().stream()
                        .map(replicaLocation -> ReplicaTableLocation.builder()
                                .url(replicaLocation.getUrl())
                                .replicaTableId(replicaLocation.getUrl().equals(data.getCreationLocation()) ? creationId : null)
                                .build())
                        .collect(Collectors.toList()))
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
                if (c.getUnitUri() != null) {
                    log.trace("column {} has assigned unit uri: {}", column.getInternalName(), c.getUnitUri());
                    TableColumnUnit unit;
                    try {
                        unit = unitService.find(c.getUnitUri());
                    } catch (UnitNotFoundException e) {
                        unit = unitService.create(metadataMapper.entityDtoToTableColumnUnit(entityService.findOneByUri(c.getUnitUri())));
                    }
                    column.setUnit(unit);
                }
                if (c.getConceptUri() != null) {
                    log.trace("column {} has assigned concept uri: {}", column.getInternalName(), c.getConceptUri());
                    TableColumnConcept concept;
                    try {
                        concept = conceptService.find(c.getConceptUri());
                    } catch (ConceptNotFoundException e) {
                        concept = conceptService.create(metadataMapper.entityDtoToTableColumnConcept(entityService.findOneByUri(c.getConceptUri())));
                    }
                    column.setConcept(concept);
                }
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
        /* update in search service */
        searchServiceGateway.update(entity);
        log.info("Created table with id {} and creationId {}", optional.get().getId(), creationId);
        return optional.get();
    }

    @Override
    @Transactional
    public void deleteTable(Table table) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceConnectionException, SearchServiceException {
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
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Deleted table with id {}", table.getId());
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
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Updated table with id {}", table.getId());
        return tableEntity;
    }

    @Override
    @Transactional
    public TableColumn update(TableColumn column, ColumnSemanticsUpdateDto data) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, MalformedException, OntologyNotFoundException,
            SemanticEntityNotFoundException {
        /* assign */
        if (data.getUnitUri() != null) {
            TableColumnUnit unit;
            try {
                unit = unitService.find(data.getUnitUri());
            } catch (UnitNotFoundException e) {
                unit = metadataMapper.entityDtoToTableColumnUnit(entityService.findOneByUri(data.getUnitUri()));
            }
            column.setUnit(unit);
        } else {
            column.setUnit(null);
        }
        if (data.getConceptUri() != null) {
            TableColumnConcept concept;
            try {
                concept = conceptService.find(data.getConceptUri());
            } catch (ConceptNotFoundException e) {
                concept = metadataMapper.entityDtoToTableColumnConcept(entityService.findOneByUri(data.getConceptUri()));
            }
            column.setConcept(concept);
        } else {
            column.setConcept(null);
        }
        /* update in metadata database */
        final Table table = column.getTable();
        table.getColumns()
                .set(table.getColumns().indexOf(column), column);
        final Database database = databaseRepository.save(table.getDatabase());
        /* update in open search service */
        searchServiceGateway.update(database);
        log.info("Updated table column semantics");
        return column;
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
    public Table updateReplicationUrl(UUID tableId, TableUpdateReplicationUrlDto data)
            throws TableNotFoundException, SearchServiceException, SearchServiceConnectionException, DatabaseNotFoundException {
        log.debug("endpoint update replication URL, tableId={}, replicaUrl={}, replicaTableId={}",
                tableId, data.getReplicaUrl(), data.getReplicaTableId());

        // Find the table by ID across all databases
        Table table = null;
        for (Database database : databaseRepository.findAll()) {
            Optional<Table> optionalTable = database.getTables().stream()
                    .filter(t -> t.getId().equals(tableId))
                    .findFirst();
            if (optionalTable.isPresent()) {
                table = optionalTable.get();
                break;
            }
        }

        if (table == null) {
            log.error("Failed to find table with id {}", tableId);
            throw new TableNotFoundException("Failed to find table with id " + tableId);
        }

        // Find the existing ReplicaTableLocation with the matching URL and update its replicaTableId
        boolean found = false;
        for (ReplicaTableLocation replicaLocation : table.getReplicaUrls()) {
            if (replicaLocation.getUrl().equals(data.getReplicaUrl())) {
                replicaLocation.setReplicaTableId(data.getReplicaTableId());
                found = true;
                log.debug("Updated replica table ID for URL: {}", data.getReplicaUrl());
                break;
            }
        }

        if (!found) {
            log.error("Failed to find replication URL: {} for table: {}", data.getReplicaUrl(), tableId);
            throw new IllegalArgumentException("Replication URL not found: " + data.getReplicaUrl());
        }

        /* update in metadata database */
        final Database updatedDatabase = databaseRepository.save(table.getDatabase());

        /* save in search service */
        searchServiceGateway.update(updatedDatabase);
        log.info("Updated replication URL for table with id {} & search database", table.getId());
        return table;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalTableIdDto findLocalTableIdByReplicaTableId(UUID replicaTableId) throws TableNotFoundException {
        log.debug("Finding local table ID by replica table ID: {}", replicaTableId);

        Optional<Table> tableOptional = tableRepository.findByReplicaTableId(replicaTableId);
        
        if (tableOptional.isEmpty()) {
            log.error("Failed to find table with replica table ID {}", replicaTableId);
            throw new TableNotFoundException("Failed to find table with replica table ID " + replicaTableId);
        }

        Table table = tableOptional.get();
        log.info("Found local table ID {} for replica table ID {}", table.getId(), replicaTableId);
        
        return new LocalTableIdDto(table.getId(), replicaTableId);
    }

}
