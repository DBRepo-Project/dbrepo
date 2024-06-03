package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.config.RabbitConfig;
import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.mapper.SparqlMapper;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.service.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;

@Log4j2
@Service
public class TableServiceImpl implements TableService {

    private final UserService userService;
    private final UnitService unitService;
    private final SparqlMapper ontologyMapper;
    private final RabbitConfig rabbitConfig;
    private final EntityService entityService;
    private final ConceptService conceptService;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final DataServiceGateway dataServiceGateway;
    private final DatabaseRepository databaseRepository;
    private final SearchServiceGateway searchServiceGateway;

    @Autowired
    public TableServiceImpl(UserService userService, UnitService unitService, SparqlMapper ontologyMapper, 
                            RabbitConfig rabbitConfig, EntityService entityService, ConceptService conceptService, 
                            MetadataMapper metadataMapper, DatabaseService databaseService, 
                            DataServiceGateway dataServiceGateway, DatabaseRepository databaseRepository, 
                            SearchServiceGateway searchServiceGateway) {
        this.userService = userService;
        this.unitService = unitService;
        this.ontologyMapper = ontologyMapper;
        this.rabbitConfig = rabbitConfig;
        this.entityService = entityService;
        this.conceptService = conceptService;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.searchServiceGateway = searchServiceGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public Table findById(Long databaseId, Long tableId) throws TableNotFoundException,
            DatabaseNotFoundException {
        final Optional<Table> table = databaseService.findById(databaseId)
                .getTables()
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
    public Table findByName(Long databaseId, String internalName) throws TableNotFoundException,
            DatabaseNotFoundException {
        final Optional<Table> table = databaseService.findById(databaseId)
                .getTables()
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
    public Table createTable(Database database, TableCreateDto data, Principal principal) throws ServiceException,
            ServiceConnectionException, UserNotFoundException, TableNotFoundException, DatabaseNotFoundException,
            TableExistsException, SearchServiceException, SearchServiceConnectionException, MalformedException {
        final User owner = userService.findByUsername(principal.getName());
        /* check */
        if (data.getConstraints().getPrimaryKey().isEmpty()) {
            final List<ColumnCreateDto> columns = new LinkedList<>();
            columns.add(ColumnCreateDto.builder()
                    .name("id")
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .build());
            columns.addAll(data.getColumns());
            data.setNeedSequence(true);
            data.setColumns(columns);
            data.getConstraints()
                    .setPrimaryKey(Set.of("id"));
            log.debug("no primary key provided: generate primary key column with sequence");
        } else {
            log.trace("primary key provided: no column with sequence needed");
            data.setNeedSequence(false);
        }
        /* map table */
        final Table table = Table.builder()
                .isVersioned(true)
                .name(data.getName())
                .internalName(metadataMapper.nameToInternalName(data.getName()))
                .description(data.getDescription())
                .queueName(rabbitConfig.getQueueName())
                .tdbid(database.getId())
                .database(database)
                .createdBy(owner.getId())
                .creator(owner)
                .ownedBy(owner.getId())
                .owner(owner)
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .build();
        try {
            /* set the ordinal position for the columns */
            table.getColumns()
                    .addAll(data.getColumns()
                            .stream()
                            .map(c -> {
                                final TableColumn column = metadataMapper.columnCreateDtoToTableColumn(c, database.getContainer().getImage());
                                if (data.isNeedSequence() && column.getName().equals("id")) {
                                    column.setAutoGenerated(true);
                                }
                                if (List.of(TableColumnType.TIME, TableColumnType.TIMESTAMP, TableColumnType.DATE, TableColumnType.DATETIME).contains(column.getColumnType())) {
                                    final Optional<ContainerImageDate> optional = database.getContainer()
                                            .getImage()
                                            .getDateFormats()
                                            .stream()
                                            .filter(df -> df.getId().equals(c.getDfid()))
                                            .findFirst();
                                    if (optional.isEmpty()) {
                                        log.error("Failed to find date format with id {} in metadata database", c.getDfid());
                                        throw new IllegalArgumentException("Failed to find date format in metadata database");
                                    }
                                    column.setDateFormat(optional.get());
                                    log.debug("column is of temporal type: added date format with id {}", column.getDateFormat().getId());
                                }
                                return column;
                            })
                            .toList());
            /* set constraints */
            table.setConstraints(metadataMapper.constraintsCreateDtoToConstraints(data.getConstraints(), database, table));
        } catch (IllegalArgumentException e) {
            throw new MalformedException(e);
        }
        log.debug("map constraints: {}", table.getConstraints());
        for (int i = 0; i < data.getConstraints().getUniques().size(); i++) {
            if (data.getConstraints().getUniques().get(i).size() != table.getConstraints().getUniques().get(i).getColumns().size()) {
                log.error("Failed to create table: some unique constraint(s) reference non-existing table columns: {}", data.getConstraints().getUniques().get(i));
                throw new MalformedException("Failed to create table: some unique constraint(s) reference non-existing table columns");
            }
        }
        int[] idx = {0};
        table.getColumns()
                .forEach(column -> {
                    column.setTable(table);
                    column.setOrdinalPosition(idx[0]++);
                });
        database.getTables().add(table);
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
        log.info("Created table with id {}", optional.get().getId());
        return optional.get();
    }

    @Override
    @Transactional
    public void deleteTable(Table table) throws ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, TableNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* delete at data service */
        dataServiceGateway.deleteTable(table.getDatabase().getId(), table.getId());
        /* update in metadata database */
        table.getDatabase().getTables().remove(table);
        final Database database = databaseRepository.save(table.getDatabase());
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Deleted table with id {}", table.getId());
    }

    @Override
    @Transactional
    public TableColumn update(TableColumn column, ColumnSemanticsUpdateDto data) throws ServiceException,
            ServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
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
    public TableColumn findColumnById(Table table, Long columnId) throws MalformedException {
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
    @Transactional(readOnly = true)
    public TableColumn findColumnByName(Table table, String name) throws MalformedException {
        final Optional<TableColumn> optional = table.getColumns()
                .stream()
                .filter(c -> c.getInternalName().equals(name))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column with name {} in table with name {}", name, table.getInternalName());
            throw new MalformedException("Failed to find column in metadata database");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void updateStatistics(Table table, TableStatisticDto data) throws MalformedException, SearchServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException {
        final List<String> notFound = data.getColumns()
                .keySet()
                .stream()
                .filter(key -> table.getColumns().stream().noneMatch(c -> c.getInternalName().equals(key)))
                .toList();
        if (!notFound.isEmpty()) {
            log.error("Failed to update statistics: column(s) not found: {}", notFound);
            throw new MalformedException("Failed to update statistics: column(s) not found");
        }
        table.getColumns()
                .forEach(column -> {
                    if (!data.getColumns().containsKey(column.getInternalName())) {
                        return;
                    }
                    final ColumnStatisticDto statistic = data.getColumns().get(column.getInternalName());
                    column.setMean(statistic.getMean());
                    column.setMedian(statistic.getMedian());
                    column.setMin(statistic.getMin());
                    column.setMax(statistic.getMax());
                });
        /* update in metadata database */
        final Database database = table.getDatabase();
        database.getTables()
                .set(database.getTables().indexOf(table), table);
        /* update in open search service */
        searchServiceGateway.update(database);
        log.info("Updated table statistics");
    }

}
