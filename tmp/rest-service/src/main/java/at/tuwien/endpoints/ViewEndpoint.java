package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.service.ViewService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/view")
public class ViewEndpoint {

    private final TableService tableService;
    private final ViewService viewService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public ViewEndpoint(TableService tableService, ViewService viewService,
                        MetadataServiceGateway metadataServiceGateway) {
        this.tableService = tableService;
        this.viewService = viewService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('admin')")
    @Observed(name = "dbr_database_create")
    @Operation(summary = "Create view", security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Created a new view",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
    })
    public ResponseEntity<Void> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @Valid @RequestBody ViewCreateDto data) throws DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, DatabaseMalformedException {
        log.debug("endpoint create view, databaseId={}, data.name={}", databaseId, data.getName());
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        try {
            viewService.create(database, data);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @DeleteMapping("/{viewId}")
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('admin')")
    @Observed(name = "dbr__create")
    @Operation(summary = "Delete view in database", security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Deleted table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotBlank @PathVariable("databaseId") Long databaseId,
                                       @NotBlank @PathVariable("viewId") Long viewId)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            DatabaseMalformedException {
        log.debug("endpoint delete view, databaseId={}, viewId={}", databaseId, viewId);
        final PrivilegedViewDto view = metadataServiceGateway.getViewById(databaseId, viewId);
        try {
            viewService.delete(view);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

}
