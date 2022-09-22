package at.tuwien.endpoints;

import at.tuwien.api.database.*;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.ContainerService;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.QueryStoreService;
import at.tuwien.service.UserService;
import at.tuwien.service.impl.MariaDbServiceImpl;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/container/{id}/database")
public class DatabaseEndpoint extends AbstractEndpoint {

    private final DatabaseMapper databaseMapper;
    private final MariaDbServiceImpl databaseService;
    private final QueryStoreService queryStoreService;
    private final MessageQueueService messageQueueService;

    @Autowired
    public DatabaseEndpoint(DatabaseMapper databaseMapper, ContainerService containerService,
                            MariaDbServiceImpl databaseService, QueryStoreService queryStoreService,
                            MessageQueueService messageQueueService) {
        super(databaseService, containerService);
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.queryStoreService = queryStoreService;
        this.messageQueueService = messageQueueService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List databases")
    public ResponseEntity<List<DatabaseBriefDto>> findAll(@NotBlank @PathVariable("id") Long containerId) {
        final List<DatabaseBriefDto> databases = databaseService.findAll(containerId)
                .stream()
                .map(databaseMapper::databaseToDatabaseBriefDto)
                .collect(Collectors.toList());
        log.info("Found {} databases", databases.size());
        log.debug("found databases {}", databases);
        return ResponseEntity.ok(databases);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Create database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseBriefDto> create(@NotBlank @PathVariable("id") Long containerId,
                                                   @Valid @RequestBody DatabaseCreateDto createDto,
                                                   Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException, DatabaseMalformedException,
            AmqpException, ContainerConnectionException, UserNotFoundException,
            DatabaseNotFoundException, DatabaseNameExistsException, DatabaseConnectionException,
            QueryMalformedException, NotAllowedException, BrokerVirtualHostCreationException {
        if (!hasContainerPermission(containerId, "CREATE_DATABASE", principal)) {
            log.error("Missing database create permission");
            throw new NotAllowedException("Missing database create permission");
        }
        final Database database = databaseService.create(containerId, createDto, principal);
        messageQueueService.createExchange(database, principal);
        queryStoreService.create(containerId, database.getId());
        messageQueueService.updatePermissions(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(databaseMapper.databaseToDatabaseBriefDto(database));
    }

    @PutMapping("/{databaseId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Update database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> update(@NotBlank @PathVariable("id") Long containerId,
                                              @NotBlank @PathVariable Long databaseId,
                                              @Valid @RequestBody DatabaseModifyDto modifyDto,
                                              @NotNull Principal principal)
            throws UserNotFoundException, DatabaseNotFoundException, LicenseNotFoundException, NotAllowedException {
        if (!hasDatabasePermission(containerId, databaseId, "UPDATE_DATABASE", principal)) {
            log.error("Missing database update permission");
            throw new NotAllowedException("Missing database update permission");
        }
        final Database database = databaseService.modify(containerId, databaseId, modifyDto);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @PutMapping("/{databaseId}/transfer")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Update database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> transfer(@NotBlank @PathVariable("id") Long containerId,
                                                @NotBlank @PathVariable Long databaseId,
                                                @Valid @RequestBody DatabaseTransferDto transferDto,
                                                @NotNull Principal principal)
            throws DatabaseNotFoundException, NotAllowedException {
        if (!hasDatabasePermission(containerId, databaseId, "TRANSFER_DATABASE", principal)) {
            log.error("Missing database update permission");
            throw new NotAllowedException("Missing database update permission");
        }
        final Database database = databaseService.transfer(containerId, databaseId, transferDto);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dto);
    }

    @GetMapping("/{databaseId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> findById(@NotBlank @PathVariable("id") Long containerId,
                                                @NotBlank @PathVariable Long databaseId)
            throws DatabaseNotFoundException {
        final Database database = databaseService.findById(containerId, databaseId);
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        log.info("Found database with id {}", database.getId());
        log.debug("found database {}", database);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{databaseId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Delete some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotBlank @PathVariable("id") Long containerId,
                                    @NotBlank @PathVariable Long databaseId,
                                    Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseMalformedException, AmqpException, ContainerNotFoundException,
            DatabaseConnectionException, QueryMalformedException, BrokerVirtualHostCreationException {
        final Database database = databaseService.findById(containerId, databaseId);
        messageQueueService.deleteExchange(database);
        databaseService.delete(containerId, databaseId, principal);
        messageQueueService.updatePermissions(principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}
