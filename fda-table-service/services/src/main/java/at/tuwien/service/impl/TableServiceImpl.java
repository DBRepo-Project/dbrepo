package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableCreateRawQuery;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.ConceptRepository;
import at.tuwien.repository.jpa.TableColumnRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.repository.jpa.UnitRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.service.UserService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TableServiceImpl extends HibernateConnector implements TableService {

    private final TableMapper tableMapper;
    private final UserService userService;
    private final UnitRepository unitRepository;
    private final TableRepository tableRepository;
    private final DatabaseService databaseService;
    private final ContainerService containerService;
    private final ConceptRepository conceptRepository;
    private final TableIdxRepository tableIdxRepository;
    private final TableColumnRepository tableColumnRepository;
    private final TableColumnIdxRepository tableColumnIdxRepository;

    @Autowired
    public TableServiceImpl(TableMapper tableMapper, UserService userService, UnitRepository unitRepository,
                            TableRepository tableRepository, DatabaseService databaseService,
                            ContainerService containerService, ConceptRepository conceptRepository,
                            TableIdxRepository tableIdxRepository, TableColumnRepository tableColumnRepository,
                            TableColumnIdxRepository tableColumnIdxRepository) {
        this.tableMapper = tableMapper;
        this.userService = userService;
        this.unitRepository = unitRepository;
        this.tableRepository = tableRepository;
        this.databaseService = databaseService;
        this.containerService = containerService;
        this.conceptRepository = conceptRepository;
        this.tableIdxRepository = tableIdxRepository;
        this.tableColumnRepository = tableColumnRepository;
        this.tableColumnIdxRepository = tableColumnIdxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findAll(Long containerId, Long databaseId) throws DatabaseNotFoundException {
        final Database database = databaseService.find(containerId, databaseId);
        return tableRepository.findByDatabase(database);
    }

    @Override
    @Transactional
    public void deleteTable(Long containerId, Long databaseId, Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, QueryMalformedException, ContainerNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = findById(containerId, databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = tableMapper.tableToDropTableRawQuery(connection, table);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete table {}, reason: {}", table, e.getMessage());
            throw new TableMalformedException("Failed to delete table", e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted table with id {}", table.getId());
        log.trace("deleted table {}", table);
        /* delete in database_index - elastic search */
        tableIdxRepository.deleteById(tableId);
        /* delete in column_index - elastic search */
        table.getColumns()
                .forEach(column -> tableColumnIdxRepository.deleteById(column.getId()));
        log.info("Deleted columns in elastic search with id {}", databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Table findById(Long containerId, Long databaseId, Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException, ContainerNotFoundException {
        final Container container = containerService.find(containerId);
        final Database database = databaseService.find(containerId, databaseId);
        final Optional<Table> optional = tableRepository.findByDatabaseAndId(database, tableId);
        if (optional.isEmpty()) {
            log.error("Failed to find table with id {} in metadata database", tableId);
            throw new TableNotFoundException("Table not found");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public Table createTable(Long containerId, Long databaseId, TableCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, DatabaseNotFoundException, TableMalformedException,
            TableNameExistsException, UserNotFoundException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Optional<Table> optional = tableRepository.findByDatabaseAndInternalName(database,
                tableMapper.nameToInternalName(createDto.getName()));
        if (optional.isPresent()) {
            log.error("Table '{}' exists in metadata database", optional.get().getInternalName());
            throw new TableNameExistsException("Table exists in metadata database");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        final TableCreateRawQuery query;
        try {
            final Connection connection = dataSource.getConnection();
            query = tableMapper.tableToCreateTableRawQuery(connection, database, createDto);
            if (query.getGenerated()) {
                /* in case the id column needs to be generated, we need to generate the sequence too */
                final PreparedStatement preparedStatement10 = tableMapper.tableToCreateSequenceRawQuery(connection, database, createDto);
                preparedStatement10.executeUpdate();
                log.debug("created id sequence");
            }
            final PreparedStatement preparedStatement11 = query.getPreparedStatement();
            preparedStatement11.executeUpdate();
        } catch (SQLException e) {
            try {
                final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement11 = tableMapper.tableToDropSequenceRawQuery(connection, database, createDto);
                preparedStatement11.executeUpdate();
                log.debug("successfully rolled back creation of id sequence");
            } catch (SQLException ex) {
                log.error("Failed to rollback creation of id sequence");
            }
            log.error("Failed to create table, reason: {}", e.getMessage());
            throw new TableMalformedException("Failed to create table", e);
        } finally {
            dataSource.close();
        }
        int[] idx = {0};
        /* map table */
        final Table tmp = tableMapper.tableCreateDtoToTable(createDto);
        tmp.setInternalName(tableMapper.nameToInternalName(tmp.getName()));
        tmp.setQueueName(database.getExchangeName() + "/" + tmp.getInternalName());
        tmp.setRoutingKey(tmp.getQueueName() + "/1");
        tmp.setTdbid(databaseId);
        tmp.setDatabase(database);
        tmp.setColumns(List.of());
        final User creator = userService.findByUsername(principal.getName());
        tmp.setCreator(creator);
        /* save in metadata database */
        final Table entity = tableRepository.save(tmp);
        entity.setColumns(createDto.getColumns()
                .stream()
                .map(tableMapper::columnCreateDtoToTableColumn)
                .map(column -> tableMapper.tableColumnToTableColumn(entity, column, query))
                .collect(Collectors.toList()));
        /* set the ordinal position for the columns */
        entity.getColumns()
                .forEach(column -> {
                    column.setOrdinalPosition(idx[0]++);
                });
        /* create history view */
        final ComboPooledDataSource dataSource1 = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource1.getConnection();
            final PreparedStatement preparedStatement = tableMapper.tableToCreateHistoryViewRawQuery(connection, entity);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("failed to create history view, reason: {}", e.getMessage());
            throw new TableMalformedException("Failed to create history view", e);
        } finally {
            dataSource1.close();
        }
        /* save in metadata database */
        final Table table = tableRepository.save(entity);
        log.info("Created table with id {}", table.getId());
        log.trace("created table {}", table);
        /* save in database_index - elastic search */
        tableIdxRepository.save(tableMapper.tableToTableDto(table));
        /* save in column_index - elastic search */
        tableColumnIdxRepository.saveAll(tableMapper.tableToTableDto(table).getColumns());
        log.info("Saved table with id {} in elastic search", table.getId());
        return table;
    }

    @Override
    @Transactional
    public TableColumn update(Long containerId, Long databaseId, Long tableId, Long columnId,
                              ColumnSemanticsUpdateDto updateDto, Principal principal) throws TableNotFoundException,
            DatabaseNotFoundException, ContainerNotFoundException, TableMalformedException, UnitNotFoundException, ConceptNotFoundException {
        final Table table = findById(containerId, databaseId, tableId);
        final TableColumn column = findColumn(table, columnId);
        /* assign */
        if (updateDto.getUnitUri() != null) {
            final TableColumnUnit unit = findUnit(updateDto.getUnitUri());
            column.setUnit(unit);
            log.debug("update unit of column, unit={}, column={}", unit, column);
        } else {
            column.setUnit(null);
            log.debug("remove unit of column, column={}", column);
        }
        if (updateDto.getConceptUri() != null) {
            final TableColumnConcept concept = findConcept(updateDto.getConceptUri());
            column.setConcept(concept);
            log.debug("update ColumnConcept of column, concept={}, column={}", concept, column);
        } else {
            column.setConcept(null);
            log.debug("remove ColumnConcept of column, column={}", column);
        }
        final TableColumn out = tableColumnRepository.save(column);
        log.info("Updated table column with id {} of table with id {}", columnId, tableId);
        log.debug("updated table column {}", out);
        /* save in database_index - elastic search */
        table.getColumns().set(table.getColumns().indexOf(column), column);
        tableIdxRepository.save(tableMapper.tableToTableDto(table));
        /* save in column_index - elastic search */
        tableColumnIdxRepository.save(tableMapper.tableColumnToColumnDto(out));
        return out;
    }

    /**
     * Finds a column in a given table with column id
     *
     * @param table    The table.
     * @param columnId The column id.
     * @return The column, if successful.
     * @throws TableMalformedException The requested column was not found in the table.
     */
    protected TableColumn findColumn(Table table, Long columnId) throws TableMalformedException {
        final Optional<TableColumn> optional = table.getColumns()
                .stream()
                .filter(c -> c.getId().equals(columnId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column with id {}", columnId);
            throw new TableMalformedException("Failed to find column");
        }
        return optional.get();
    }

    /**
     * Finds a ColumnConcept with given uri
     *
     * @param uri The uri.
     * @return The concept, if successful.
     * @throws ConceptNotFoundException The ColumnConcept was not found in the metadata database.
     */
    protected TableColumnConcept findConcept(String uri) throws ConceptNotFoundException {
        final Optional<TableColumnConcept> optional = conceptRepository.findById(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find ColumnConcept with uri {}", uri);
            throw new ConceptNotFoundException("Failed to find concept");
        }
        return optional.get();
    }

    /**
     * Finds a unit with given uri
     *
     * @param uri The uri.
     * @return The unit, if successful.
     * @throws UnitNotFoundException The unit was not found in the metadata database.
     */
    protected TableColumnUnit findUnit(String uri) throws UnitNotFoundException {
        final Optional<TableColumnUnit> optional = unitRepository.findById(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find unit with uri {}", uri);
            throw new UnitNotFoundException("Failed to find unit");
        }
        return optional.get();
    }

}
