package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.*;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKeyReference;
import at.tuwien.entities.database.table.constraints.primaryKey.PrimaryKey;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.service.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Log4j2
@Service
public class DatabaseServiceImpl implements DatabaseService {

    private final MetadataMapper metadataMapper;
    private final ContainerService containerService;
    private final DatabaseRepository databaseRepository;
    private final DataServiceGateway dataServiceGateway;
    private final SearchServiceGateway searchServiceGateway;

    @Autowired
    public DatabaseServiceImpl(MetadataMapper metadataMapper, ContainerService containerService,
                               DatabaseRepository databaseRepository, DataServiceGateway dataServiceGateway,
                               SearchServiceGateway searchServiceGateway) {
        this.metadataMapper = metadataMapper;
        this.containerService = containerService;
        this.databaseRepository = databaseRepository;
        this.dataServiceGateway = dataServiceGateway;
        this.searchServiceGateway = searchServiceGateway;
    }

    @Override
    public List<Database> findAll() {
        return databaseRepository.findAllDesc();
    }

    @Override
    public List<Database> findAllAccess(UUID userId) {
        return databaseRepository.findReadAccess(userId);
    }

    @Override
    public Database findByInternalName(String internalName) throws DatabaseNotFoundException {
        log.trace("find database by internal name: {}", internalName);
        final Optional<Database> database = databaseRepository.findByInternalName(internalName);
        if (database.isEmpty()) {
            log.error("Failed to find database with internal name {} in metadata database", internalName);
            throw new DatabaseNotFoundException("Failed to find database in metadata database");
        }
        return database.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Database findById(Long id) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(id);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {} in metadata database", id);
            throw new DatabaseNotFoundException("Failed to find database in metadata database");
        }
        return database.get();
    }

    @Override
    @Transactional
    public Database create(Container container, DatabaseCreateDto data, User user) throws UserNotFoundException,
            ContainerNotFoundException, DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        Database database = Database.builder()
                .isPublic(data.getIsPublic())
                .name(data.getName())
                .internalName(metadataMapper.nameToInternalName(data.getName()) + "_" + RandomStringUtils.randomAlphabetic(4).toLowerCase())
                .cid(data.getCid())
                .container(container)
                .ownedBy(user.getId())
                .owner(user)
                .createdBy(user.getId())
                .creator(user)
                .contactPerson(user.getId())
                .contact(user)
                .tables(new LinkedList<>())
                .views(new LinkedList<>())
                .accesses(new LinkedList<>())
                .identifiers(new LinkedList<>())
                .build();
        /* create in data database */
        final CreateDatabaseDto payload = CreateDatabaseDto.builder()
                .containerId(data.getCid())
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getMariadbPassword())
                .privilegedUsername(container.getPrivilegedUsername())
                .privilegedPassword(container.getPrivilegedPassword())
                .internalName(database.getInternalName())
                .build();
        final DatabaseDto dto = dataServiceGateway.createDatabase(payload);
        database.setExchangeName(dto.getExchangeName());
        /* create in metadata database */
        database = databaseRepository.save(database);
        database.getAccesses()
                .add(DatabaseAccess.builder()
                        .type(AccessType.WRITE_ALL)
                        .hdbid(database.getId())
                        .database(database)
                        .huserid(user.getId())
                        .user(user)
                        .build());
        database = databaseRepository.save(database);
        /* create in search service */
        searchServiceGateway.update(database);
        log.info("Created database with id {}", database.getId());
        return database;
    }

    @Override
    @Transactional(readOnly = true)
    public void updatePassword(Database database, User user) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException {
        final List<Database> databases = databaseRepository.findReadAccess(user.getId())
                .stream()
                .distinct()
                .toList();
        log.debug("found {} distinct databases where access for user with id {} is present", databases.size(), user.getId());
        final UpdateUserPasswordDto payload = UpdateUserPasswordDto.builder()
                .username(user.getUsername())
                .password(user.getMariadbPassword())
                .build();
        dataServiceGateway.updateDatabase(database.getId(), payload);
        log.info("Updated user password in database with id {}", database.getId());
    }

    @Override
    @Transactional
    public Database modifyVisibility(Database database, DatabaseModifyVisibilityDto data)
            throws DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {
        /* update in metadata database */
        database.setIsPublic(data.getIsPublic());
        database = databaseRepository.save(database);
        /* update in open search service */
        searchServiceGateway.update(database);
        log.info("Updated database visibility of database with id {}", database.getId());
        return database;
    }

    @Override
    @Transactional
    public Database modifyOwner(Database database, User user) throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* update in metadata database */
        database.setOwnedBy(user.getId());
        database = databaseRepository.save(database);
        /* save in search service */
        searchServiceGateway.update(database);
        log.info("Updated database owner of database with id {}", database);
        return database;
    }

    @Override
    @Transactional
    public Database modifyImage(Database database, byte[] image) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        /* update in metadata database */
        database.setImage(image);
        database = databaseRepository.save(database);
        /* save in search service */
        searchServiceGateway.update(database);
        log.info("Updated database owner of database with id {} & search database", database.getId());
        return database;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Database updateTableMetadata(Database database) throws DatabaseNotFoundException, DataServiceException,
            SearchServiceException, SearchServiceConnectionException, DataServiceConnectionException,
            MalformedException, TableNotFoundException {
        for (TableDto table : dataServiceGateway.getTableSchemas(database.getId())) {
            if (database.getTables().stream().anyMatch(t -> t.getInternalName().equals(table.getInternalName()))) {
                log.debug("fetched known table from data service: {}.{}", database.getInternalName(), table.getInternalName());
                continue;
            }
            log.debug("fetched unknown table from data service: {}.{}", database.getInternalName(), table.getInternalName());
            final Table tableEntity = metadataMapper.tableDtoToTable(table);
            tableEntity.setDatabase(database);
            tableEntity.getColumns()
                    .forEach(column -> {
                        column.setTable(tableEntity);
                    });
            /* map unique constraint(s) */
            tableEntity.getConstraints()
                    .getUniques()
                    .forEach(uk -> {
                        uk.setTable(tableEntity);
                        uk.getColumns()
                                .forEach(column -> {
                                    column.setTable(tableEntity);
                                });
                    });
            /* map foreign key constraint(s) */
            tableEntity.getConstraints()
                    .getForeignKeys()
                    .forEach(fk -> {
                        fk.setTable(tableEntity);
                    });
            /* map primary key constraint */
            for (PrimaryKey key : tableEntity.getConstraints().getPrimaryKey()) {
                final Optional<TableColumn> optional = tableEntity.getColumns()
                        .stream()
                        .filter(c -> c.getInternalName().equals(key.getColumn().getInternalName()))
                        .findFirst();
                if (optional.isEmpty()) {
                    log.error("Failed to find primary key column {} in table {}.{}", key.getColumn().getInternalName(), database.getInternalName(), table.getInternalName());
                    throw new MalformedException("Failed to find primary key column: " + key.getColumn().getInternalName());
                }
                key.setTable(tableEntity);
                key.setColumn(optional.get());
            }
            database.getTables()
                    .add(tableEntity);
        }
        /* update referenced tables after they are known to the service */
        for (ForeignKey foreignKey : database.getTables().stream().map(t -> t.getConstraints().getForeignKeys()).flatMap(List::stream).toList()) {
            log.trace("lookup table {} in tables: {}", foreignKey.getReferencedTable().getInternalName(), database.getTables().stream().map(Table::getInternalName).toList());
            final Optional<Table> optional = database.getTables()
                    .stream()
                    .filter(t -> t.getInternalName().equals(foreignKey.getReferencedTable().getInternalName()))
                    .findFirst();
            if (optional.isEmpty()) {
                log.error("Failed to find referenced table: {}.{}", database.getInternalName(), foreignKey.getReferencedTable().getInternalName());
                throw new IllegalArgumentException("Failed to find referenced table: " + database.getInternalName() + "." + foreignKey.getReferencedTable().getInternalName());
            }
            foreignKey.setReferencedTable(optional.get());
            for (ForeignKeyReference reference : foreignKey.getReferences()) {
                reference.setForeignKey(foreignKey);
                final Optional<TableColumn> optional1 = database.getTables()
                        .stream()
                        .filter(t -> t.getInternalName().equals(foreignKey.getTable().getInternalName()))
                        .map(Table::getColumns)
                        .flatMap(List::stream)
                        .filter(c -> c.getInternalName().equals(reference.getColumn().getInternalName()))
                        .findFirst();
                if (optional1.isEmpty()) {
                    log.error("Failed to find foreign key column: {}.{}.{}", database.getInternalName(), foreignKey.getTable().getInternalName(), reference.getColumn().getInternalName());
                    throw new IllegalArgumentException("Failed to find foreign key column: " + reference.getColumn().getInternalName());
                }
                reference.setColumn(optional1.get());
                final Optional<TableColumn> optional2 = database.getTables()
                        .stream()
                        .filter(t -> t.getInternalName().equals(foreignKey.getReferencedTable().getInternalName()))
                        .map(Table::getColumns)
                        .flatMap(List::stream)
                        .filter(c -> c.getInternalName().equals(reference.getReferencedColumn().getInternalName()))
                        .findFirst();
                if (optional2.isEmpty()) {
                    log.error("Failed to find foreign key referenced column: {}", reference.getReferencedColumn().getInternalName());
                    throw new IllegalArgumentException("Failed to find foreign key referenced column: " + reference.getReferencedColumn().getInternalName());
                }
                reference.setReferencedColumn(optional2.get());
            }
        }
        database.getTables()
                .stream()
                .filter(t -> t.getConstraints().getForeignKeys().size() > 0)
                .map(t -> t.getConstraints().getForeignKeys())
                .flatMap(List::stream)
                .filter(fk -> fk.getReferences().size() > 1)
                .forEach(fk -> {
                    log.debug("");
                });
        /* update in metadata database */
        database = databaseRepository.save(database);
        /* save in search service */
        searchServiceGateway.update(database);
        log.info("Updated table metadata of database with id {} & search database", database.getId());
        return database;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Database updateViewMetadata(Database database) throws DatabaseNotFoundException, DataServiceException,
            SearchServiceException, SearchServiceConnectionException, DataServiceConnectionException,
            ViewNotFoundException {
        for (ViewDto view : dataServiceGateway.getViewSchemas(database.getId())) {
            if (database.getViews().stream().anyMatch(v -> v.getInternalName().equals(view.getInternalName()))) {
                log.debug("fetched known view from data service: {}.{}", database.getInternalName(), view.getInternalName());
                continue;
            }
            log.debug("fetched unknown view from data service: {}.{}", database.getInternalName(), view.getInternalName());
            final View viewEntity = metadataMapper.viewDtoToView(view);
            viewEntity.setDatabase(database);
            for (ViewColumn column : viewEntity.getColumns()) {
                column.setView(viewEntity);
            }
            database.getViews()
                    .add(viewEntity);
        }
        /* update in metadata database */
        database = databaseRepository.save(database);
        /* save in search service */
        searchServiceGateway.update(database);
        log.info("Updated view metadata of database with id {} & search database", database.getId());
        return database;
    }

}
