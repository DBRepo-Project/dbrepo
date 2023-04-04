package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
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
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class AccessServiceImpl extends HibernateConnector implements AccessService {

    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;
    private final DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    public AccessServiceImpl(UserService userService, DatabaseMapper databaseMapper, DatabaseService databaseService,
                             DatabaseAccessRepository databaseAccessRepository) {
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.databaseAccessRepository = databaseAccessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseAccess> list(Long databaseId) throws AccessDeniedException {
        return databaseAccessRepository.findByHdbid(databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseAccess find(Long databaseId, String username) throws AccessDeniedException {
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username);
        if (optional.isEmpty()) {
            throw new AccessDeniedException("Failed to retrieve access to database with id " + databaseId + " for user with username '" + username + "'");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void create(Long containerId, Long databaseId, DatabaseGiveAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        /* check */
        final Database database = databaseService.findById(containerId, databaseId);
        final Container container = database.getContainer();
        final User user = userService.findByUsername(accessDto.getUsername());
        log.trace("give access to user with username {}", user.getUsername());
        if (database.getOwner().getUsername().equals(user.getUsername())) {
            log.error("Failed to give access to user with username {}, is already database owner", user.getUsername());
            throw new NotAllowedException("Failed give access");
        }
        if (databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, accessDto.getUsername()).isPresent()) {
            log.error("Failed to give access to user with username {}, has already permission", accessDto.getUsername());
            throw new NotAllowedException("Failed to give access");
        }
        final User root = databaseMapper.containerToPrivilegedUser(container);
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, database, root);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user if not exists */
            final PreparedStatement preparedStatement1 = databaseMapper.userToRawCreateUserQuery(connection, user);
            preparedStatement1.executeUpdate();
            /* grant access */
            final PreparedStatement preparedStatement2 = databaseMapper.rawGrantUserAccessQuery(connection, accessDto);
            preparedStatement2.executeUpdate();
            final PreparedStatement preparedStatement3 = databaseMapper.rawGrantUserProcedure(connection, user);
            preparedStatement3.executeUpdate();
            final PreparedStatement preparedStatement4 = databaseMapper.rawFlushPrivileges(connection);
            preparedStatement4.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to give database access {}, reason {}", accessDto, e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update access */
        final DatabaseAccess entity = databaseMapper.databaseGiveAccessDtoToDatabaseAccess(database, user, accessDto);
        databaseAccessRepository.save(entity);
        log.info("Gave access to database with id {} for user with username {}", databaseId, user.getUsername());
    }

    @Override
    @Transactional
    public void update(Long containerId, Long databaseId, String username, DatabaseModifyAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        /* check */
        final Database database = databaseService.findById(containerId, databaseId);
        final Container container = database.getContainer();
        final User user = userService.findByUsername(username);
        if (database.getOwner().getUsername().equals(username)) {
            log.error("Failed to modify access of user with username {}, because it is the owner", username);
            throw new NotAllowedException("Failed modify access");
        }
        final User root = databaseMapper.containerToPrivilegedUser(container);
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, database, root);
        final DatabaseGiveAccessDto giveAccess = databaseMapper.databaseModifyAccessToDatabaseGiveAccessDto(username, accessDto);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user if not exists */
            final PreparedStatement preparedStatement1 = databaseMapper.userToRawCreateUserQuery(connection, user);
            preparedStatement1.executeUpdate();
            /* grant access */
            final PreparedStatement preparedStatement2 = databaseMapper.rawGrantUserAccessQuery(connection, giveAccess);
            preparedStatement2.executeUpdate();
            final PreparedStatement preparedStatement3 = databaseMapper.rawGrantUserProcedure(connection, user);
            preparedStatement3.executeUpdate();
            final PreparedStatement preparedStatement4 = databaseMapper.rawFlushPrivileges(connection);
            preparedStatement4.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to modify database access {}, reason {}", accessDto, e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update access */
        final DatabaseAccess access = databaseMapper.databaseModifyAccessDtoToDatabaseAccess(database, user, accessDto);
        final DatabaseAccess entity = databaseAccessRepository.save(access);
        log.info("Modified access to database with id {} for user with username {}", databaseId, username);
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, String username)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        /* check */
        final Database database = databaseService.findById(containerId, databaseId);
        final Container container = database.getContainer();
        final User user = userService.findByUsername(username);
        if (database.getOwner().getUsername().equals(username)) {
            log.error("Failed to revoke access of user with username {}, because it is the owner", username);
            throw new NotAllowedException("Failed revoke access");
        }
        final User root = databaseMapper.containerToPrivilegedUser(container);
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, root);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user */
            final PreparedStatement preparedStatement1 = databaseMapper.rawRevokeUserAccessQuery(connection, user);
            preparedStatement1.executeUpdate();
            final PreparedStatement preparedStatement2 = databaseMapper.userToRawDropUserQuery(connection, user);
            preparedStatement2.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to revoke database access, reason {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update access */
        databaseAccessRepository.deleteByHdbidAndHuserid(databaseId, user.getId());
        log.info("Revoked access to database with id {} for user with username {}", databaseId, user.getId());
        log.trace("revoked access to database for user {}", user);
    }

}
