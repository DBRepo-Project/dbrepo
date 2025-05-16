package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ViewColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKeyReference;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.primaryKey.PrimaryKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.repository.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class DatabaseServiceImpl implements DatabaseService {

    private final MetadataMapper metadataMapper;
    private final DatabaseRepository databaseRepository;
    private final DataServiceGateway dataServiceGateway;
    private final SearchServiceGateway searchServiceGateway;

    @Autowired
    public DatabaseServiceImpl(MetadataMapper metadataMapper, DatabaseRepository databaseRepository,
                               DataServiceGateway dataServiceGateway, SearchServiceGateway searchServiceGateway) {
        this.metadataMapper = metadataMapper;
        this.databaseRepository = databaseRepository;
        this.dataServiceGateway = dataServiceGateway;
        this.searchServiceGateway = searchServiceGateway;
    }

    @Override
    public List<Database> findAll() {
        return databaseRepository.findAllDesc();
    }

    @Override
    public List<Database> findAllPublicOrSchemaPublic() {
        return databaseRepository.findAllPublicOrSchemaPublicDesc();
    }

    @Override
    public List<Database> findByInternalName(String internalName) {
        return databaseRepository.findAllByInternalNameDesc(internalName);
    }

    @Override
    public List<Database> findAllPublicOrSchemaPublicOrReadAccessByInternalName(UUID userId, String internalName) {
        return databaseRepository.findAllPublicOrSchemaPublicOrReadAccessByInternalNameDesc(userId, internalName);
    }

    @Override
    public List<Database> findAllPublicOrSchemaPublicOrReadAccess(UUID userId) {
        return databaseRepository.findAllPublicOrSchemaPublicOrReadAccessDesc(userId);
    }

    @Override
    public List<Database> findAllPublicOrSchemaPublicByInternalName(String internalName) {
        return databaseRepository.findAllPublicOrSchemaPublicByInternalNameDesc(internalName);
    }

    @Override
    @Transactional(readOnly = true)
    public Database findById(UUID id) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(id);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {} in metadata database", id);
            throw new DatabaseNotFoundException("Failed to find database in metadata database");
        }
        return database.get();
    }

    @Override
    @Transactional
    public Database create(Container container, CreateDatabaseDto data, User user, List<User> internalUsers)
            throws DataServiceException, SearchServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException {
        final Database entity = Database.builder()
                .isPublic(data.getIsPublic())
                .isSchemaPublic(data.getIsSchemaPublic())
                .isDashboardEnabled(true)
                .name(data.getName())
                .internalName(metadataMapper.nameToInternalName(data.getName()) + "_" + RandomStringUtils.randomAlphabetic(4).toLowerCase())
                .cid(data.getCid())
                .container(container)
                .ownedBy(user.getId())
                .owner(user)
                .contactPerson(user.getId())
                .contact(user)
                .tables(new LinkedList<>())
                .views(new LinkedList<>())
                .accesses(new LinkedList<>())
                .identifiers(new LinkedList<>())
                .build();
        /* create in data database */
        final at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto payload = at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto.builder()
                .containerId(data.getCid())
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getMariadbPassword())
                .privilegedUsername(container.getPrivilegedUsername())
                .privilegedPassword(container.getPrivilegedPassword())
                .readonlyUsername(container.getReadonlyUsername())
                .readonlyPassword(container.getReadonlyPassword())
                .internalName(entity.getInternalName())
                .build();
        final DatabaseDto dto = dataServiceGateway.createDatabase(payload);
        entity.setExchangeName(dto.getExchangeName());
        /* create in metadata database */
        final Database entity1 = databaseRepository.save(entity);
        entity1.getAccesses()
                .add(metadataMapper.userToWriteAllAccess(entity1, user));
        final Database database = databaseRepository.save(entity1);
        /* create in search service */
        searchServiceGateway.update(database);
        log.info("Created database with id {}", database.getId());
        return database;
    }

    @Override
    @Transactional(readOnly = true)
    public void updatePassword(Database database, User user) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException {
        final List<Database> databases = databaseRepository.findAllAtLestReadAccessDesc(user.getId())
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
        database.setIsSchemaPublic(data.getIsSchemaPublic());
        database.setIsDashboardEnabled(data.getIsDashboardEnabled());
        log.debug("visibility change affects {} table(s)", database.getTables().stream().filter(t -> !t.getIsSchemaPublic().equals(data.getIsSchemaPublic())).count());
        database.getTables()
                .forEach(table -> table.setIsSchemaPublic(data.getIsSchemaPublic()));
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
        database.setOwner(user);
        database.setOwnedBy(user.getId());
        database.setContact(user);
        database.setContactPerson(user.getId());
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
    @Transactional
    public Database modifyDashboard(Database database, String uid) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        /* update in metadata database */
        database.setDashboardUid(uid);
        database = databaseRepository.save(database);
        /* save in search service */
        searchServiceGateway.update(database);
        log.info("Updated database dashboard uid of database with id {} & search database", database.getId());
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
            tableEntity.setIsPublic(database.getIsPublic());
            tableEntity.setIsSchemaPublic(database.getIsSchemaPublic());
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
                        log.trace("mapped unique constraint: {} ({})", tableEntity.getName(), uk.getColumns().stream().map(TableColumn::getInternalName).toList());
                    });
            /* map foreign key constraint(s) */
            tableEntity.getConstraints()
                    .getForeignKeys()
                    .forEach(fk -> {
                        fk.setTable(tableEntity);
                        log.trace("mapped foreign key constraint: {} ({}) -> {} ({})", fk.getTable().getInternalName(), fk.getReferences().stream().map(r -> r.getColumn().getInternalName()).toList(), fk.getReferencedTable().getInternalName(), fk.getReferences().stream().map(r -> r.getReferencedColumn().getInternalName()).toList());
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
        /* correct the unique constraint columns */
        for (Table table : database.getTables()) {
            for (Unique uniqueConstraint : table.getConstraints().getUniques()) {
                uniqueConstraint.setColumns(new LinkedList<>(uniqueConstraint.getColumns()
                        .stream()
                        .map(column -> {
                            final Optional<TableColumn> optional = table.getColumns()
                                    .stream()
                                    .filter(c -> c.getInternalName().equals(column.getInternalName()))
                                    .findFirst();
                            if (optional.isEmpty()) {
                                log.error("Failed to find unique constraint column: {}", column.getInternalName());
                                throw new IllegalArgumentException("Failed to find unique constraint column: " + column.getInternalName());
                            }
                            return optional.get();
                        })
                        .toList()));
            }
        }
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
