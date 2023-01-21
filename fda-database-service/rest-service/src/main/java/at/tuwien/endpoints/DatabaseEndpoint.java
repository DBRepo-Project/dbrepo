package at.tuwien.endpoints;

import at.tuwien.api.database.*;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.*;
import at.tuwien.service.impl.MariaDbServiceImpl;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/container/{id}/database")
public class DatabaseEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final AccessService accessService;
    private final DatabaseMapper databaseMapper;
    private final IdentifierMapper identifierMapper;
    private final MariaDbServiceImpl databaseService;
    private final QueryStoreService queryStoreService;
    private final IdentifierService identifierService;
    private final MessageQueueService messageQueueService;
    private final DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    public DatabaseEndpoint(DatabaseMapper databaseMapper, ContainerService containerService,
                            UserService userService, MariaDbServiceImpl databaseService, QueryStoreService queryStoreService,
                            IdentifierService identifierService, IdentifierMapper identifierMapper,
                            MessageQueueService messageQueueService, AccessService accessService,
                            DatabaseAccessRepository databaseAccessRepository) {
        super(databaseService, containerService, databaseAccessRepository);
        this.userService = userService;
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
        this.identifierMapper = identifierMapper;
        this.databaseService = databaseService;
        this.queryStoreService = queryStoreService;
        this.identifierService = identifierService;
        this.messageQueueService = messageQueueService;
        this.databaseAccessRepository = databaseAccessRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "database.list", description = "Time needed to list the databases")
    @Operation(summary = "List databases")
    public ResponseEntity<List<DatabaseBriefDto>> list(@NotNull @PathVariable("id") Long containerId,
                                                       @NotNull Principal principal) {
        log.debug("endpoint list databases, containerId={}, principal={}", containerId, principal);
        final List<Identifier> identifiers = identifierService.findAll(containerId);
        final List<DatabaseBriefDto> databases = databaseService.findAll(containerId)
                .stream()
                .map(databaseMapper::databaseToDatabaseBriefDto)
                .collect(Collectors.toList());
        databases.forEach(db -> {
            final Optional<Identifier> id = identifiers.stream()
                    .filter(i -> i.getContainerId().equals(containerId) && i.getDatabaseId().equals(containerId) &&
                            i.getType().equals(IdentifierType.DATABASE))
                    .findFirst();
            id.ifPresent(identifier -> db.setIdentifier(identifierMapper.identifierToIdentifierDto(identifier)));
        });
        log.trace("list databases resulted in databases {}", databases);
        return ResponseEntity.ok(databases);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DEVELOPER')")
    @Timed(value = "database.create", description = "Time needed to create a database")
    @Operation(summary = "Create database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseBriefDto> create(@NotNull @PathVariable("id") Long containerId,
                                                   @Valid @RequestBody DatabaseCreateDto createDto,
                                                   @NotNull Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException, DatabaseMalformedException,
            AmqpException, ContainerConnectionException, UserNotFoundException,
            DatabaseNotFoundException, DatabaseNameExistsException, DatabaseConnectionException,
            QueryMalformedException, NotAllowedException, BrokerVirtualHostCreationException, QueryStoreException {
        log.debug("endpoint create database, containerId={}, createDto={}, principal={}", containerId, createDto,
                principal);
        if (!hasContainerPermission(containerId, "CREATE_DATABASE", principal)) {
            log.error("Missing database create permission");
            throw new NotAllowedException("Missing database create permission");
        }
        final Database database = databaseService.create(containerId, createDto, principal);
        final User user = userService.findByUsername(principal.getName());
        messageQueueService.createExchange(database, principal);
        queryStoreService.create(containerId, database.getId(), principal);
        messageQueueService.updatePermissions(principal);
        databaseAccessRepository.save(databaseMapper.defaultCreatorAccess(database, user));
        final DatabaseBriefDto dto = databaseMapper.databaseToDatabaseBriefDto(database);
        log.trace("create database resulted in database {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping("/{databaseId}/visibility")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DEVELOPER')")
    @Timed(value = "database.visibility", description = "Time needed to modify a database visibility")
    @Operation(summary = "Update database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> visibility(@NotNull @PathVariable("id") Long containerId,
                                                  @NotNull @PathVariable Long databaseId,
                                                  @Valid @RequestBody DatabaseModifyVisibilityDto data,
                                                  @NotNull Principal principal)
            throws DatabaseNotFoundException, NotAllowedException {
        log.debug("endpoint update database, containerId={}, databaseId={}, data={}, principal={}", containerId,
                databaseId, data, principal);
        if (!hasDatabasePermission(containerId, databaseId, "VISIBILITY_DATABASE", principal)) {
            log.error("Missing database update visibility permission");
            throw new NotAllowedException("Missing database update visibility permission");
        }
        final Database database = databaseService.visibility(containerId, databaseId, data);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        log.trace("update database resulted in database {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @PutMapping("/{databaseId}/transfer")
    @Transactional
    @Timed(value = "database.transfer", description = "Time needed to transfer a database ownership")
    @Operation(summary = "Update database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> transfer(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable Long databaseId,
                                                @Valid @RequestBody DatabaseTransferDto transferDto,
                                                @NotNull Principal principal)
            throws DatabaseNotFoundException, NotAllowedException, UserNotFoundException {
        log.debug("endpoint update database, containerId={}, databaseId={}, transferDto={}, principal={}", containerId,
                databaseId, transferDto, principal);
        if (!hasDatabasePermission(containerId, databaseId, "TRANSFER_DATABASE", principal)) {
            log.error("Missing database transfer ownership permission");
            throw new NotAllowedException("Missing database transfer ownership permission");
        }
        final Database database = databaseService.transfer(containerId, databaseId, transferDto);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        log.trace("update database resulted in database {}", dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @GetMapping("/{databaseId}")
    @Transactional(readOnly = true)
    @Timed(value = "database.find", description = "Time needed to find a database")
    @Operation(summary = "Find some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> findById(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable Long databaseId,
                                                Principal principal)
            throws DatabaseNotFoundException, AccessDeniedException {
        log.debug("endpoint find database, containerId={}, databaseId={}", containerId, databaseId);
        final Database database = databaseService.findById(containerId, databaseId);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        try {
            final Identifier identifier = identifierService.find(containerId, databaseId, IdentifierType.DATABASE);
            dto.setIdentifier(identifierMapper.identifierToIdentifierDto(identifier));
        } catch (IdentifierNotFoundException e) {
            // ignore
        }
        if (principal != null && database.getOwner().getUsername().equals(principal.getName())) {
            /* only owner sees the access rights */
            final List<DatabaseAccess> accesses = accessService.list(databaseId);
            dto.setAccesses(accesses.stream()
                    .map(databaseMapper::databaseAccessToDatabaseAccessDto)
                    .collect(Collectors.toList()));
        }
        log.trace("find database resulted in database {}", database);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{databaseId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER')")
    @Timed(value = "database.delete", description = "Time needed to delete a database")
    @Operation(summary = "Delete some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable Long databaseId,
                                    Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseMalformedException, AmqpException, ContainerNotFoundException,
            QueryMalformedException, BrokerVirtualHostCreationException, UserNotFoundException, DatabaseConnectionException {
        log.debug("endpoint delete database, containerId={}, databaseId={}, principal={}", containerId, databaseId,
                principal);
        final Database database = databaseService.findById(containerId, databaseId);
        messageQueueService.deleteExchange(database);
        databaseService.delete(containerId, databaseId, principal);
        messageQueueService.updatePermissions(principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}
