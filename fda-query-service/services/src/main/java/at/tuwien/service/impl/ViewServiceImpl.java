package at.tuwien.service.impl;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.elastic.ViewIdxRepository;
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
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class ViewServiceImpl extends HibernateConnector implements ViewService {

    private final ViewMapper viewMapper;
    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final ViewRepository viewRepository;
    private final DatabaseService databaseService;
    private final ViewIdxRepository viewIdxRepository;

    @Autowired
    public ViewServiceImpl(ViewMapper viewMapper, UserService userService, DatabaseMapper databaseMapper,
                           ViewRepository viewRepository, DatabaseService databaseService,
                           ViewIdxRepository viewIdxRepository) {
        this.viewMapper = viewMapper;
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.viewRepository = viewRepository;
        this.databaseService = databaseService;
        this.viewIdxRepository = viewIdxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<View> findAll(Long databaseId, Principal principal) throws UserNotFoundException {
        if (principal == null) {
            log.trace("principal is null, list only public views");
            return viewRepository.findAllPublicByDatabaseId(databaseId);
        }
        log.trace("principal is not null, list public views and mine");
        return viewRepository.findAllPublicOrMineByDatabaseId(databaseId, principal.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public View findById(Long databaseId, Long id, Principal principal) throws ViewNotFoundException {
        final Optional<View> optional;
        if (principal == null) {
            log.trace("principal is null, find only public view");
            optional = viewRepository.findPublicByDatabaseIdAndId(databaseId, id);
        } else {
            log.trace("principal is not null, find public view or mine");
            optional = viewRepository.findPublicOrMineByDatabaseIdAndId(databaseId, id, principal.getName());
        }
        if (optional.isEmpty()) {
            log.error("Failed to find view with id {} and database with id {}", id, databaseId);
            throw new ViewNotFoundException("Failed to find view with id " + id);
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Long id, Principal principal) throws ViewNotFoundException,
            UserNotFoundException, DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException, ViewMalformedException {
        /* find */
        final View view = findById(databaseId, id, principal);
        final Database database = databaseService.find(containerId, databaseId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* delete view */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement createViewStatement = viewMapper.viewToRawDeleteViewQuery(connection, view);
            createViewStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to delete view", e);
        } finally {
            dataSource.close();
        }
        /* delete in metadata database */
        viewRepository.delete(view);
        log.info("Deleted view with id {}", view.getId());
        log.trace("deleted view {}", view);
        viewIdxRepository.deleteById(id);
        log.info("Deleted view with id {} in elastic search", id);
    }

    @Override
    @Transactional
    public View create(Long containerId, Long databaseId, ViewCreateDto data, Principal principal)
            throws DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException,
            ViewMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final User user = userService.findByUsername(principal.getName());
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* create view */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement createViewStatement = viewMapper.viewCreateDtoToRawCreateViewQuery(connection, data);
            createViewStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to create view", e);
        } finally {
            dataSource.close();
        }
        /* save in metadata database */
        final View entity = View.builder()
                .vcid(containerId)
                .vdbid(databaseId)
                .name(data.getName())
                .internalName(viewMapper.nameToInternalName(data.getName()))
                .creator(user)
                .database(database)
                .query(data.getQuery())
                .isInitialView(false)
                .isPublic(data.getIsPublic())
                .build();
        final View view = viewRepository.save(entity);
        log.info("Created view with id {}", view.getId());
        log.trace("created view {}", view);
        final View elView = viewIdxRepository.save(view);
        log.info("Created view with id {} in elastic search", elView.getId());
        return view;
    }

}
