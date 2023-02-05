package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.elastic.DatabaseIdxRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class MariaDbServiceImpl extends HibernateConnector implements DatabaseService {

    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final ContainerService containerService;
    private final DatabaseRepository databaseRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public MariaDbServiceImpl(UserService userService, DatabaseMapper databaseMapper,
                              ContainerService containerService, DatabaseRepository databaseRepository,
                              DatabaseIdxRepository databaseIdxRepository) {
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.containerService = containerService;
        this.databaseRepository = databaseRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Override
    public List<Database> findAll(Long containerId) {
        return databaseRepository.findAll(containerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Database findPublicOrMineById(Long containerId, Long databaseId, Principal principal)
            throws DatabaseNotFoundException {
        final Optional<Database> database;
        if (principal == null) {
            log.trace("principal is null, find public database");
            database = databaseRepository.findPublic(containerId, databaseId);
        } else {
            log.trace("principal is not null, find public or mine database");
            database = databaseRepository.findPublicOrMine(containerId, databaseId, principal.getName());
        }
        if (database.isEmpty()) {
            log.error("Failed to find database");
            throw new DatabaseNotFoundException("Failed to find database");
        }
        return database.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Database findById(Long id, Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(databaseId);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {}", databaseId);
            throw new DatabaseNotFoundException("could not find database with this id");
        }
        return database.get();
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseMalformedException, ContainerNotFoundException,
            DatabaseConnectionException, QueryMalformedException, UserNotFoundException {
        final Container container = containerService.find(containerId);
        final Database database = findPublicOrMineById(containerId, databaseId, principal);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final User root = databaseMapper.containerToPrivilegedUser(container);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, database, root);
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
        log.info("Deleted database with id {}", databaseId);
        log.trace("deleted database {}", database);
        // delete in database_index - elastic search
        databaseIdxRepository.deleteById(databaseId);
        log.info("Deleted database in elastic search with id {}", databaseId);
    }

    @Override
    @Transactional
    public Database create(Long containerId, DatabaseCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, UserNotFoundException,
            DatabaseNameExistsException, DatabaseConnectionException, QueryMalformedException {
        final Container container = containerService.find(containerId);
        if (container.getDatabase() != null) {
            log.error("Failed to create database {} in container with id {}, only one database per container", createDto.getName(), containerId);
            throw new DatabaseMalformedException("Failed to create database " + createDto.getName() + " in container with id " + containerId + ", only one database per container");
        }
        final User root = databaseMapper.containerToPrivilegedUser(container);
        final User user = userService.findByUsername(principal.getName());
        /* start the object */
        final Database database = databaseMapper.databaseCreateDtoToDatabase(createDto);
        database.setId(containerId);
        database.setContainer(container);
        final User owner = userService.findByUsername(principal.getName());
        database.setCreator(owner);
        database.setOwner(owner);
        database.setExchangeName("dbrepo/" + database.getInternalName());
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, root);
        try {
            final Connection connection = dataSource.getConnection();
            /* create database */
            final PreparedStatement preparedStatement = databaseMapper.databaseToRawCreateDatabaseQuery(connection, database);
            preparedStatement.executeUpdate();
            /* create user */
            final PreparedStatement preparedStatement1 = databaseMapper.userToRawCreateUserQuery(connection, user);
            preparedStatement1.executeUpdate();
            final PreparedStatement preparedStatement2 = databaseMapper.rawGrantCreatorAccessQuery(connection, user);
            preparedStatement2.executeUpdate();
            final PreparedStatement preparedStatement3 = databaseMapper.rawFlushPrivileges(connection);
            preparedStatement3.executeUpdate();
            /* grant read-only access */
            final PreparedStatement preparedStatement4 = databaseMapper.rawGrantDefaultReadonlyAccessQuery(connection, user);
            preparedStatement4.executeUpdate();
            final PreparedStatement preparedStatement5 = databaseMapper.rawFlushPrivileges(connection);
            preparedStatement5.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create database with internal name {}, reason: {}", database.getInternalName(), e.getMessage());
            throw new DatabaseMalformedException("Failed to create database", e);
        } finally {
            dataSource.close();
        }
        log.info("Created user {} on database with owner access", user.getUsername());
        /* save in metadata database */
        final Database entity = databaseRepository.save(database);
        log.info("Created database with id {}", entity.getId());
        /* save in database_index - elastic search */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Saved database in elastic search with id {}", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public Database visibility(Long containerId, Long databaseId, DatabaseModifyVisibilityDto data)
            throws DatabaseNotFoundException {
        /* check */
        final Database database = findById(containerId, databaseId);
        /* map */
        database.setIsPublic(data.getIsPublic());
        /* update entity in metadata database */
        final Database entity = databaseRepository.save(database);
        log.info("Updated database visibility to {} with id {}", data.getIsPublic(), entity.getId());
        // save in database_index - elastic search
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database in elastic search with id {}", entity.getId());
        return entity;
    }

    @Override
    public Database transfer(Long containerId, Long databaseId, DatabaseTransferDto transferDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException {
        /* check */
        final Database entity = findById(containerId, databaseId);
        final User user = userService.findByUsername(transferDto.getUsername());
        if (!entity.getOwner().getId().equals(user.getId())) {
            log.error("Failed to transfer ownership because user with id {} is not owner of database with id {}", user.getId(), entity.getId());
            throw new NotAllowedException("Failed to transfer ownership");
        }
        /* update in metadata database */
        entity.setOwner(user);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database in elastic search with id {}", entity.getId());
        return entity;
    }

}
