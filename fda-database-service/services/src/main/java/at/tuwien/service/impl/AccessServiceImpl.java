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
    public DatabaseAccess hasAccess(Long databaseId, String username) throws AccessDeniedException {
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username);
        if (optional.isEmpty()) {
            log.error("Failed to retrieve access, not found");
            throw new AccessDeniedException("Failed to retrieve access");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void giveAccess(Long containerId, Long databaseId, DatabaseGiveAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        /* check */
        final Database database = databaseService.findById(containerId, databaseId);
        final Container container = database.getContainer();
        final User user = userService.findByUsername(accessDto.getUsername());
        if (database.getCreator().getUsername().equals(user.getUsername())) {
            log.error("Failed to modify access of user with username {}, because it is the owner", user.getUsername());
            throw new NotAllowedException("Failed modify access");
        }
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, database.getCreator());
        try {
            final Connection connection = dataSource.getConnection();
            /* create user */
            final PreparedStatement preparedStatement1 = databaseMapper.userToRawCreateUserQuery(connection, user);
            preparedStatement1.executeUpdate();
            final PreparedStatement preparedStatement2 = databaseMapper.rawGrantUserAccessQuery(connection, accessDto);
            preparedStatement2.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to give database access {}, reason {}", accessDto, e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update access */
        final DatabaseAccess access = databaseMapper.databaseGiveAccessDtoToDatabaseAccess(database, user, accessDto);
        final DatabaseAccess entity = databaseAccessRepository.save(access);
        log.info("Gave access to database with id {} for user with username {}", databaseId, user.getId());
        log.trace("gave access {} to database for user {}", entity, user);
    }

    @Override
    @Transactional
    public void modifyAccess(Long containerId, Long databaseId, String username, DatabaseModifyAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        /* check */
        final Database database = databaseService.findById(containerId, databaseId);
        final Container container = database.getContainer();
        final User user = userService.findByUsername(username);
        if (database.getCreator().getUsername().equals(username)) {
            log.error("Failed to modify access of user with username {}, because it is the owner", username);
            throw new NotAllowedException("Failed modify access");
        }
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, database.getCreator());
        final DatabaseGiveAccessDto giveAccess = databaseMapper.databaseModifyAccessToDatabaseGiveAccessDto(username, accessDto);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user */
            final PreparedStatement preparedStatement2 = databaseMapper.rawGrantUserAccessQuery(connection, giveAccess);
            preparedStatement2.executeUpdate();
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
        log.trace("modified access {} to database for user {}", entity, user);
    }

    @Override
    @Transactional
    public void revokeAccess(Long containerId, Long databaseId, String username)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        /* check */
        final Database database = databaseService.findById(containerId, databaseId);
        final Container container = database.getContainer();
        final User user = userService.findByUsername(username);
        if (database.getCreator().getUsername().equals(username)) {
            log.error("Failed to revoke access of user with username {}, because it is the owner", username);
            throw new NotAllowedException("Failed revoke access");
        }
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, database.getCreator());
        try {
            final Connection connection = dataSource.getConnection();
            /* create user */
            final PreparedStatement preparedStatement2 = databaseMapper.rawRevokeUserAccessQuery(connection, user);
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
