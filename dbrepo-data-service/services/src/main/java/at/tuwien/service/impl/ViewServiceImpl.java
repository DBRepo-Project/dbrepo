package at.tuwien.service.impl;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.exception.ColumnTypeMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.ViewNotFoundException;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.UserMapper;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.mdb.ViewRepository;
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

    private final UserMapper userMapper;
    private final ViewMapper viewMapper;
    private final QueryMapper queryMapper;
    private final DatabaseMapper databaseMapper;
    private final ViewRepository viewRepository;

    @Autowired
    public ViewServiceImpl(UserMapper userMapper, ViewMapper viewMapper, QueryMapper queryMapper,
                           DatabaseMapper databaseMapper, ViewRepository viewRepository) {
        this.userMapper = userMapper;
        this.viewMapper = viewMapper;
        this.queryMapper = queryMapper;
        this.databaseMapper = databaseMapper;
        this.viewRepository = viewRepository;
    }

    @Override
    public List<ViewBriefDto> findAll(Database database) throws QueryMalformedException {
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
            final ViewDto dto = queryMapper.resultSetToViewDto(resultSet, name);
            dto.setDatabase(databaseMapper.databaseToDatabaseDto(database));
            dto.setCreator(userMapper.userToUserDto(database.getCreator()));
            dto.setCreatedBy(userMapper.userToUserDto(database.getCreator()).getId());
            return dto;
        } catch (SQLException | QueryMalformedException e) {
            log.error("Failed to find view with name {}: {}", name, e.getMessage());
            throw new ViewNotFoundException("Failed to find view with name " + name + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public View save(ViewDto data) {
        final int[] idx = new int[]{0};
        final View mapped = viewMapper.viewDtoToView(data);
        mapped.setColumns(mapped.getColumns()
                .stream()
                .peek(c -> c.setOrdinalPosition(idx[0]++))
                .toList());
        /* save */
        final View view = viewRepository.save(mapped);
        log.info("Saved view with id {}", view.getId());
        return view;
    }

}
