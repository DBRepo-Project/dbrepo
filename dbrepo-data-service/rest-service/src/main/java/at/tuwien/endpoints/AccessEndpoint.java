package at.tuwien.endpoints;

import at.tuwien.api.database.CreateAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.CredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/access")
public class AccessEndpoint extends RestEndpoint {

    private final AccessService accessService;
    private final CredentialService credentialService;

    @Autowired
    public AccessEndpoint(AccessService accessService, CredentialService credentialService) {
        this.accessService = accessService;
        this.credentialService = credentialService;
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAuthority('system')")
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
    public ResponseEntity<Void> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @PathVariable("userId") UUID userId,
                                       @Valid @RequestBody CreateAccessDto data)
            throws NotAllowedException, DatabaseUnavailableException, DatabaseNotFoundException,
            RemoteUnavailableException, UserNotFoundException, DatabaseMalformedException, MetadataServiceException {
        log.debug("endpoint give access to database, databaseId={}, userId={}", databaseId, userId);
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        final UserDto user = credentialService.getUser(userId);
        if (database.getAccesses().stream().anyMatch(a -> a.getUser().getId().equals(userId))) {
            log.error("Failed to create access to user with id {}: already has access", userId);
            throw new NotAllowedException("Failed to create access to user with id " + userId + ": already has access");
        }
        try {
            accessService.create(database, user, data.getType());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('system')")
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
    public ResponseEntity<Void> update(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @PathVariable("userId") UUID userId,
                                       @Valid @RequestBody CreateAccessDto access) throws NotAllowedException,
            DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            DatabaseMalformedException, MetadataServiceException {
        log.debug("endpoint modify access to database, databaseId={}, userId={}, access.type={}", databaseId, userId,
                access.getType());
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        final UserDto user = credentialService.getUser(userId);
        if (database.getAccesses().stream().noneMatch(a -> a.getHuserid().equals(userId))) {
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

    @PutMapping
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Invalidate access cache for database",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Invalidated access cache succeeded")
    })
    public ResponseEntity<Void> invalidateAccess(@NotNull @PathVariable("databaseId") Long databaseId) {
        log.debug("endpoint empty access cache for database, databaseId={}", databaseId);
        credentialService.invalidateAccess(databaseId);
        return ResponseEntity.accepted()
                .build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('system')")
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
    public ResponseEntity<Void> revoke(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @PathVariable("userId") UUID userId) throws NotAllowedException,
            DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            DatabaseMalformedException, MetadataServiceException {
        log.debug("endpoint revoke access to database, databaseId={}, userId={}", databaseId, userId);
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        final UserDto user = credentialService.getUser(userId);
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
