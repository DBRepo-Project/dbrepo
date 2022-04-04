package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
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
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/container/{id}/database")
public class ContainerDatabaseEndpoint {

    private final DatabaseMapper databaseMapper;
    private final MariaDbServiceImpl databaseService;

    @Autowired
    public ContainerDatabaseEndpoint(DatabaseMapper databaseMapper, MariaDbServiceImpl databaseService) {
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List databases")
    public ResponseEntity<List<DatabaseBriefDto>> findAll(@NotBlank @PathVariable("id") Long id) {
        final List<DatabaseBriefDto> databases = databaseService.findAll(id)
                .stream()
                .map(databaseMapper::databaseToDatabaseBriefDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(databases);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Create database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseDto> create(@NotBlank @PathVariable("id") Long id,
                                              @Valid @RequestBody DatabaseCreateDto createDto)
            throws ImageNotSupportedException, ContainerNotFoundException, DatabaseMalformedException,
            AmqpException, ContainerConnectionException {
        final Database database = databaseService.create(id, createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(databaseMapper.databaseToDatabaseDto(database));
    }

    @GetMapping("/{databaseId}")
    @Transactional(readOnly = true)
    @Operation(summary = "List some database")
    public ResponseEntity<DatabaseDto> findById(@NotBlank @PathVariable("id") Long id,
                                                @NotBlank @PathVariable Long databaseId)
            throws DatabaseNotFoundException {
        return ResponseEntity.ok(databaseMapper.databaseToDatabaseDto(databaseService.findById(id, databaseId)));
    }

    @DeleteMapping("/{databaseId}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_DEVELOPER') or hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Delete some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotBlank @PathVariable("id") Long id,
                                    @NotBlank @PathVariable Long databaseId) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseMalformedException, AmqpException, ContainerConnectionException {
        databaseService.delete(id, databaseId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();
    }

}
