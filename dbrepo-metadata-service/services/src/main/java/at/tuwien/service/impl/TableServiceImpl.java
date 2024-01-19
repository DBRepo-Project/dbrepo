package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.SemanticService;
import at.tuwien.service.TableService;
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

    private final QueryMapper queryMapper;
    private final TableMapper tableMapper;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;
    private final SemanticService semanticService;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public TableServiceImpl(QueryMapper queryMapper, TableMapper tableMapper, DatabaseMapper databaseMapper,
                            DatabaseService databaseService, SemanticService semanticService,
                            DatabaseRepository databaseRepository, DatabaseIdxRepository databaseIdxRepository) {
        this.queryMapper = queryMapper;
        this.tableMapper = tableMapper;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.semanticService = semanticService;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Table find(Long databaseId, Long tableId) throws DatabaseNotFoundException, TableNotFoundException {
        final Optional<Table> table = databaseService.find(databaseId)
                .getTables()
                .stream()
                .filter(t -> t.getId().equals(tableId))
                .findFirst();
        if (table.isEmpty()) {
            log.error("Failed to find table with id {} in metadata database", tableId);
            throw new TableNotFoundException("Failed to find table with id " + tableId + " in metadata database");
        }
        return table.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Table find(Long databaseId, String internalName) throws DatabaseNotFoundException, TableNotFoundException {
        final Optional<Table> table = databaseService.find(databaseId)
                .getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(internalName))
                .findFirst();
        if (table.isEmpty()) {
            log.error("Failed to find table with internal name {} in metadata database", internalName);
            throw new TableNotFoundException("Failed to find table with internal name " + internalName + " in metadata database");
        }
        return table.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findAll() {
        return databaseService.findAll()
                .stream()
                .map(Database::getTables)
                .flatMap(List::stream)
                .distinct()
                .toList();
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
        return databaseService.find(databaseId)
                .getTables();
    }

    @Override
    @Transactional
    public Table createTable(Long databaseId, TableCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, DatabaseNotFoundException, TableMalformedException,
            TableNameExistsException, QueryMalformedException, TableNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final String internalName = tableMapper.nameToInternalName(createDto.getName());
        final Optional<Table> optional = database.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(internalName))
                .findFirst();
        if (optional.isPresent()) {
            log.error("Failed to create table with name {}: exists in metadata database", internalName);
            throw new TableNameExistsException("Failed to create table with name " + internalName + ": exists in metadata database");
        }
        final Table table = tableMapper.tableCreateDtoToTable(createDto);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
        final Boolean generatedSequence;
        try {
            final Connection connection = dataSource.getConnection();
            generatedSequence = tableMapper.tableToCreateTableRawQuery(connection, createDto);
            /* create history view */
            int[] idx = {0};
            /* map table */
            table.setInternalName(tableMapper.nameToInternalName(table.getName()));
            table.setQueueName("dbrepo");
            table.setRoutingKey("dbrepo." + database.getInternalName() + "." + table.getInternalName());
            table.setIsVersioned(true);
            table.setTdbid(databaseId);
            table.setDatabase(database);
            table.setConstraints(null);
            table.setCreatedBy(UserUtil.getId(principal));
            table.setOwnedBy(UserUtil.getId(principal));
            /* map columns */
            table.setColumns(createDto.getColumns()
                    .stream()
                    .map(column -> tableMapper.columnCreateDtoToTableColumn(column, database.getContainer().getImage()))
                    .map(column -> tableMapper.tableColumnToTableColumn(table, column, generatedSequence))
                    .toList());
            /* set the ordinal position for the columns */
            table.getColumns()
                    .forEach(column -> {
                        column.setOrdinalPosition(idx[0]++);
                    });
            /* set constraints */
            table.setConstraints(tableMapper.constraintsCreateDtoToConstraints(table, createDto.getConstraints()));
            final PreparedStatement preparedStatement = tableMapper.tableToCreateHistoryViewRawQuery(connection, table);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create table or history view: {}", e.getMessage());
            throw new TableMalformedException("Failed to create table or history view", e);
        } finally {
            dataSource.close();
        }
        database.getTables().add(table);
        /* create in metadata database */
        final Optional<Table> optionalEntity = databaseRepository.save(database)
                .getTables()
                .stream()
                .filter(t -> t.getDatabase().getId().equals(databaseId))
                .filter(t -> t.getInternalName().equals(table.getInternalName()))
                .findFirst();
        if (optionalEntity.isEmpty()) {
            log.error("Failed to find table of database with id {} and internal name {}", databaseId, table.getInternalName());
            throw new TableNotFoundException("Failed to find table of database with id " + databaseId + " and internal name " + table.getInternalName());
        }
        /* create in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Created table with id {} in metadata database & search database", optionalEntity.get().getId());
        return optionalEntity.get();
    }

    @Override
    @Transactional
    public TableColumn update(Long databaseId, Long tableId, Long columnId, ColumnSemanticsUpdateDto updateDto,
                              String authorization) throws TableNotFoundException, DatabaseNotFoundException,
            TableMalformedException {
        final Table table = find(databaseId, tableId);
        final TableColumn column = findColumn(table, columnId);
        /* assign */
        if (updateDto.getUnitUri() != null) {
            try {
                column.setUnit(semanticService.findUnit(updateDto.getUnitUri()));
                log.debug("found unit with uri {} in metadata database", updateDto.getUnitUri());
            } catch (UnitNotFoundException e) {
                final TableColumnUnit unit = TableColumnUnit.builder()
                        .uri(updateDto.getUnitUri())
                        .build();
                column.setUnit(unit);
            }
        } else {
            column.setUnit(null);
        }
        if (updateDto.getConceptUri() != null) {
            try {
                column.setConcept(semanticService.findConcept(updateDto.getConceptUri()));
                log.debug("found concept with uri {} in metadata database", updateDto.getConceptUri());
            } catch (ConceptNotFoundException e) {
                final TableColumnConcept concept = TableColumnConcept.builder()
                        .uri(updateDto.getConceptUri())
                        .build();
                column.setConcept(concept);
            }
        } else {
            column.setConcept(null);
        }
        /* update in metadata database */
        table.getColumns().set(table.getColumns().indexOf(column), column);
        databaseRepository.save(table.getDatabase());
        /* update in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Updated table column with id {} of table with id {} in metadata database & search database", columnId, tableId);
        return column;
    }

    @Override
    @Transactional
    public void deleteTable(Long databaseId, Long tableId)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, QueryMalformedException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final Table table = find(databaseId, tableId);
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            tableMapper.tableToDropTableRawQuery(connection, table);
        } catch (SQLException e) {
            log.error("Failed to drop table: {}", e.getMessage());
            throw new TableMalformedException("Failed to drop table: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        /* delete in metadata database */
        database.getTables().remove(table);
        databaseRepository.save(database);
        log.info("Deleted table with id {} in metadata database", table.getId());
        /* delete in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Deleted table with id {} in open search database", table.getId());
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
            log.error("Failed to find column with id {} in metadata database", columnId);
            throw new TableMalformedException("Failed to find column with id " + columnId + "  in metadata database");
        }
        return optional.get();
    }

}
