package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKeyReference;
import at.tuwien.entities.database.table.constraints.foreignKey.ReferenceType;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.mapper.TableMapper;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.mdb.ContainerRepository;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Log4j2
@Service
public class MariaDbServiceImpl extends HibernateConnector implements DatabaseService {

    private final ViewMapper viewMapper;
    private final QueryConfig queryConfig;
    private final QueryMapper queryMapper;
    private final TableMapper tableMapper;
    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final ContainerService containerService;
    private final DatabaseRepository databaseRepository;
    private final TableColumnService tableColumnService;
    private final ContainerRepository containerRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    public MariaDbServiceImpl(ViewMapper viewMapper, QueryConfig queryConfig, QueryMapper queryMapper,
                              TableMapper tableMapper, UserService userService, DatabaseMapper databaseMapper,
                              ContainerService containerService, DatabaseRepository databaseRepository,
                              TableColumnService tableColumnService, ContainerRepository containerRepository,
                              DatabaseIdxRepository databaseIdxRepository) {
        this.viewMapper = viewMapper;
        this.queryConfig = queryConfig;
        this.queryMapper = queryMapper;
        this.tableMapper = tableMapper;
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.containerService = containerService;
        this.databaseRepository = databaseRepository;
        this.tableColumnService = tableColumnService;
        this.containerRepository = containerRepository;
        this.databaseIdxRepository = databaseIdxRepository;
    }

    @Override
    public List<Database> findAll() {
        return databaseRepository.findAll();
    }

    @Override
    public List<Database> findAccess(UUID userId) {
        return databaseRepository.findReadAccess(userId);
    }

