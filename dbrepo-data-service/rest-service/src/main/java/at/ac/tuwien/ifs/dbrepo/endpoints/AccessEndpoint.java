package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class AccessEndpoint extends RestEndpoint {

    private final CacheService cacheService;
    private final AccessService accessService;

    @Autowired
    public AccessEndpoint(CacheService cacheService, AccessService accessService) {
        this.cacheService = cacheService;
        this.accessService = accessService;
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
        final DatabaseDto database = cacheService.getDatabase(databaseId, true);
        final UserDto user = cacheService.getUser(username);
        if (database.getAccesses().stream().anyMatch(a -> a.getUser().getUsername().equals(username))) {
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
        final DatabaseDto database = cacheService.getDatabase(databaseId, true);
        final UserDto user = cacheService.getUser(username);
        if (database.getAccesses().stream().noneMatch(a -> a.getUser().getUsername().equals(username))) {
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
        final DatabaseDto database = cacheService.getDatabase(databaseId, true);
        final UserDto user = cacheService.getUser(username);
        if (database.getAccesses().stream().noneMatch(a -> a.getUser().getUsername().equals(username))) {
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

}
