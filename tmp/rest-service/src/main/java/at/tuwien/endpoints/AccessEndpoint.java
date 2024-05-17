package at.tuwien.endpoints;

import at.tuwien.api.database.UpdateDatabaseAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.user.PrivilegedUserDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.AccessService;
import at.tuwien.utils.UserUtil;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
    @Transactional
    @Observed(name = "dbr_access_give")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Give access to some database", security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Granting access succeeded",
                    content = {@Content}),
            @ApiResponse(responseCode = "400",
                    description = "Granting access query or database connection is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Failed giving access",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Granting access not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> create(@NotBlank @PathVariable("databaseId") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @Valid @RequestBody UpdateDatabaseAccessDto data,
                                    @NotNull Principal principal)
            throws NotAllowedException, QueryMalformedException, DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, DatabaseMalformedException {
        log.debug("endpoint give access to database, databaseId={}, userId={}", databaseId, userId);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final PrivilegedUserDto user = metadataServiceGateway.getUserById(userId);
        if (database.getOwner().getUsername().equals(principal.getName())) {
            log.error("Failed to create access to user with id {}: not owner", userId);
            throw new NotAllowedException("Failed to create access to user with id " + userId + ": not owner");
        }
        if (database.getAccesses().stream().anyMatch(a -> a.getUser().getUsername().equals(principal.getName()))) {
            log.error("Failed to create access to user with id {}: already has access", userId);
            throw new NotAllowedException("Failed to create access to user with id " + userId + ": already has access");
        }
        try {
            accessService.create(database, user, data.getType());
            return ResponseEntity.accepted()
                    .build();
        } catch (SQLException e) {
            throw new QueryMalformedException(e);
        }
    }

    @PutMapping("/{userId}")
    @Transactional
    @Observed(name = "dbr_access_modify")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Modify access to some database", security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modify access succeeded",
                    content = {@Content}),
            @ApiResponse(responseCode = "400",
                    description = "Modify access query or database connection is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Modify access not permitted when no access is granted in the first place",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> update(@NotBlank @PathVariable("databaseId") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @Valid @RequestBody DatabaseModifyAccessDto accessDto,
                                    @NotNull Principal principal) throws NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException, DatabaseMalformedException {
        log.debug("endpoint modify access to database, databaseId={}, userId={}, accessDto={}", databaseId, userId, accessDto);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final PrivilegedUserDto user = metadataServiceGateway.getUserById(userId);
        if (database.getOwner().getUsername().equals(principal.getName())) {
            log.error("Failed to update access to user with id {}: not owner", userId);
            throw new NotAllowedException("Failed to update access to user with id " + userId + ": not owner");
        }
        if (database.getAccesses().stream().noneMatch(a -> a.getUser().getUsername().equals(principal.getName()))) {
            log.error("Failed to update access to user with id {}: no access", userId);
            throw new NotAllowedException("Failed to update access to user with id " + userId + ": no access");
        }
        try {
            accessService.update(database, user, accessDto.getType());
            return ResponseEntity.accepted()
                    .build();
        } catch (SQLException e) {
            throw new QueryMalformedException(e);
        }
    }

    @DeleteMapping("/{userId}")
    @Transactional
    @Observed(name = "dbr_access_delete")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Revoke access to some database", security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Revoked access successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "400",
                    description = "Modify access query or database connection is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Revoke of access not permitted as no access was found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "User, database with access was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> revoke(@NotBlank @PathVariable("databaseId") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @NotNull Principal principal) throws NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException, DatabaseMalformedException {
        log.debug("endpoint revoke access to database, databaseId={}, userId={}", databaseId, userId);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final PrivilegedUserDto user = metadataServiceGateway.getUserById(userId);
        if (database.getOwner().getUsername().equals(principal.getName())) {
            log.error("Failed to delete access to user with id {}: not owner", userId);
            throw new NotAllowedException("Failed to delete access to user with id " + userId + ": not owner");
        }
        if (database.getAccesses().stream().noneMatch(a -> a.getUser().getUsername().equals(principal.getName()))) {
            log.error("Failed to delete access to user with id {}: no access", userId);
            throw new NotAllowedException("Failed to delete access to user with id " + userId + ": no access");
        }
        try {
            accessService.delete(database, user);
            return ResponseEntity.accepted()
                    .build();
        } catch (SQLException e) {
            throw new QueryMalformedException(e);
        }
    }

}
