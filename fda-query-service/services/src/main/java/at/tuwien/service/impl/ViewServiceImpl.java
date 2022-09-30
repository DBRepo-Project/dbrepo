package at.tuwien.service.impl;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.jpa.ViewRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import at.tuwien.service.ViewService;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static at.tuwien.service.impl.HibernateConnector.getDataSource;

@Log4j2
@Service
public class ViewServiceImpl implements ViewService {

    private final ViewMapper viewMapper;
    private final QueryMapper queryMapper;
    private final UserService userService;
    private final ViewRepository viewRepository;
    private final DatabaseService databaseService;

    @Autowired
    public ViewServiceImpl(ViewMapper viewMapper, QueryMapper queryMapper, UserService userService,
                           ViewRepository viewRepository, DatabaseService databaseService) {
        this.viewMapper = viewMapper;
        this.queryMapper = queryMapper;
        this.userService = userService;
        this.viewRepository = viewRepository;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<View> findAll(Long databaseId) {
        return viewRepository.findAllByDatabaseId(databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public View findById(Long databaseId, Long id) throws ViewNotFoundException {
        final Optional<View> optional = viewRepository.findByDatabaseIdAndId(databaseId, id);
        if (optional.isEmpty()) {
            log.error("Failed to find view with id {}", id);
            throw new ViewNotFoundException("Failed to find view");
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Long count(Long containerId, Long databaseId, Long viewId) throws DatabaseNotFoundException,
            DatabaseConnectionException, TableMalformedException, ViewNotFoundException, QueryMalformedException,
            ImageNotSupportedException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final View view = findById(databaseId, viewId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = queryMapper.queryToRawTimestampedQuery(connection, view.getQuery(), database, Instant.now(), null, null);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return queryMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to count tuples");
            throw new TableMalformedException("Failed to count tuples", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public View create(Long containerId, Long databaseId, ViewCreateDto data, Principal principal)
            throws DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException,
            ViewMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final User user = userService.findByUsername(principal.getName());
        /* create view */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = viewMapper.viewCreateDtoToRawCreateViewQuery(connection, data);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to create view", e);
        } finally {
            dataSource.close();
        }
        /* save in metadata database */
        final View entity = View.builder()
                .vdbid(databaseId)
                .name(viewMapper.nameToInternalName(data.getName()))
                .creator(user)
                .query(data.getQuery())
                .isInitialView(false)
                .isPublic(true)
                .build();
        final View view = viewRepository.save(entity);
        log.info("Created view with id {}", view.getId());
        log.debug("created view {}", view);
        return view;
    }

}
