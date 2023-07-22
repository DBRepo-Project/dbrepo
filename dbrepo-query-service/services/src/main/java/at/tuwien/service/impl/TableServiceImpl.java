package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
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
    private final TableRepository tableRepository;
    private final DatabaseService databaseService;

    @Autowired
    public TableServiceImpl(QueryMapper queryMapper, TableRepository tableRepository, DatabaseService databaseService) {
        this.queryMapper = queryMapper;
        this.tableRepository = tableRepository;
        this.databaseService = databaseService;
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

}
