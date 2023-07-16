package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.ColumnTypeMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.UserMapper;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.service.TableService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Log4j2
@Service
public class TableServiceImpl extends HibernateConnector implements TableService {

    private final UserMapper userMapper;
    private final ViewMapper viewMapper;
    private final QueryMapper queryMapper;
    private final DatabaseMapper databaseMapper;
    private final TableRepository tableRepository;

    @Autowired
    public TableServiceImpl(UserMapper userMapper, ViewMapper viewMapper, QueryMapper queryMapper,
                            DatabaseMapper databaseMapper, TableRepository tableRepository) {
        this.userMapper = userMapper;
        this.viewMapper = viewMapper;
        this.queryMapper = queryMapper;
        this.databaseMapper = databaseMapper;
        this.tableRepository = tableRepository;
    }

    @Override
    public List<TableBriefDto> findAll(Database database) throws QueryMalformedException {
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = prepareStatement(connection, queryMapper.findAllTablesQuery(database));
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToTableDtoList(resultSet);
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to find tables: {}", e.getMessage());
            throw new QueryMalformedException("Failed to find tables: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public TableDto find(Database database, String name) throws TableNotFoundException, ColumnTypeMalformedException {
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement1 = prepareStatement(connection, queryMapper.findColumnsForTable(database, name));
            final ResultSet resultSet1 = preparedStatement1.executeQuery();
            final TableDto dto = queryMapper.resultSetToTableDto(resultSet1, name);
            dto.setDatabase(databaseMapper.databaseToDatabaseDto(database));
            dto.setDatabaseId(database.getId());
            dto.setCreator(userMapper.userToUserDto(database.getCreator()));
            dto.setCreatedBy(userMapper.userToUserDto(database.getCreator()).getId());
            dto.setQueueName("dbrepo." + database.getInternalName() + "." + dto.getInternalName());
            dto.setRoutingKey(dto.getQueueName());
            final PreparedStatement preparedStatement2 = prepareStatement(connection, queryMapper.findTableQuery(database, name));
            final ResultSet resultSet2 = preparedStatement2.executeQuery();
            resultSet2.next();
            dto.setIsVersioned(resultSet2.getBoolean("is_versioned"));
            return dto;
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to find table with name {}: {}", name, e.getMessage());
            throw new TableNotFoundException("Failed to find table with name " + name + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Table save(TableDto data) {
        final int[] idx = new int[]{0};
        final Table mapped = viewMapper.tableDtoToTable(data);
        mapped.setColumns(mapped.getColumns()
                .stream()
                .peek(c -> c.setOrdinalPosition(idx[0]++))
                .toList());
        /* save */
        final Table table = tableRepository.save(mapped);
        log.info("Saved table with id {}", table.getId());
        return table;
    }

}

