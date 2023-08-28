package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
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
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
@Service
public class MariaDbServiceImpl extends HibernateConnector implements DatabaseService {

    private final QueryConfig queryConfig;
    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final ContainerService containerService;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public MariaDbServiceImpl(QueryConfig queryConfig, UserService userService, DatabaseMapper databaseMapper,
                              ContainerService containerService, DatabaseRepository databaseRepository,
                              DatabaseIdxRepository databaseIdxRepository) {
        this.queryConfig = queryConfig;
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.containerService = containerService;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Override
    public List<Database> findAll() {
        return databaseRepository.findAll();
    }

    @Override
    public Database find(Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(databaseId);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {}", databaseId);
            throw new DatabaseNotFoundException("could not find database with this id");
        }
        return database.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Database findPublicOrMineById(Long databaseId, UUID userId) throws DatabaseNotFoundException {
        final Optional<Database> database;
        if (userId == null) {
            log.trace("user id is null, find public database");
            database = databaseRepository.findPublic(databaseId);
        } else {
            log.trace("user id is not null, find public or mine database");
            database = databaseRepository.findPublicOrMine(databaseId, userId);
        }
        if (database.isEmpty()) {
            log.error("Failed to find database with id {}", databaseId);
            throw new DatabaseNotFoundException("Failed to find database with id " + databaseId);
        }
        return database.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Database findById(Long id) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(id);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {}", id);
            throw new DatabaseNotFoundException("could not find database with id " + id);
        }
        return database.get();
    }

    @Override
    @Transactional
    public void delete(Long databaseId, UUID userId) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseMalformedException, DatabaseConnectionException,
            QueryMalformedException, UserNotFoundException {
        final Database database = findById(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        if (!database.getOwnedBy().equals(userId)) {
            log.error("Failed to delete database: user is not owner");
            throw new DatabaseMalformedException("Failed to delete database: user is not owner");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = databaseMapper.databaseToRawDeleteDatabaseQuery(connection, database);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete database {}, reason: {}", database, e.getMessage());
            throw new DatabaseMalformedException("Failed to delete database", e);
        } finally {
            dataSource.close();
        }
        /* save in metadata database */
        databaseRepository.deleteById(databaseId);
        log.info("Deleted database with id {} in metadata database", databaseId);
        /* save in open search database */
        databaseIdxRepository.deleteById(databaseId);
        log.info("Deleted database with id {} in open search database", databaseId);
    }

    @Override
    @Transactional
    public Database create(DatabaseCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException, DatabaseMalformedException, AmqpException,
            ContainerConnectionException, UserNotFoundException, DatabaseNameExistsException,
            DatabaseConnectionException, QueryMalformedException, KeycloakRemoteException, AccessDeniedException {
        /* start the object */
        final Database database = databaseMapper.databaseCreateDtoToDatabase(createDto);
        final Container container = containerService.find(database.getCid());
        final UserDto owner = userService.findByUsername(principal.getName());
        database.setContainer(container);
        database.setOwnedBy(owner.getId());
        database.setCreatedBy(owner.getId());
        database.setContactPerson(owner.getId());
        database.setExchangeName("dbrepo." + database.getInternalName());
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container.getImage(), container);
        try {
            final Connection connection = dataSource.getConnection();
            /* create database */
            final PreparedStatement preparedStatement1 = databaseMapper.databaseToRawCreateDatabaseQuery(connection, database);
            preparedStatement1.executeUpdate();
            /* create user */
            final PreparedStatement preparedStatement2 = databaseMapper.userToRawCreateUserQuery(connection, owner);
            preparedStatement2.executeUpdate();
            /* give access */
            final PreparedStatement preparedStatement3 = databaseMapper.rawGrantCreatorAccessQuery(connection, database.getInternalName(), principal.getName(), queryConfig.getGrantPrivileges());
            preparedStatement3.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create database with internal name {}, reason: {}", database.getInternalName(), e.getMessage());
            throw new DatabaseMalformedException("Failed to create database: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created user {} on database with owner access", owner.getUsername());
        /* save in metadata database */
        final Database entity = databaseRepository.save(database);
        log.info("Created database with id {} in metadata database", entity.getId());
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Created database with id {} in open search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public void updatePassword(UserDto user) throws DatabaseMalformedException, QueryMalformedException {
        /* start the object */
        final List<Database> databases = databaseRepository.findReadAccess(user.getId())
                .stream()
                .distinct()
                .toList();
        log.trace("found {} distinct databases", databases.size());
        for (Database database : databases) {
            final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer());
            try {
                final Connection connection = dataSource.getConnection();
                /* update password database */
                final PreparedStatement preparedStatement = databaseMapper.userToRawUpdateUserQuery(connection, user);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed to update user password in database with internal name {}: {}", database.getInternalName(), e.getMessage());
                throw new DatabaseMalformedException("Failed to update user password in database with internal name " + database.getInternalName() + ": " + e.getMessage(), e);
            } finally {
                dataSource.close();
            }
            log.debug("updated user password in database with internal name {}", database.getInternalName());
        }
    }

    @Override
    @Transactional
    public Database visibility(Long databaseId, DatabaseModifyVisibilityDto data) throws DatabaseNotFoundException {
        /* check */
        final Database database = findById(databaseId);
        /* map */
        database.setIsPublic(data.getIsPublic());
        /* update entity in metadata database */
        final Database entity = databaseRepository.save(database);
        log.info("Updated database visibility of database with id {} in metadata database", entity.getId());
        /* update in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database visibility of database with id {} in open search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public Database transfer(Long databaseId, DatabaseTransferDto transferDto) throws DatabaseNotFoundException,
            UserNotFoundException, KeycloakRemoteException, AccessDeniedException {
        /* check */
        final Database database = findById(databaseId);
        final UserDto user = userService.findByUsername(transferDto.getUsername());
        /* update in metadata database */
        database.setOwnedBy(user.getId());
        final Database entity = databaseRepository.save(database);
        log.info("Updated database owner of database with id {} in metadata database", entity.getId());
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database owner of database with id {} in open search database", entity.getId());
        return entity;
    }

}
