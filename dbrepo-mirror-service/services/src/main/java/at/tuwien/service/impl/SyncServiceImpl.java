package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.mapper.*;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.*;
import at.tuwien.service.SyncService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
public class SyncServiceImpl implements SyncService {

    private final UserMapper userMapper;
    private final ViewMapper viewMapper;
    private final TableMapper tableMapper;
    private final OntologyMapper ontologyMapper;
    private final DatabaseMapper databaseMapper;
    private final ViewRepository viewRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final TableRepository tableRepository;
    private final IdentifierMapper identifierMapper;
    private final ConceptRepository conceptRepository;
    private final UnitIdxRepository unitIdxRepository;
    private final UserIdxRepository userIdxRepository;
    private final ViewIdxRepository viewIdxRepository;
    private final DatabaseRepository databaseRepository;
    private final TableIdxRepository tableIdxRepository;
    private final IdentifierRepository identifierRepository;
    private final ConceptIdxRepository conceptIdxRepository;
    private final DatabaseIdxRepository databaseIdxRepository;
    private final TableColumnRepository tableColumnRepository;
    private final IdentifierIdxRepository identifierIdxRepository;
    private final TableColumnIdxRepository tableColumnIdxRepository;

    @Autowired
    public SyncServiceImpl(UserMapper userMapper, ViewMapper viewMapper, TableMapper tableMapper,
                           OntologyMapper ontologyMapper, DatabaseMapper databaseMapper, ViewRepository viewRepository,
                           UnitRepository unitRepository, UserRepository userRepository, TableRepository tableRepository,
                           IdentifierMapper identifierMapper, ConceptRepository conceptRepository,
                           UnitIdxRepository unitIdxRepository, UserIdxRepository userIdxRepository,
                           ViewIdxRepository viewIdxRepository, DatabaseRepository databaseRepository,
                           TableIdxRepository tableIdxRepository, IdentifierRepository identifierRepository,
                           ConceptIdxRepository conceptIdxRepository, DatabaseIdxRepository databaseIdxRepository,
                           TableColumnRepository tableColumnRepository, IdentifierIdxRepository identifierIdxRepository,
                           TableColumnIdxRepository tableColumnIdxRepository) {
        this.userMapper = userMapper;
        this.viewMapper = viewMapper;
        this.tableMapper = tableMapper;
        this.ontologyMapper = ontologyMapper;
        this.databaseMapper = databaseMapper;
        this.viewRepository = viewRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.tableRepository = tableRepository;
        this.identifierMapper = identifierMapper;
        this.conceptRepository = conceptRepository;
        this.unitIdxRepository = unitIdxRepository;
        this.userIdxRepository = userIdxRepository;
        this.viewIdxRepository = viewIdxRepository;
        this.databaseRepository = databaseRepository;
        this.tableIdxRepository = tableIdxRepository;
        this.identifierRepository = identifierRepository;
        this.conceptIdxRepository = conceptIdxRepository;
        this.databaseIdxRepository = databaseIdxRepository;
        this.tableColumnRepository = tableColumnRepository;
        this.identifierIdxRepository = identifierIdxRepository;
        this.tableColumnIdxRepository = tableColumnIdxRepository;
    }

    @Override
    @Transactional
    public void start() {
        /* concepts */
        final List<ConceptDto> concepts = conceptRepository.findAll()
                .stream()
                .map(ontologyMapper::tableColumnConceptToConceptDto)
                .toList();
        conceptIdxRepository.saveAll(concepts);
        log.debug("saved {} concepts to open search database", concepts.size());
        /* databases */
        final List<DatabaseDto> databases = databaseRepository.findAll()
                .stream()
                .map(databaseMapper::databaseToDatabaseDto)
                .toList();
        databaseIdxRepository.saveAll(databases);
        log.debug("saved {} databases to open search database", databases.size());
        /* identifiers */
        final List<IdentifierDto> identifiers = identifierRepository.findAll()
                .stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .toList();
        identifierIdxRepository.saveAll(identifiers);
        log.debug("saved {} identifiers to open search database", identifiers.size());
        /* columns */
        final List<ColumnDto> columns = tableColumnRepository.findAll()
                .stream()
                .map(tableMapper::tableColumnToColumnDto)
                .toList();
        tableColumnIdxRepository.saveAll(columns);
        log.debug("saved {} columns to open search database", columns.size());
        /* tables */
        final List<TableDto> tables = tableRepository.findAll()
                .stream()
                .map(tableMapper::tableToTableDto)
                .toList();
        tableIdxRepository.saveAll(tables);
        log.debug("saved {} tables to open search database", tables.size());
        /* units */
        final List<UnitDto> units = unitRepository.findAll()
                .stream()
                .map(ontologyMapper::tableColumnUnitToUnitDto)
                .toList();
        unitIdxRepository.saveAll(units);
        log.debug("saved {} units to open search database", units.size());
        /* users */
        final List<UserDto> users = userRepository.findAll()
                .stream()
                .map(userMapper::userToUserDto)
                .toList();
        userIdxRepository.saveAll(users);
        log.debug("saved {} users to open search database", users.size());
        /* view */
        final List<ViewDto> views = viewRepository.findAll()
                .stream()
                .map(viewMapper::viewToViewDto)
                .toList();
        viewIdxRepository.saveAll(views);
        log.debug("saved {} views to open search database", views.size());
        log.info("Synchronized 8 indices");
    }

}
