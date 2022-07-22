package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.util.*;

@Log4j2
@Service
public class TableServiceImpl extends HibernateConnector implements TableService {

    private final QueryMapper queryMapper;
    private final TableRepository tableRepository;
    private final DatabaseRepository databaseRepository;

    @Autowired
    public TableServiceImpl(QueryMapper queryMapper, TableRepository tableRepository, DatabaseRepository databaseRepository) {
        this.queryMapper = queryMapper;
        this.tableRepository = tableRepository;
        this.databaseRepository = databaseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Table find(Long containerId, Long databaseId, Long tableId) throws DatabaseNotFoundException,
            TableNotFoundException {
        final Optional<Table> table = tableRepository.find(containerId, databaseId, tableId);
        if (table.isEmpty()) {
            log.error("Failed to find table with id {} of database with id {} in metadata database", tableId,
                    databaseId);
            throw new TableNotFoundException("Failed to find table in metadata database");
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
    public List<TableHistoryDto> findHistory(Long containerId, Long databaseId, Long tableId)
            throws DatabaseNotFoundException, QueryMalformedException, TableNotFoundException {
        /* find */
        final Optional<Database> database = databaseRepository.findById(databaseId);
        if (database.isEmpty()) {
            log.error("Database with id {} not found in metadata database", databaseId);
            throw new DatabaseNotFoundException("Database not found in metadata database");
        }
        final Table table = find(containerId, databaseId, tableId);
        /* run query */
        final Session session = getSession(database.get(), true);
        final Transaction transaction = session.beginTransaction();
        /* use jpa to select one */
        final NativeQuery<?> query = session.createSQLQuery(queryMapper.historyRawQuery(table));
        try {
            log.debug("affected tuples {}", query.executeUpdate());
        } catch (PersistenceException e) {
            log.error("Failed to obtain query history");
            throw new QueryMalformedException("Failed to obtain query history", e);
        }
        final List<TableHistoryDto> history = queryMapper.resultListToTableHistoryDto(table, query.getResultList());
        transaction.commit();
        log.info("Found table history with {} tuples", history.size());
        log.debug("Found table history {}", history);
        return history;
    }

}
