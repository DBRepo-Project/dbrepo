package at.tuwien.service.impl;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.ViewColumn;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.ViewService;
import at.tuwien.utils.UserUtil;
import com.google.common.hash.Hashing;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
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
    private final QueryMapper queryMapper;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public ViewServiceImpl(ViewMapper viewMapper, QueryMapper queryMapper, DatabaseMapper databaseMapper,
                           DatabaseService databaseService, DatabaseRepository databaseRepository,
                           DatabaseIdxRepository databaseIdxRepository) {
        this.viewMapper = viewMapper;
        this.queryMapper = queryMapper;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Override
    public View findById(Long databaseId, Long viewId) throws ViewNotFoundException, DatabaseNotFoundException {
        final Optional<View> optional = databaseService.find(databaseId)
                .getViews()
                .stream()
                .filter(v -> v.getId().equals(viewId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find view with id {} in metadata database", viewId);
            throw new ViewNotFoundException("Failed to find view with id " + viewId + " in metadata database");
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<View> findAll(Long databaseId, Principal principal) throws UserNotFoundException,
            DatabaseNotFoundException {
        if (principal == null) {
            final List<View> views = databaseService.find(databaseId)
                    .getViews()
                    .stream()
                    .filter(v -> v.getDatabase().getId().equals(databaseId))
                    .toList();
            log.debug("list {} public view(s)", views.size());
            return views;
        }
        final List<View> views = databaseService.find(databaseId)
                .getViews()
                .stream()
                .filter(v -> v.getDatabase().getId().equals(databaseId) || v.getCreatedBy().equals(UserUtil.getId(principal)))
                .toList();
        log.debug("list {} public or private self-owned view(s)", views.size());
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public View findById(Long databaseId, Long id, Principal principal) throws ViewNotFoundException,
            UserNotFoundException, DatabaseNotFoundException {
        final Optional<View> optional = findAll(databaseId, principal)
                .stream()
                .filter(v -> v.getId().equals(id))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find view with id {} in metadata database", id);
            throw new ViewNotFoundException("Failed to find view with id " + id + " in metadata database");
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
        database.getViews().remove(view);
        databaseRepository.save(database);
        /* delete in opensearch database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Deleted view with id {} in metadata database & search database", id);
    }

    @Override
    @Transactional
    public View create(Long databaseId, ViewCreateDto data, Principal principal)
            throws DatabaseNotFoundException, DatabaseConnectionException, QueryMalformedException,
            ViewMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        /* create view */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        final List<TableColumn> columns;
        try {
            columns = queryMapper.parseColumns(data.getQuery(), database);
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new QueryMalformedException("Failed to map/parse columns: " + e.getMessage(), e);
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
                .createdBy(UserUtil.getId(principal))
                .query(data.getQuery())
                .queryHash(Hashing.sha256()
                        .hashString(data.getQuery(), StandardCharsets.UTF_8)
                        .toString())
                .isInitialView(false)
                .isPublic(data.getIsPublic())
                .build();
        entity.setColumns(viewMapper.tableColumnsToViewColumns(entity, columns));
        database.getViews()
                .add(entity);
        final Optional<View> optional = databaseRepository.save(database)
                .getViews()
                .stream()
                .filter(v -> v.getInternalName().equals(entity.getInternalName()))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find created view from database with id {}", databaseId);
            throw new ViewMalformedException("Failed to find created view from database with id " + databaseId);
        }
        /* save in opensearch database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Created view with id {} in metadata database & search database", optional.get().getId());
        return optional.get();
    }

}
