package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableCreateRawQuery;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.mdb.TableColumnRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import at.tuwien.service.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TableServiceImpl extends HibernateConnector implements TableService {

    private final TableMapper tableMapper;
    private final UserService userService;
    private final DatabaseService databaseService;
    private final SemanticService semanticService;
    private final TableRepository tableRepository;
    private final TableIdxRepository tableIdxRepository;
    private final TableColumnRepository tableColumnRepository;
    private final TableColumnIdxRepository tableColumnIdxRepository;

    @Autowired
    public TableServiceImpl(TableMapper tableMapper, UserService userService, SemanticService semanticService,
                            TableRepository tableRepository, DatabaseService databaseService,
                            TableIdxRepository tableIdxRepository, TableColumnRepository tableColumnRepository,
                            TableColumnIdxRepository tableColumnIdxRepository) {
        this.tableMapper = tableMapper;
        this.userService = userService;
        this.semanticService = semanticService;
        this.tableRepository = tableRepository;
        this.databaseService = databaseService;
        this.tableIdxRepository = tableIdxRepository;
        this.tableColumnRepository = tableColumnRepository;
        this.tableColumnIdxRepository = tableColumnIdxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findAll(Long databaseId) throws DatabaseNotFoundException {
        final Database database = databaseService.find(databaseId);
        final List<Table> tables = tableRepository.findByDatabaseOrderByCreatedDesc(database);
        log.trace("found {} table(s) in database with id {}", tables.size(), databaseId);
        return tables;
    }

    @Override
    @Transactional
    public void deleteTable(Long databaseId, Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = findById(databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
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
        tableRepository.delete(table);
        log.info("Deleted table with id {} in metadata database", table.getId());
        tableIdxRepository.delete(table);
        log.info("Deleted table with id {} in open search database", table.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Table findById(Long databaseId, Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException {
        final Database database = databaseService.find(databaseId);
        final Optional<Table> optional = tableRepository.findByDatabaseAndId(database, tableId);
        if (optional.isEmpty()) {
            log.error("Failed to find table with id {} in metadata database", tableId);
            throw new TableNotFoundException("Table not found");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public Table createTable(Long databaseId, TableCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, DatabaseNotFoundException, TableMalformedException,
            TableNameExistsException, UserNotFoundException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Optional<Table> optional = tableRepository.findByDatabaseAndInternalName(database,
                tableMapper.nameToInternalName(createDto.getName()));
        if (optional.isPresent()) {
            log.error("Table '{}' exists in metadata database", optional.get().getInternalName());
            throw new TableNameExistsException("Table exists in metadata database");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
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
        tmp.setQueueName(database.getExchangeName() + "." + tmp.getInternalName());
        tmp.setRoutingKey(tmp.getQueueName());
        tmp.setTdbid(databaseId);
        tmp.setDatabase(database);
        tmp.setColumns(List.of());
        tmp.setConstraints(null);
        final User creator = userService.findByUsername(principal.getName());
        tmp.setCreator(creator);
        tmp.setOwner(creator);
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
        /* set constraints */
        entity.setConstraints(tableMapper.constraintsCreateDtoToConstraints(tableRepository, entity, createDto.getConstraints()));
        /* create history view */
        final ComboPooledDataSource dataSource1 = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
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
        log.info("Created table with id {} in metadata database", table.getId());
        /* save in database_index - elastic search */
        tableIdxRepository.save(table);
        log.info("Created table with id {} in open search database", table.getId());
        /* save in column_index - elastic search */
        tableColumnIdxRepository.saveAll(table.getColumns());
        log.info("Saved table columns with table id {} in open search database", table.getId());
        return table;
    }

    @Override
    @Transactional
    public TableColumn update(Long databaseId, Long tableId, Long columnId,
                              ColumnSemanticsUpdateDto updateDto, String authorization)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            SemanticEntityNotFoundException {
        final Table table = findById(databaseId, tableId);
        final TableColumn column = findColumn(table, columnId);
        /* assign */
        if (updateDto.getUnitUri() != null) {
            try {
                column.setUnit(semanticService.findUnit(updateDto.getUnitUri()));
            } catch (UnitNotFoundException e) {
                log.warn("Unit with uri {} not found in metadata database", updateDto.getUnitUri());
                column.setUnit(semanticService.saveUnit(updateDto.getUnitUri(), authorization));
            }
        } else {
            column.setUnit(null);
            log.debug("remove unit of column, column={}", column);
        }
        if (updateDto.getConceptUri() != null) {
            try {
                column.setConcept(semanticService.findConcept(updateDto.getConceptUri()));
            } catch (ConceptNotFoundException e) {
                log.warn("Concept with uri {} not found in metadata database", updateDto.getConceptUri());
                column.setConcept(semanticService.saveConcept(updateDto.getConceptUri(), authorization));
            }
        } else {
            column.setConcept(null);
            log.debug("remove ColumnConcept of column, column={}", column);
        }
        final TableColumn out = tableColumnRepository.save(column);
        log.info("Updated table column with id {} of table with id {}", columnId, tableId);
        /* save in database_index - elastic search */
        table.getColumns().set(table.getColumns().indexOf(column), column);
        tableIdxRepository.save(table);
        log.info("Updated table column with id {} of table with id {} in open search database", columnId, tableId);
        /* save in column_index - elastic search */
        tableColumnIdxRepository.save(column);
        log.info("Updated table column with id {} of table with id {} in open search database", columnId, tableId);
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
            throw new TableMalformedException("Failed to find column with id " + columnId);
        }
        return optional.get();
    }

}
