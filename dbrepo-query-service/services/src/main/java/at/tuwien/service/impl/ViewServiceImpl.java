package at.tuwien.service.impl;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.sdb.ViewIdxRepository;
import at.tuwien.repository.mdb.ViewRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.UserService;
import at.tuwien.service.ViewService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
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
    private final ViewRepository viewRepository;
    private final DatabaseService databaseService;
    private final ViewIdxRepository viewIdxRepository;
    private final QueryService queryService;

    @Autowired
    public ViewServiceImpl(ViewMapper viewMapper, UserService userService, ViewRepository viewRepository,
                           DatabaseService databaseService, ViewIdxRepository viewIdxRepository, QueryService queryService) {
        this.viewMapper = viewMapper;
        this.userService = userService;
        this.viewRepository = viewRepository;
        this.databaseService = databaseService;
        this.viewIdxRepository = viewIdxRepository;
        this.queryService = queryService;
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
    public void delete(Long databaseId, Long id, Principal principal) throws ViewNotFoundException,
            UserNotFoundException, DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException, ViewMalformedException {
        /* find */
        final View view = findById(databaseId, id, principal);
        final Database database = databaseService.find(databaseId);
        /* delete view */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
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
        log.info("Deleted view with id {} in metadata database", id);
        viewIdxRepository.deleteById(id);
        log.info("Deleted view with id {} in open search database", id);
    }

    @Override
    @Transactional
    public View create(Long databaseId, ViewCreateDto data, Principal principal)
            throws DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException,
            ViewMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        final User user = userService.findByUsername(principal.getName());
        /* create view */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        final List<TableColumn> columns;
        try {
            columns = queryService.parseColumns(data.getQuery(), database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new QueryMalformedException(e.getMessage(), e);
        }
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement createViewStatement = viewMapper.viewCreateDtoToRawCreateViewQuery(connection, data);
            createViewStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to create view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        /* save in metadata database */
        final View entity = View.builder()
                .vdbid(databaseId)
                .database(database)
                .name(data.getName())
                .internalName(viewMapper.nameToInternalName(data.getName()))
                .createdBy(user.getId())
                .query(data.getQuery())
                .isInitialView(false)
                .isPublic(data.getIsPublic())
                .columns(columns)
                .build();
        final View view = viewRepository.save(entity);
        log.info("Created view with id {} in metadata database", view.getId());
        viewIdxRepository.save(viewMapper.viewToViewDto(view));
        log.info("Created view with id {} in open search database", view.getId());
        return view;
    }

}
