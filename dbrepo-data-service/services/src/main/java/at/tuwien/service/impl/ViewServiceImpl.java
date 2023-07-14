package at.tuwien.service.impl;

import at.tuwien.api.database.ViewDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ColumnTypeMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.ViewNotFoundException;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.service.ViewService;
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
public class ViewServiceImpl extends HibernateConnector implements ViewService {

    private final QueryMapper queryMapper;

    @Autowired
    public ViewServiceImpl(QueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    @Override
    public List<ViewDto> findAll(Database database) throws QueryMalformedException {
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = prepareStatement(connection, queryMapper.findAllViewsQuery(database));
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToViewDtoList(resultSet);
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to find views: {}", e.getMessage());
            throw new QueryMalformedException("Failed to find views: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public ViewDto find(Database database, String name) throws ViewNotFoundException, ColumnTypeMalformedException {
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = prepareStatement(connection, queryMapper.findColumnsForTable(database, name));
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToViewDto(resultSet, name);
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to find view with name {}: {}", name, e.getMessage());
            throw new ViewNotFoundException("Failed to find view with name " + name + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    private PreparedStatement prepareStatement(Connection connection, String statement) throws QueryMalformedException {
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}m reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

}