    @Override
    public Database find(Long databaseId) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(databaseId);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {} in metadata database", databaseId);
            throw new DatabaseNotFoundException("could not find database with id " + databaseId + " in metadata database");
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
            log.error("Failed to find database with id {} in metadata database", databaseId);
            throw new DatabaseNotFoundException("Failed to find database with id " + databaseId + " in metadata database");
        }
        return database.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Database findById(Long id) throws DatabaseNotFoundException {
        final Optional<Database> database = databaseRepository.findById(id);
        if (database.isEmpty()) {
            log.error("Failed to find database with id {} in metadata database", id);
            throw new DatabaseNotFoundException("could not find database with id " + id + " in metadata database");
        }
        return database.get();
    }

    @Override
    @Transactional
    public Database create(DatabaseCreateDto createDto, Principal principal) throws ContainerNotFoundException,
            DatabaseMalformedException, UserNotFoundException, QueryMalformedException {
        /* start the object */
        final Database database = databaseMapper.databaseCreateDtoToDatabase(createDto);
        final Container container = containerService.find(database.getCid());
        final User owner = userService.findByUsername(principal.getName());
        database.setContainer(container);
        database.setOwnedBy(owner.getId());
        database.setCreatedBy(owner.getId());
        database.setContactPerson(owner.getId());
        database.setExchangeName("dbrepo");
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
            log.error("Failed to create database/-user: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to create database/-user: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        /* save in metadata database */
        final Database entity = databaseRepository.save(database);
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Created database with id {} and saved it in the metadata database & search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public void updatePassword(User user) throws QueryMalformedException {
        /* start the object */
        final List<Database> databases = databaseRepository.findReadAccess(user.getId())
                .stream()
                .distinct()
                .toList();
        log.debug("found {} distinct databases where access for user with id {} is present", databases.size(), user.getId());
        for (Database database : databases) {
            final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer());
            try {
                final Connection connection = dataSource.getConnection();
                /* update password database */
                final PreparedStatement preparedStatement = databaseMapper.userToRawUpdateUserQuery(connection, user);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                log.error("Failed to update user password in database with internal name {}: {}", database.getInternalName(), e.getMessage());
                throw new QueryMalformedException("Failed to update user password in database with internal name " + database.getInternalName() + ": " + e.getMessage(), e);
            } finally {
                dataSource.close();
            }
            log.debug("updated user password in database with internal name {}", database.getInternalName());
        }
        log.info("Updated user password in {} database(s)", databases.size());
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
        /* update in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database visibility of database with id {} in metadata database & search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public Database transfer(Long databaseId, DatabaseTransferDto transferDto) throws DatabaseNotFoundException,
            UserNotFoundException {
        /* check */
        final Database database = findById(databaseId);
        final User user = userService.find(transferDto.getId());
        /* update in metadata database */
        database.setOwnedBy(user.getId());
        final Database entity = databaseRepository.save(database);
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database owner of database with id {} in metadata database & search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public Database modifyImage(Long databaseId, byte[] image) throws DatabaseNotFoundException {
        /* check */
        final Database database = findById(databaseId);
        /* update in metadata database */
        database.setImage(image);
        final Database entity = databaseRepository.save(database);
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database owner of database with id {} in metadata database & search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public Database obtainConstraints(Long databaseId) throws DatabaseNotFoundException, QueryMalformedException,
            TableMalformedException {
        /* check */
        final Database database = findById(databaseId);
        final List<Table> diffTables = database.getTables()
                .stream()
                .filter(t -> !t.getProcessedConstraints())
                .toList();
        /* obtain constraints */
        log.info("Database with id {} contains {} table(s) with unknown constraint(s)", databaseId, diffTables.size());
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            for (Table table : diffTables) {
                final PreparedStatement preparedStatement = queryMapper.databaseToDatabaseConstraintMetadata(connection, table.getDatabase().getInternalName(), table.getInternalName());
                final Constraints constraints = resultSetTableToObtainedConstraintsMetadata(databaseId, table, preparedStatement.executeQuery());
                table.setConstraints(constraints);
                table.setProcessedConstraints(true);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain constraint information in database with id {}: {}", database.getId(), e.getMessage());
            throw new QueryMalformedException("Failed to obtain constraint information in database with id " + database.getId() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        /* update in metadata database */
        final Database entity = databaseRepository.save(database);
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database with id {} in metadata database & search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public Database obtainTablesMetadata(Long databaseId) throws DatabaseNotFoundException, QueryMalformedException,
            ColumnParseException {
        /* check */
        final Database database = findById(databaseId);
        final List<Table> diffTables;
        final List<Table> knownTables;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement0 = databaseMapper.databaseToDatabaseMetadata(connection, database);
            final List<Table> tables = tableMapper.resultListToTableList(preparedStatement0.executeQuery(), database);
            diffTables = tables.stream()
                    .filter(obtainedTable -> database.getTables()
                            .stream()
                            .noneMatch(t -> t.getInternalName().equals(obtainedTable.getInternalName())))
                    .toList();
            knownTables = tables.stream()
                    .filter(table -> diffTables.stream()
                            .noneMatch(t -> t.getInternalName().equals(table.getInternalName())))
                    .map(obtainedTable -> {
                        final Optional<Table> optional = database.getTables()
                                .stream()
                                .filter(t -> t.getInternalName().equals(obtainedTable.getInternalName()))
                                .findFirst();
                        if (optional.isPresent()) {
                            final Table table = optional.get();
                            table.setNumRows(obtainedTable.getNumRows());
                            table.setDataLength(obtainedTable.getDataLength());
                            table.setMaxDataLength(obtainedTable.getMaxDataLength());
                            table.setAvgRowLength(obtainedTable.getAvgRowLength());
                            return table;
                        }
                        return obtainedTable;
                    })
                    .toList();
            /* default times */
            final Optional<ContainerImageDate> defaultDateFormat = containerRepository.findDefaultDateFormat();
            if (defaultDateFormat.isEmpty()) {
                log.error("Failed to find default date format in metadata database");
                throw new ColumnParseException("Failed to find default date format in metadata database");
            }
            final Optional<ContainerImageDate> defaultTimestampFormat = containerRepository.findDefaultTimestampFormat();
            if (defaultTimestampFormat.isEmpty()) {
                log.error("Failed to find default timestamp format in metadata database");
                throw new ColumnParseException("Failed to find default timestamp format in metadata database");
            }
            /* obtain table schema */
            log.info("Database with id {} contains {} unknown table(s)", databaseId, diffTables.size());
            log.debug("database with id {} misses table(s) in metadata database: {}", databaseId, diffTables.stream().map(Table::getInternalName).toList());
            database.getTables().replaceAll(table -> {
                final Optional<Table> optional = knownTables.stream()
                        .filter(t -> t.getId().equals(table.getId()))
                        .findFirst();
                if (optional.isPresent()) {
                    log.trace("found table with id {} and merged it", table.getId());
                    return optional.get();
                }
                return table;
            });
            for (Table table : diffTables) {
                final PreparedStatement preparedStatement1 = queryMapper.obtainTableMetadataRawQuery(connection, table.getDatabase().getInternalName(), table.getInternalName());
                table = tableMapper.resultSetTableToObtainedMetadata(preparedStatement1.executeQuery(), table,
                        defaultDateFormat.get(), defaultTimestampFormat.get());
                if (!table.getIsVersioned()) {
                    log.debug("table with name {} is not system-versioned", table.getInternalName());
                    final PreparedStatement preparedStatement2 = queryMapper.tableEnableSystemVersioning(connection, table.getDatabase().getInternalName(), table.getInternalName());
                    preparedStatement2.execute();
                    log.info("Enabled system-versioning for table with name {}", table.getInternalName());
                }
                table.setProcessedConstraints(false);
                final PreparedStatement preparedStatement3 = tableMapper.tableToCreateHistoryViewRawQuery(connection, table);
                preparedStatement3.executeUpdate();
                database.getTables().add(table);
            }
        } catch (SQLException e) {
            log.error("Failed to obtain schema information in database with id {}: {}", database.getId(), e.getMessage());
            throw new QueryMalformedException("Failed to obtain schema information in database with id " + database.getId() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        /* update in metadata database */
        final Database entity = databaseRepository.save(database);
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database with id {} in metadata database & search database", entity.getId());
        return entity;
    }

    @Override
    @Transactional
    public Database obtainViewsMetadata(Long databaseId) throws DatabaseNotFoundException, QueryMalformedException,
            ColumnParseException {
        /* check */
        final Database database = findById(databaseId);
        final List<View> diffViews;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement0 = databaseMapper.databaseToDatabaseMetadata(connection, database);
            final List<View> views = tableMapper.resultListToViewList(preparedStatement0.executeQuery(), database);
            diffViews = views.stream()
                    .filter(view -> database.getViews()
                            .stream()
                            .noneMatch(v -> v.getInternalName().equals(view.getInternalName())))
                    .toList();
            /* obtain table schema */
            log.info("Database with id {} contains {} unknown view(s)", databaseId, diffViews.size());
            /* default times */
            final Optional<ContainerImageDate> defaultDateFormat = containerRepository.findDefaultDateFormat();
            if (defaultDateFormat.isEmpty()) {
                log.error("Failed to find default date format in metadata database");
                throw new ColumnParseException("Failed to find default date format in metadata database");
            }
            final Optional<ContainerImageDate> defaultTimestampFormat = containerRepository.findDefaultTimestampFormat();
            if (defaultTimestampFormat.isEmpty()) {
                log.error("Failed to find default timestamp format in metadata database");
                throw new ColumnParseException("Failed to find default timestamp format in metadata database");
            }
        } catch (SQLException e) {
            log.error("Failed to obtain schema information in database with id {}: {}", database.getId(), e.getMessage());
            throw new QueryMalformedException("Failed to obtain schema information in database with id " + database.getId() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        /* obtain view schema */
        log.debug("database with id {} misses view(s) in metadata database: {}", databaseId, diffViews.stream().map(View::getInternalName).toList());
        for (View view : diffViews) {
            try {
                view.setColumns(viewMapper.tableColumnsToViewColumns(view, queryMapper.parseColumns(view.getQuery(), database)));
            } catch (JSQLParserException e) {
                log.error("Failed to map/parse columns: {}", e.getMessage());
                throw new ColumnParseException("Failed to map/parse columns: " + e.getMessage(), e);
            }
            if (view.getColumns().stream().anyMatch(c -> c.getColumn().getId() == null)) {
                log.warn("Skipping creation of view {}: referenced columns does not exist in metadata database", view.getInternalName());
                continue;
            }
            database.getViews()
                    .add(view);
        }
        /* update in metadata database */
        final Database entity = databaseRepository.save(database);
        /* save in open search database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(entity));
        log.info("Updated database with id {} in metadata database & search database", entity.getId());
        return entity;
    }

    @Transactional(readOnly = true)
    public Constraints resultSetTableToObtainedConstraintsMetadata(Long databaseId, Table table, ResultSet resultSet)
            throws SQLException, DatabaseNotFoundException, TableMalformedException {
        final Database database = find(databaseId);
        final Set<String> checks = new LinkedHashSet<>();
        final List<Unique> uniques = new LinkedList<>();
        final List<ForeignKey> foreignKeys = new LinkedList<>();
        while (resultSet.next()) {
            if (resultSet.getString(1).equals("CHECK")) {
                /* check constraints */
                checks.add(resultSet.getString(4));
            } else if (resultSet.getString(1).equals("FOREIGN KEY")) {
                /* foreign key constraints */
                final List<ForeignKeyReference> foreignKeyReferences = new LinkedList<>();
                final String foreignKeyName = resultSet.getString(2);
                if (foreignKeys.stream().anyMatch(fk -> fk.getName().equals(foreignKeyName))) {
                    final Optional<ForeignKey> optional = foreignKeys.stream()
                            .filter(fk -> fk.getName().equals(foreignKeyName))
                            .findFirst();
                    if (optional.isEmpty()) {
                        /* should never happen */
                        continue;
                    }
                    final ForeignKey foreignKey = optional.get();
                    foreignKey.getReferences()
                            .add(queryMapper.foreignKeyToForeignKeyReference(foreignKey,
                                    tableColumnService.findColumn(database, resultSet.getString(6), resultSet.getString(8)),
                                    tableColumnService.findColumn(table, resultSet.getString(7))));
                }
                final ForeignKey foreignKey;
                try {
                    foreignKey = ForeignKey.builder()
                            .name(foreignKeyName)
                            .table(table)
                            .referencedTable(find(database, resultSet.getString(6)))
                            .references(foreignKeyReferences)
                            .onDelete(ReferenceType.NO_ACTION)
                            .onUpdate(ReferenceType.NO_ACTION)
                            .build();
                } catch (TableNotFoundException e) {
                    /* ignore */
                    return null;
                }
                final ForeignKeyReference fk = ForeignKeyReference.builder()
                        .foreignKey(foreignKey)
                        .column(tableColumnService.findColumn(table, resultSet.getString(7)))
                        .referencedColumn(tableColumnService.findColumn(database, resultSet.getString(6), resultSet.getString(8)))
                        .build();
                foreignKey.setReferences(List.of(fk));
                foreignKeys.add(foreignKey);
            } else if (resultSet.getString(1).equals("UNIQUE")) {
                /* unique constraints */
                final String uniqueConstraintName = resultSet.getString(1);
                final Optional<Unique> optional = uniques.stream().filter(u -> u.getName().equals(uniqueConstraintName)).findFirst();
                if (optional.isPresent()) {
                    log.debug("unique constraint {} already present: add column", uniqueConstraintName);
                    optional.get()
                            .getColumns()
                            .add(tableColumnService.findColumn(table, resultSet.getString(7)));
                    continue;
                }
                final List<TableColumn> columns = new LinkedList<>();
                columns.add(tableColumnService.findColumn(table, resultSet.getString(7)));
                final Unique uk = Unique.builder()
                        .name(uniqueConstraintName)
                        .table(table)
                        .columns(columns)
                        .build();
                uniques.add(uk);
            }
        }
        final Constraints constraints = Constraints.builder()
                .uniques(uniques)
                .checks(checks)
                .foreignKeys(foreignKeys)
                .build();
        log.debug("mapped result set to {} check,- {} unique- & {} foreign key constraint(s)",
                constraints.getChecks().size(), constraints.getUniques().size(), constraints.getForeignKeys().size());
        log.trace("mapped result set to constraints: {}", constraints);
        return constraints;
    }

    public Table find(Database database, String internalName) throws DatabaseNotFoundException, TableNotFoundException {
        final Optional<Table> table = database.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(internalName))
                .findFirst();
        if (table.isEmpty()) {
            log.error("Failed to find table with internal name {} in metadata database", internalName);
            throw new TableNotFoundException("Failed to find table with internal name " + internalName + " in metadata database");
        }
        return table.get();
    }

}
