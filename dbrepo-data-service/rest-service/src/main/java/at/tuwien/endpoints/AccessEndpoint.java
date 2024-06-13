package at.tuwien.endpoints;

import at.tuwien.api.database.UpdateDatabaseAccessDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.PrivilegedUserDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.AccessService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/access")
public class AccessEndpoint {

    private final AccessService accessService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public AccessEndpoint(AccessService accessService, MetadataServiceGateway metadataServiceGateway) {
        this.accessService = accessService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Give access",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Granting access succeeded"),
            @ApiResponse(responseCode = "400",
                    description = "Granting access query or database connection is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to give access",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to give access in the database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection to metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> create(@NotBlank @PathVariable("databaseId") Long databaseId,
                                       @NotBlank @PathVariable("userId") UUID userId,
                                       @Valid @RequestBody UpdateDatabaseAccessDto data)
            throws NotAllowedException, DatabaseUnavailableException, DatabaseNotFoundException,
            RemoteUnavailableException, UserNotFoundException, DatabaseMalformedException, ServiceException {
        log.debug("endpoint give access to database, databaseId={}, userId={}", databaseId, userId);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final PrivilegedUserDto user = metadataServiceGateway.getPrivilegedUserById(userId);
        if (database.getAccesses().stream().anyMatch(a -> a.getUser().getId().equals(userId))) {
            log.error("Failed to create access to user with id {}: already has access", userId);
            throw new NotAllowedException("Failed to create access to user with id " + userId + ": already has access");
        }
        try {
            accessService.create(database, user, data.getType());
            return ResponseEntity.accepted()
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Update access",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Update access succeeded"),
            @ApiResponse(responseCode = "400",
                    description = "Update access query or database connection is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to update access",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to update access in database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> update(@NotBlank @PathVariable("databaseId") Long databaseId,
                                       @NotBlank @PathVariable("userId") UUID userId,
                                       @Valid @RequestBody UpdateDatabaseAccessDto access) throws NotAllowedException,
            DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            DatabaseMalformedException, ServiceException {
        log.debug("endpoint modify access to database, databaseId={}, userId={}, access.type={}", databaseId, userId,
                access.getType());
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final UserDto user = metadataServiceGateway.getUserById(userId);
        if (database.getAccesses().stream().noneMatch(a -> a.getUser().getId().equals(userId))) {
            log.error("Failed to update access to user with id {}: no access", userId);
            throw new NotAllowedException("Failed to update access to user with id " + userId + ": no access");
        }
        try {
            accessService.update(database, user, access.getType());
            return ResponseEntity.accepted()
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Revoke access",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Revoked access successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Revoke access query or database connection is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to revoke access",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to revoke access in database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> revoke(@NotBlank @PathVariable("databaseId") Long databaseId,
                                       @NotBlank @PathVariable("userId") UUID userId) throws NotAllowedException,
            DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            DatabaseMalformedException, ServiceException {
        log.debug("endpoint revoke access to database, databaseId={}, userId={}", databaseId, userId);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final UserDto user = metadataServiceGateway.getUserById(userId);
        if (database.getAccesses().stream().noneMatch(a -> a.getUser().getId().equals(userId))) {
            log.error("Failed to delete access to user with id {}: no access", userId);
            throw new NotAllowedException("Failed to delete access to user with id " + userId + ": no access");
        }
        try {
            accessService.delete(database, user);
            return ResponseEntity.accepted()
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
