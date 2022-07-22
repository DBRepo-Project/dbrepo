package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.License;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.AmqpMapper;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.elastic.DatabaseidxRepository;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.LicenseService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class MariaDbServiceImpl extends HibernateConnector implements DatabaseService {

    private final AmqpMapper amqpMapper;
    private final UserService userService;
    private final LicenseService licenseService;
    private final DatabaseMapper databaseMapper;
    private final RabbitMqServiceImpl amqpService;
    private final ContainerService containerService;
    private final DatabaseRepository databaseRepository;
    private final ContainerRepository containerRepository;
    private final DatabaseidxRepository databaseidxRepository;

    @Autowired
    public MariaDbServiceImpl(AmqpMapper amqpMapper, UserService userService, LicenseService licenseService,
                              DatabaseMapper databaseMapper, RabbitMqServiceImpl amqpService,
                              ContainerService containerService, DatabaseRepository databaseRepository,
                              ContainerRepository containerRepository, DatabaseidxRepository databaseidxRepository) {
        this.amqpMapper = amqpMapper;
        this.userService = userService;
        this.licenseService = licenseService;
        this.databaseMapper = databaseMapper;
        this.amqpService = amqpService;
        this.containerService = containerService;
        this.databaseRepository = databaseRepository;
        this.containerRepository = containerRepository;
        this.databaseidxRepository = databaseidxRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public List<Database> findAllPublic(Long containerId) {
        return databaseRepository.findAllByPublicAndContainerId(containerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Database> findAllPublicOrMine(Long containerId, Principal principal) {
        return databaseRepository.findAllByPublicAndContainerIdOrMine(containerId, principal.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Database findPublicOrMineById(Long containerId, Long databaseId, Principal principal)
            throws DatabaseNotFoundException {
        final Optional<Database> database;
        if (principal == null) {
            database = databaseRepository.findPublic(containerId, databaseId);
        } else {
            database = databaseRepository.findPublicOrMine(containerId, databaseId, principal.getName());
        }
        if (database.isEmpty()) {
            log.warn("could not find database with id {}", databaseId);
            throw new DatabaseNotFoundException("could not find database with this id");
        }
        return database.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Database findById(Long id, Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(databaseId);
        if (database.isEmpty()) {
            log.warn("could not find database with id {}", databaseId);
            throw new DatabaseNotFoundException("could not find database with this id");
        }
        return database.get();
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseMalformedException, ContainerConnectionException, AmqpException,
            ContainerNotFoundException {
        final Container container = containerService.find(containerId);
        final Database database = findPublicOrMineById(containerId, databaseId, principal);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final Session session = getCurrentSession(container.getImage(), container, database);
        final Transaction transaction = session.beginTransaction();
        final NativeQuery<?> query = session.createSQLQuery(databaseMapper.databaseToRawDeleteDatabaseQuery(database));
        try (session) {
            log.debug("query affected {} rows", query.executeUpdate());
            transaction.commit();
        } catch (ServiceException e) {
            log.error("Failed to delete database.");
            throw new DatabaseMalformedException("Failed to delete database", e);
        }
        database.setDeleted(Instant.now()) /* method has void, only for debug logs */;
        /* save in metadata database */
        databaseRepository.deleteById(databaseId);
        log.info("Deleted database with id {}", databaseId);
        log.debug("deleted database {}", database);
        amqpService.deleteExchange(database);
        log.debug("deleted exchange {}", database.getExchange());
    }

    @Override
    @Transactional
    public Database create(Long containerId, DatabaseCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, UserNotFoundException {
        final Container container = containerService.find(containerId);
        /* start the object */
        final Database database = new Database();
        database.setName(createDto.getName());
        database.setInternalName(databaseMapper.nameToInternalName(database.getName()));
        database.setContainer(container);
        /* run query */
        final Session session = getCurrentSession(container.getImage(), container, database);
        final Transaction transaction = session.beginTransaction();
        final NativeQuery<?> query = session.createSQLQuery(databaseMapper.databaseToRawCreateDatabaseQuery(database));
        try {
            log.debug("query affected {} rows", query.executeUpdate());
        } catch (PersistenceException e) {
            log.error("Failed to delete database.");
            session.close();
            throw new DatabaseMalformedException("Failed to delete database", e);
        }
        final NativeQuery<?> grant = session.createSQLQuery(databaseMapper.imageToRawGrantReadonlyAccessQuery());
        try (session) {
            log.debug("grant affected {} rows", grant.executeUpdate());
            transaction.commit();
        } catch (PersistenceException e) {
            log.error("Failed to grant privileges.");
            throw new DatabaseMalformedException("Failed to grant privileges", e);
        }
        /* save in metadata database */
        database.setExchange(amqpMapper.exchangeName(database));
        database.setDescription(createDto.getDescription());
        database.setIsPublic(createDto.getIsPublic());
        final User creator = userService.findByUsername(principal.getName());
        database.setCreator(creator);
        final Database out = databaseRepository.save(database);
        log.info("Created database with id {}", out.getId());
        log.debug("created database {}", out);
        // save in database_index - elastic search
        databaseidxRepository.save(database);
        amqpService.createExchange(database, principal);
        log.debug("created exchange {}", database.getExchange());
        return out;
    }

    @Override
    @Transactional
    public Database modify(Long containerId, Long databaseId, DatabaseModifyDto modifyDto)
            throws UserNotFoundException, DatabaseNotFoundException, LicenseNotFoundException {
        final Database database = findById(containerId, databaseId);
        if (modifyDto.getContactPerson() != null) {
            database.setContact(userService.findByUsername(modifyDto.getContactPerson()));
        }
        final License license = licenseService.find(modifyDto.getLicense().getIdentifier());
        database.setIsPublic(modifyDto.getIsPublic());
        database.setDescription(modifyDto.getDescription());
        database.setPublisher(modifyDto.getPublisher());
        database.setPublication(modifyDto.getPublication());
        database.setLanguage(databaseMapper.languageTypeDtoToLanguageType(modifyDto.getLanguage()));
        database.setLicense(license);
        final Database out = databaseRepository.save(database);
        /* update entity in metadata database */
        log.info("Updated database with id {}", out.getId());
        log.debug("updated database {}", out);
        // save in database_index - elastic search
//        databaseidxRepository.save(database);
        return out;
    }

}
