package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class AccessServiceImpl extends HibernateConnector implements AccessService {

    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public AccessServiceImpl(UserService userService, DatabaseMapper databaseMapper, DatabaseService databaseService,
                             DatabaseRepository databaseRepository, DatabaseIdxRepository databaseIdxRepository) {
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseAccess> list(Long databaseId) throws DatabaseNotFoundException {
        return databaseService.find(databaseId)
                .getAccesses();
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseAccess find(Long databaseId, UUID userId) throws AccessDeniedException, DatabaseNotFoundException {
        final Database database = databaseService.find(databaseId);
        if (database.getAccesses() == null) {
            database.setAccesses(new LinkedList<>()) /* FIXME proper hibernate mapping needed */;
        }
        final Optional<DatabaseAccess> optional = database.getAccesses()
                .stream()
                .filter(a -> a.getUser().getId().equals(userId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find database access for database with id {}", databaseId);
            throw new AccessDeniedException("Failed to find database access for database with id " + databaseId);
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void create(Long databaseId, UUID userId, DatabaseGiveAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        /* check */
        final Database database = databaseService.findById(databaseId);
        final Container container = database.getContainer();
        final User user = userService.find(userId);
        try {
            find(databaseId, userId);
            log.error("Failed to give access to user with id {}: has already permission", userId);
            throw new NotAllowedException("Failed to give access to user with id " + userId + ": has already permission");
        } catch (AccessDeniedException e) {
            /* ignore */
        }
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container.getImage(), container, database);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user if not exists */
            final PreparedStatement preparedStatement1 = databaseMapper.userToRawCreateUserQuery(connection, user);
            preparedStatement1.executeUpdate();
            /* grant access */
            final PreparedStatement preparedStatement2 = databaseMapper.rawGrantUserAccessQuery(connection, user.getUsername(), accessDto.getType());
            preparedStatement2.executeUpdate();
            final PreparedStatement preparedStatement3 = databaseMapper.rawGrantUserProcedure(connection, user.getUsername());
            preparedStatement3.executeUpdate();
            final PreparedStatement preparedStatement4 = databaseMapper.rawFlushPrivileges(connection);
            preparedStatement4.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to give database access {}: {}", accessDto, e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update in metadat database */
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(databaseId)
                .database(database)
                .huserid(userId)
                .type(databaseMapper.accessTypeDtoToAccessType(accessDto.getType()))
                .build();
        database.getAccesses()
                .add(access);
        databaseRepository.save(database);
        /* update in opensearch database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Created access to database with id {} for user with id {} in metadata database & search database", databaseId, userId);
    }

    @Override
    @Transactional
    public void update(Long databaseId, UUID userId, DatabaseModifyAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, QueryMalformedException,
            DatabaseMalformedException, NotAllowedException {
        /* check */
        final Database database = databaseService.findById(databaseId);
        final Container container = database.getContainer();
        final User user = userService.find(userId);
        if (database.getOwnedBy().equals(userId)) {
            log.error("Failed to modify database access of user with id {}: is the owner", userId);
            throw new NotAllowedException("Failed to modify database access of user with id " + userId + ": is the owner");
        }
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container.getImage(), container, database);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user if not exists */
            final PreparedStatement preparedStatement1 = databaseMapper.userToRawCreateUserQuery(connection, user);
            preparedStatement1.executeUpdate();
            /* grant access */
            final PreparedStatement preparedStatement2 = databaseMapper.rawGrantUserAccessQuery(connection, user.getUsername(), accessDto.getType());
            preparedStatement2.executeUpdate();
            final PreparedStatement preparedStatement3 = databaseMapper.rawGrantUserProcedure(connection, user.getUsername());
            preparedStatement3.executeUpdate();
            final PreparedStatement preparedStatement4 = databaseMapper.rawFlushPrivileges(connection);
            preparedStatement4.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to modify database access {}, reason {}", accessDto, e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update in metadata database */
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(databaseId)
                .database(database)
                .huserid(userId)
                .user(user)
                .type(databaseMapper.accessTypeDtoToAccessType(accessDto.getType()))
                .build();
        final int idx = database.getAccesses().indexOf(access);
        if (idx == -1) {
            log.error("Failed to find access in database with id {}", databaseId);
            throw new NotAllowedException("Failed to find access in database with id " + databaseId);
        }
        database.getAccesses().set(idx, access);
        databaseRepository.save(database);
        /* update in opensearch database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Updated access to database with id {} for user with id {} in metadata database & search database", databaseId, userId);
    }

    @Override
    @Transactional
    public void delete(Long databaseId, UUID userId) throws DatabaseNotFoundException, UserNotFoundException,
            NotAllowedException, QueryMalformedException, DatabaseMalformedException, AccessDeniedException {
        /* check */
        final Database database = databaseService.findById(databaseId);
        final Container container = database.getContainer();
        final DatabaseAccess access = find(databaseId, userId);
        if (database.getOwnedBy().equals(userId)) {
            log.error("Failed to revoke database access of user with id {}: is the owner", userId);
            throw new NotAllowedException("Failed to revoke database access of user with id " + userId + ": is the owner");
        }
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container.getImage(), container);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user */
            final PreparedStatement preparedStatement1 = databaseMapper.rawRevokeUserAccessQuery(connection, access.getUser().getUsername());
            preparedStatement1.executeUpdate();
            final PreparedStatement preparedStatement2 = databaseMapper.userToRawDropUserQuery(connection, access.getUser().getUsername());
            preparedStatement2.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to revoke database access, reason {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update in metadata database */
        database.getAccesses().remove(access);
        databaseRepository.save(database);
        /* update in opensearch database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(databaseId)));
        log.info("Deleted access to database with id {} for user with id {} in metadata database & search database", databaseId, userId);
    }

}
