package at.tuwien.service.impl;

import at.tuwien.CreateTableRawQuery;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnit;
import java.security.Principal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TableServiceImpl extends HibernateConnector implements TableService {

    private final TableMapper tableMapper;
    private final UserService userService;
    private final TableRepository tableRepository;
    private final DatabaseService databaseService;

    @Autowired
    public TableServiceImpl(TableMapper tableMapper, UserService userService, TableRepository tableRepository,
                            DatabaseService databaseService) {
        this.tableMapper = tableMapper;
        this.userService = userService;
        this.tableRepository = tableRepository;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findAll(Long containerId, Long databaseId, Principal principal)
            throws DatabaseNotFoundException {
        final Database database = databaseService.findPublicOrMineById(containerId, databaseId, principal);
        return tableRepository.findByDatabase(database);
    }

    @Override
    @Transactional
    public void deleteTable(Long containerId, Long databaseId, Long tableId, Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException {
        /* find */
        final Database database = databaseService.findPublicOrMineById(containerId, databaseId, principal);
        final Table table = findById(containerId, databaseId, tableId, principal);
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        try {
            session.createSQLQuery(tableMapper.tableToDropTableRawQuery(table));
            activeConnection(session);
            transaction.commit();
            log.info("Deleted table with id {}", table.getId());
            log.debug("deleted table {}", table);
        } catch (PersistenceException e) {
            log.error("Failed to drop table with id {}", tableId);
            log.debug("failed to drop table {}", table);
            throw new TableMalformedException("Failed to drop table");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Table findById(Long containerId, Long databaseId, Long tableId, Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException {
        final Database database = databaseService.findPublicOrMineById(containerId, databaseId, principal);
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
            TableNameExistsException, UserNotFoundException {
        /* find */
        final Database database = databaseService.findPublicOrMineById(containerId, databaseId, principal);
        final Optional<Table> optional = tableRepository.findByDatabaseAndInternalName(database,
                tableMapper.nameToInternalName(createDto.getName()));
        if (optional.isPresent()) {
            log.error("Table name exists in database with id {} as table id {}", database.getId(),
                    optional.get().getId());
            throw new TableNameExistsException("Table name exists");
        }
        /* run query */
        final Session session = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        final Transaction transaction = session.beginTransaction();
        final CreateTableRawQuery query = tableMapper.tableToCreateTableRawQuery(database, createDto);
        if (query.getGenerated()) {
            /* in case the id column needs to be generated, we need to generate the sequence too */
            try {
                session.createSQLQuery(tableMapper.tableToCreateSequenceRawQuery(database, createDto))
                        .executeUpdate();
            } catch (PersistenceException e) {
                log.error("Table sequence exists, but table does not. Create an issue for this.");
                session.close();
                throw new TableNameExistsException("Sequence exists", e);
            }
            log.debug("created id sequence");
        }
        try {
            session.createSQLQuery(query.getQuery())
                    .executeUpdate();
            activeConnection(session);
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to create table");
            log.debug("failed to create table: {}", e.getMessage());
            session.close();
            throw new TableMalformedException("Failed to create table", e);
        }
        session.close();
        int[] idx = {0};
        /* map table */
        final Table tmp = tableMapper.tableCreateDtoToTable(createDto);
        tmp.setInternalName(tableMapper.nameToInternalName(tmp.getName()));
        tmp.setTdbid(databaseId);
        tmp.setDatabase(database);
        tmp.setTopic(tmp.getInternalName());
        tmp.setColumns(List.of());
        final User creator = userService.findByUsername(principal.getName());
        tmp.setCreator(creator);
        log.debug("mapped new table {}", tmp);
        /* save in metadata database */
        final Table entity = tableRepository.save(tmp);
        entity.setColumns(Arrays.stream(createDto.getColumns())
                .map(tableMapper::columnCreateDtoToTableColumn)
                .map(column -> tableMapper.tableColumnToTableColumn(entity, column, query))
                .collect(Collectors.toList()));
        /* set the ordinal position for the columns */
        entity.getColumns()
                .forEach(column -> {
                    column.setOrdinalPosition(idx[0]++);
                });
        /* create history view */
        final Session session2 = getCurrentSession(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Transaction transaction2 = session2.beginTransaction();
            session2.createSQLQuery(tableMapper.tableToCreateHistoryViewRawQuery(entity))
                    .executeUpdate();
            activeConnection(session2);
            transaction2.commit();
        } catch (PersistenceException e) {
            log.error("Failed to create history view");
            log.debug("failed to create history view: {}", e.getMessage());
            session2.close();
            throw new TableMalformedException("Failed to create history view");
        }
        session2.close();
        /* save */
        final Table table = tableRepository.save(entity);
        log.info("Created table with id {}", table.getId());
        log.debug("created table {}", table);
        return table;
    }

}
