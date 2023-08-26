package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableCreateRawQuery;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.mdb.TableColumnRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.SemanticService;
import at.tuwien.service.TableService;
import at.tuwien.service.UserService;
import at.tuwien.utils.UserUtil;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class TableServiceImpl extends HibernateConnector implements TableService {

    private final TableMapper tableMapper;
    private final QueryMapper queryMapper;
    private final UserService userService;
    private final DatabaseService databaseService;
    private final SemanticService semanticService;
    private final TableRepository tableRepository;
    private final TableIdxRepository tableIdxRepository;
    private final TableColumnRepository tableColumnRepository;
    private final TableColumnIdxRepository tableColumnIdxRepository;

    @Autowired
    public TableServiceImpl(TableMapper tableMapper, QueryMapper queryMapper, UserService userService,
                            SemanticService semanticService, TableRepository tableRepository, DatabaseService databaseService,
                            TableIdxRepository tableIdxRepository, TableColumnRepository tableColumnRepository,
                            TableColumnIdxRepository tableColumnIdxRepository) {
        this.tableMapper = tableMapper;
        this.queryMapper = queryMapper;
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
    public Table find(Long databaseId, Long tableId) throws DatabaseNotFoundException, TableNotFoundException {
        final Optional<Table> table = tableRepository.find(databaseId, tableId);
        if (table.isEmpty()) {
            log.error("Failed to find table with id {} in database with id {}", tableId, databaseId);
            throw new TableNotFoundException("Failed to find table with id " + tableId + " in database with id " + databaseId);
        }
        return table.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findAll() {
        return tableRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableHistoryDto> findHistory(Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, QueryStoreException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = find(databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        /* use jpa to select one */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.historyRawQuery(connection, table);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultListToTableHistoryDto(resultSet);
        } catch (SQLException e) {
            log.error("Failed to map table history: {}", e.getMessage());
            throw new QueryStoreException("Failed to map table history: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
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
        /* delete in metadata database */
        tableRepository.delete(table);
        log.info("Deleted table with id {} in metadata database", table.getId());
        /* delete in open search database */
        tableIdxRepository.delete(tableMapper.tableToTableDto(table));
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
        /* checks */
        if (createDto.getName().isBlank()) {
            log.error("Failed create table: table name is blank");
            throw new TableMalformedException("Failed create table: table name is blank");
        }
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
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
            query = tableMapper.tableToCreateTableRawQuery(connection, createDto);
            if (query.getGenerated()) {
                /* in case the id column needs to be generated, we need to generate the sequence too */
                final PreparedStatement preparedStatement10 = tableMapper.tableToCreateSequenceRawQuery(connection, database, createDto);
                preparedStatement10.executeUpdate();
                log.debug("created id sequence");
            }
            final PreparedStatement preparedStatement11 = query.getPreparedStatement();
            preparedStatement11.executeUpdate();
        } catch (Exception e) {
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
        final Table entity = tableMapper.tableCreateDtoToTable(createDto);
        entity.setInternalName(tableMapper.nameToInternalName(entity.getName()));
        entity.setQueueName(database.getExchangeName() + "." + entity.getInternalName());
        entity.setRoutingKey(entity.getQueueName());
        entity.setIsVersioned(true);
        entity.setTdbid(databaseId);
        entity.setDatabase(database);
        entity.setConstraints(null);
        entity.setCreatedBy(UserUtil.getId(principal));
        entity.setOwnedBy(UserUtil.getId(principal));
        /* map columns */
        entity.setColumns(createDto.getColumns()
                .stream()
                .map(column -> tableMapper.columnCreateDtoToTableColumn(column, database.getContainer().getImage()))
                .map(column -> tableMapper.tableColumnToTableColumn(entity, column, query))
                .toList());
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
        /* create in metadata database */
        final Table table = tableRepository.save(entity);
        log.info("Created table with id {} in metadata database", table.getId());
        /* create in open search database */
        tableIdxRepository.save(tableMapper.tableToTableDto(table));
        log.info("Created table with id {} in open search database", table.getId());
        final List<ColumnDto> columns = table.getColumns()
                .stream()
                .map(tableMapper::tableColumnToColumnDto)
                .toList();
        tableColumnIdxRepository.saveAll(columns);
        log.info("Created table columns with table id {} in open search database", table.getId());
        return table;
    }

    @Override
    @Transactional
    public TableColumn update(Long databaseId, Long tableId, Long columnId,
                              ColumnSemanticsUpdateDto updateDto, String authorization)
            throws TableNotFoundException, DatabaseNotFoundException, TableMalformedException,
            SemanticEntityNotFoundException, QueryMalformedException {
        final Table table = findById(databaseId, tableId);
        final TableColumn column = findColumn(table, columnId);
        /* assign */
        if (updateDto.getUnitUri() != null) {
            try {
                column.setUnit(semanticService.findUnit(updateDto.getUnitUri()));
            } catch (UnitNotFoundException e) {
                log.warn("Unit with uri {} not found in metadata database", updateDto.getUnitUri());
                column.setUnit(semanticService.saveUnit(updateDto.getUnitUri()));
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
                column.setConcept(semanticService.saveConcept(updateDto.getConceptUri()));
            }
        } else {
            column.setConcept(null);
            log.debug("remove ColumnConcept of column, column={}", column);
        }
        /* update in metadata database */
        final TableColumn out = tableColumnRepository.save(column);
        log.info("Updated table column with id {} of table with id {} in metadata database", columnId, tableId);
        /* update in open search database */
        table.getColumns().set(table.getColumns().indexOf(column), column);
        tableIdxRepository.save(tableMapper.tableToTableDto(table));
        tableColumnIdxRepository.save(tableMapper.tableColumnToColumnDto(column));
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
