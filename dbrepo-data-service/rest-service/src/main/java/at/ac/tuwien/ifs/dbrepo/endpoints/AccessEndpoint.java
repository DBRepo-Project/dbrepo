package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/access")
public class AccessEndpoint {

    private final AccessService accessService;
    private final MetadataService metadataService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public AccessEndpoint(AccessService accessService, MetadataService metadataService, MetadataServiceGateway metadataServiceGateway) {
        this.accessService = accessService;
        this.metadataService = metadataService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @PostMapping("/{username}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Give access to a user with given username.",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Granting access succeeded"),
            @ApiResponse(responseCode = "400",
                    description = "Granting access query or database connection is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to give access",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to give access in the database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection to metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @PathVariable("username") String username,
                                       @Valid @RequestBody CreateAccessDto data)
            throws NotAllowedException, DatabaseUnavailableException, DatabaseNotFoundException,
            RemoteUnavailableException, UserNotFoundException, DatabaseMalformedException, MetadataServiceException,
            AccessNotFoundException {
        log.debug("endpoint give access to database, databaseId={}, username={}", databaseId, username);
        final Database database = metadataServiceGateway.getDatabaseById(databaseId);
        final User user = metadataService.getUser(username);
        if (database.getAccesses().stream().anyMatch(a -> a.getUsername().equals(username))) {
            log.error("Failed to create access to user {}: already has access", username);
            throw new AccessNotFoundException("Failed to create access to user " + username + ": already has access");
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

    @PutMapping("/{username}/synchronize")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Apply the committed metadata grant", hidden = true,
            security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Current grant applied or membership revoked"),
            @ApiResponse(responseCode = "401", description = "Internal service identity required", content = {@Content}),
            @ApiResponse(responseCode = "404", description = "Database or user not found", content = {@Content}),
            @ApiResponse(responseCode = "417", description = "Failed to apply SQL privileges", content = {@Content}),
            @ApiResponse(responseCode = "503", description = "Database or metadata service unavailable", content = {@Content})
    })
    public ResponseEntity<Void> synchronize(@NotNull @PathVariable("databaseId") UUID databaseId,
                                            @PathVariable("username") String username)
            throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException, DatabaseMalformedException, DatabaseUnavailableException {
        final Database database = metadataServiceGateway.getDatabaseById(databaseId);
        final var access = database.getAccesses().stream()
                .filter(member -> username.equals(member.getUsername())).findFirst();
        // A revoked membership cancels a pending grant. No request payload can override it.
        if (access.isEmpty()) {
            return ResponseEntity.accepted().build();
        }
        final User user = metadataService.getUser(username);
        final AccessTypeDto type = AccessTypeDto.valueOf(access.get().getType().name());
        try {
            // CREATE USER IF NOT EXISTS also handles a target with no previous SQL account.
            accessService.create(database, user, type);
            accessService.update(database, user, type);
            return ResponseEntity.accepted().build();
        } catch (SQLException e) {
            throw new DatabaseUnavailableException("Failed to synchronize SQL access", e);
        }
    }

    @PutMapping("/{username}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Update access",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Update access succeeded"),
            @ApiResponse(responseCode = "400",
                    description = "Update access query or database connection is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to update access",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to update access in database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> update(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @PathVariable("username") String username,
                                       @Valid @RequestBody CreateAccessDto access) throws DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException, DatabaseMalformedException,
            MetadataServiceException, AccessNotFoundException {
        log.debug("endpoint modify access to database, databaseId={}, username={}, access.type={}", databaseId, username,
                access.getType());
        final Database database = metadataServiceGateway.getDatabaseById(databaseId);
        final User user = existingUser(username, access.getType() == AccessTypeDto.READ);
        if (database.getAccesses().stream().noneMatch(a -> a.getUsername().equals(username))) {
            log.error("Failed to update access to user {}: no access", username);
            throw new AccessNotFoundException("Failed to update access to user " + username + ": no access");
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

    @DeleteMapping("/{username}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Revoke access of user with given username",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Revoked access successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Revoke access query or database connection is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to revoke access",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to revoke access in database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> revoke(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @PathVariable("username") String username) throws DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException, DatabaseMalformedException,
            MetadataServiceException, AccessNotFoundException {
        log.debug("endpoint revoke access to database, databaseId={}, username={}", databaseId, username);
        final Database database = metadataServiceGateway.getDatabaseById(databaseId);
        final User user = existingUser(username, true);
        if (database.getAccesses().stream().noneMatch(a -> a.getUsername().equals(username))) {
            log.error("Failed to delete access to user {}: no access", username);
            throw new AccessNotFoundException("Failed to delete access to user " + username + ": no access");
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

    private User existingUser(String username, boolean allowRestriction) throws RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException {
        try {
            return metadataService.getUser(username);
        } catch (UserNotFoundException e) {
            if (!allowRestriction) {
                throw e;
            }
            // A removed identity must not prevent restricting an existing SQL account.
            return User.builder().username(username).build();
        }
    }

}
