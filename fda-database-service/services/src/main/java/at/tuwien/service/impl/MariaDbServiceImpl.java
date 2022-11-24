package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.AmqpMapper;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.elastic.DatabaseidxRepository;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class MariaDbServiceImpl extends HibernateConnector implements DatabaseService {

    private final AmqpMapper amqpMapper;
    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final ContainerService containerService;
    private final DatabaseRepository databaseRepository;
    private final DatabaseidxRepository databaseidxRepository;

    @Autowired
    public MariaDbServiceImpl(AmqpMapper amqpMapper, UserService userService, DatabaseMapper databaseMapper,
                              ContainerService containerService, DatabaseRepository databaseRepository,
                              DatabaseidxRepository databaseidxRepository) {
        this.amqpMapper = amqpMapper;
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.containerService = containerService;
        this.databaseRepository = databaseRepository;
        this.databaseidxRepository = databaseidxRepository;
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
            QueryMalformedException {
        final Container container = containerService.find(containerId);
        final Database database = findPublicOrMineById(containerId, databaseId, principal);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container, database);
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
        databaseidxRepository.delete(database);
        log.info("Deleted database in elastic search with id {}", databaseId);
        log.trace("deleted database in elastic search {}", database);
    }

    @Override
    @Transactional
    public Database create(Long containerId, DatabaseCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, UserNotFoundException,
            DatabaseNameExistsException, DatabaseConnectionException, QueryMalformedException {
        final Container container = containerService.find(containerId);
        if (container.getDatabases().size() != 0) {
            log.error("Currently we only support one database per container.");
            throw new DatabaseMalformedException("Currently only one database per container is supported");
        }
        /* start the object */
        final Database database = databaseMapper.databaseCreateDtoToDatabase(createDto);
        database.setContainer(container);
        final ComboPooledDataSource dataSource = getDataSource(container.getImage(), container);
        try {
            /* create database */
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = databaseMapper.databaseToRawCreateDatabaseQuery(connection, database);
            preparedStatement.executeUpdate();
            /* grant read-only access */
            final PreparedStatement preparedStatement1 = databaseMapper.imageToRawGrantReadonlyAccessQuery(connection);
            preparedStatement1.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create database {}, reason: {}", database, e.getMessage());
            throw new DatabaseMalformedException("Failed to create database", e);
        } finally {
            dataSource.close();
        }
        /* save in metadata database */
        database.setExchange(amqpMapper.exchangeName(database));
        database.setIsPublic(createDto.getIsPublic());
        final User creator = userService.findByUsername(principal.getName());
        database.setCreator(creator);
        final Database dbdb = databaseRepository.save(database);
        log.info("Created database with id {}", dbdb.getId());
        log.trace("created database {}", dbdb);
        /* save in database_index - elastic search */
        final Database edb = databaseidxRepository.save(database);
        log.info("Saved database in elastic search with id {}", edb.getId());
        log.trace("saved database in elastic search {}", edb);
        return dbdb;
    }

    @Override
    @Transactional
    public Database transfer(Long containerId, Long databaseId, DatabaseTransferDto transferDto)
            throws DatabaseNotFoundException {
        /* check */
        final Database database = findById(containerId, databaseId);
        /* map */
        database.setIsPublic(transferDto.getIsPublic());
        /* update entity in metadata database */
        final Database dbdb = databaseRepository.save(database);
        log.info("Updated database with id {}", dbdb.getId());
        log.trace("updated database {}", dbdb);
        // save in database_index - elastic search
        final Database edb = databaseidxRepository.save(database);
        log.info("Updated database in elastic search with id {}", edb.getId());
        log.trace("updated database in elastic search {}", edb);
        return dbdb;
    }

}
