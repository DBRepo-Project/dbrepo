package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.repository.jpa.TableRepository;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Log4j2
@Service
public class TableServiceImpl extends HibernateConnector implements TableService {

    private final QueryMapper queryMapper;
    private final UserService userService;
    private final TableRepository tableRepository;
    private final DatabaseService databaseService;

    @Autowired
    public TableServiceImpl(QueryMapper queryMapper, UserService userService, TableRepository tableRepository,
                            DatabaseService databaseService) {
        this.queryMapper = queryMapper;
        this.userService = userService;
        this.tableRepository = tableRepository;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public Table find(Long containerId, Long databaseId, Long tableId) throws DatabaseNotFoundException,
            TableNotFoundException {
        final Optional<Table> table = tableRepository.find(containerId, databaseId, tableId);
        if (table.isEmpty()) {
            log.error("Failed to find table with id {} in container with id {} and database with id {}", tableId, containerId, databaseId);
            throw new TableNotFoundException("Failed to find table");
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
    public List<TableHistoryDto> findHistory(Long containerId, Long databaseId, Long tableId, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, QueryStoreException,
            QueryMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final Table table = find(containerId, databaseId, tableId);
        final User user = userService.findByPrincipalOrAnonymous(principal, database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, user);
        /* use jpa to select one */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.historyRawQuery(connection, table);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultListToTableHistoryDto(resultSet);
        } catch (SQLException e) {
            log.error("Failed to map table history: {}", e.getMessage());
            throw new QueryStoreException("Failed to map table history", e);
        } finally {
            dataSource.close();
        }
    }

}
