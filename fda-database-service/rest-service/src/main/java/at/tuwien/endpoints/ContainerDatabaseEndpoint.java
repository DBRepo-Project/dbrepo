package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.DatabaseModifyDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.MessageQueueService;
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
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/container/{id}/database")
public class ContainerDatabaseEndpoint {

    private final DatabaseMapper databaseMapper;
    private final MariaDbServiceImpl databaseService;
    private final MessageQueueService messageQueueService;

    @Autowired
    public ContainerDatabaseEndpoint(DatabaseMapper databaseMapper, MariaDbServiceImpl databaseService,
                                     MessageQueueService messageQueueService) {
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
        this.messageQueueService = messageQueueService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List databases")
    public ResponseEntity<List<DatabaseBriefDto>> findAll(@NotBlank @PathVariable("id") Long id,
                                                          Principal principal) {
        final List<DatabaseBriefDto> databases;
        if (principal == null) {
            log.trace("principal missing, listing all public databases only");
            databases = databaseService.findAllPublic(id)
                    .stream()
                    .map(databaseMapper::databaseToDatabaseBriefDto)
                    .collect(Collectors.toList());
        } else {
            log.trace("principal present, listing all public databases and my private databases");
            databases = databaseService.findAllPublicOrMine(id, principal)
                    .stream()
                    .map(databaseMapper::databaseToDatabaseBriefDto)
                    .collect(Collectors.toList());
        }
        log.info("Found {} databases", databases.size());
        log.debug("found databases {}", databases);
        return ResponseEntity.ok(databases);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Create database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseBriefDto> create(@NotBlank @PathVariable("id") Long id,
                                              @Valid @RequestBody DatabaseCreateDto createDto,
                                              Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException, DatabaseMalformedException,
            AmqpException, ContainerConnectionException, UserNotFoundException {
        final Database database = databaseService.create(id, createDto, principal);
        messageQueueService.createExchange(database, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(databaseMapper.databaseToDatabaseBriefDto(database));
    }

    @PutMapping("/{databaseId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Update database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseBriefDto> update(@NotBlank @PathVariable("id") Long id,
                                              @NotBlank @PathVariable Long databaseId,
                                              @Valid @RequestBody DatabaseModifyDto modifyDto)
            throws UserNotFoundException, DatabaseNotFoundException, LicenseNotFoundException {
        final Database database = databaseService.modify(id, databaseId, modifyDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(databaseMapper.databaseToDatabaseBriefDto(database));
    }

    @GetMapping("/{databaseId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> findById(@NotBlank @PathVariable("id") Long containerId,
                                                @NotBlank @PathVariable Long databaseId,
                                                Principal principal)
            throws DatabaseNotFoundException {
        final Database database = databaseService.findPublicOrMineById(containerId, databaseId, principal);
        if (!database.getIsPublic() && !principal.getName().equals(database.getCreator().getUsername())) {
            log.error("Found database but is private and creator does not match");
            log.debug("found database {}", database);
            log.debug("creator {} does not equal principal {}", database.getCreator().getUsername(), principal.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
        log.info("Found database with id {}", database.getId());
        log.debug("found database {}", database);
        return ResponseEntity.ok(databaseMapper.databaseToDatabaseDto(database));
    }

    @DeleteMapping("/{databaseId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Delete some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotBlank @PathVariable("id") Long containerId,
                                    @NotBlank @PathVariable Long databaseId,
                                    Principal principal) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseMalformedException, AmqpException, ContainerConnectionException {
        final Database database = databaseService.findById(containerId, databaseId);
        messageQueueService.deleteExchange(database);
        databaseService.delete(containerId, databaseId, principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}
