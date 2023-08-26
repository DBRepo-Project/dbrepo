package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.mdb.DatabaseAccessRepository;
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
import java.util.UUID;

@Log4j2
@Service
public class AccessServiceImpl extends HibernateConnector implements AccessService {

    private final UserMapper userMapper;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;
    private final KeycloakGateway keycloakGateway;
    private final DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    public AccessServiceImpl(UserMapper userMapper, DatabaseMapper databaseMapper, DatabaseService databaseService,
                             KeycloakGateway keycloakGateway, DatabaseAccessRepository databaseAccessRepository) {
        this.userMapper = userMapper;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.keycloakGateway = keycloakGateway;
        this.databaseAccessRepository = databaseAccessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseAccess> list(Long databaseId) throws NotAllowedException {
        return databaseAccessRepository.findByHdbid(databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseAccess find(Long databaseId, UUID userId) throws AccessDeniedException {
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByDatabaseIdAndUserId(databaseId, userId);
        if (optional.isEmpty()) {
            log.error("Failed to find database access for database with id {}", databaseId);
            throw new AccessDeniedException("Failed to find database access for database with id " + databaseId);
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseAccess hasAccess(Long databaseId, UUID userId) throws NotAllowedException {
        final Optional<DatabaseAccess> optional = databaseAccessRepository.findByHdbidAndHuserid(databaseId, userId);
        if (optional.isEmpty()) {
            log.error("Failed to retrieve access, not found");
            throw new NotAllowedException("Failed to retrieve access");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void create(Long databaseId, DatabaseGiveAccessDto accessDto) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException,
            KeycloakRemoteException, AccessDeniedException {
        /* check */
        final Database database = databaseService.findById(databaseId);
        final Container container = database.getContainer();
        final UserDto user = userMapper.keycloakUserDtoToUserDto(keycloakGateway.findById(accessDto.getUserId()));
        if (databaseAccessRepository.findByDatabaseIdAndUserId(databaseId, user.getId()).isPresent()) {
            log.error("Failed to give access to user with id {}, has already permission", accessDto.getUserId());
            throw new NotAllowedException("Failed to give access");
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
            log.error("Failed to give database access {}, reason {}", accessDto, e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
        /* update access */
        final DatabaseAccess entity = databaseMapper.databaseGiveAccessDtoToDatabaseAccess(database, user.getId(), accessDto);
        databaseAccessRepository.save(entity);
        log.info("Handed access to database with id {} for user with username {}", databaseId, user.getUsername());
    }

    @Override
    @Transactional
    public void update(Long databaseId, UUID userId, DatabaseModifyAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, QueryMalformedException,
            DatabaseMalformedException, NotAllowedException, KeycloakRemoteException, AccessDeniedException {
        /* check */
        final Database database = databaseService.findById(databaseId);
        final Container container = database.getContainer();
        if (database.getOwnedBy().equals(userId)) {
            log.error("Failed to modify database access of user with id {}: is the owner", userId);
            throw new NotAllowedException("Failed to modify database access of user with id " + userId + ": is the owner");
        }
        final at.tuwien.api.user.UserDto user = userMapper.keycloakUserDtoToUserDto(keycloakGateway.findById(userId));
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container.getImage(), container, database);
        final DatabaseGiveAccessDto giveAccess = databaseMapper.databaseModifyAccessToDatabaseGiveAccessDto(userId, accessDto.getType());
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
        /* update access */
        databaseAccessRepository.save(databaseMapper.databaseModifyAccessDtoToDatabaseAccess(database, user, accessDto));
        log.info("Modified access to database with id {} for user with username {}", databaseId, user.getUsername());
    }

    @Override
    @Transactional
    public void delete(Long databaseId, UUID userId)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException, KeycloakRemoteException, AccessDeniedException {
        /* check */
        final Database database = databaseService.findById(databaseId);
        final Container container = database.getContainer();
        if (database.getOwnedBy().equals(userId)) {
            log.error("Failed to revoke database access of user with id {}: is the owner", userId);
            throw new NotAllowedException("Failed to revoke database access of user with id " + userId + ": is the owner");
        }
        final at.tuwien.api.user.UserDto user = userMapper.keycloakUserDtoToUserDto(keycloakGateway.findById(userId));
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container.getImage(), container);
        try {
            final Connection connection = dataSource.getConnection();
            /* create user */
            final PreparedStatement preparedStatement1 = databaseMapper.rawRevokeUserAccessQuery(connection, user.getUsername());
            preparedStatement1.executeUpdate();
            final PreparedStatement preparedStatement2 = databaseMapper.userToRawDropUserQuery(connection, user.getUsername());
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
