package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.UpdateDatabaseAccessDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import io.micrometer.observation.annotation.Observed;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/access")
public class AccessEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final AccessService accessService;
    private final MetadataMapper databaseMapper;
    private final DatabaseService databaseService;

    @Autowired
    public AccessEndpoint(UserService userService, AccessService accessService, MetadataMapper databaseMapper,
                          DatabaseService databaseService) {
        this.userService = userService;
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
    }

    @PostMapping("/{userId}")
    @Transactional
    @Observed(name = "dbrepo_access_give")
    @PreAuthorize("hasAuthority('create-database-access')")
    @Operation(summary = "Give access",
            description = "Give a user with given id access to some database with given id. Requires role `create-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Granting access succeeded",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseAccessDto.class))}),
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
            @ApiResponse(responseCode = "502",
                    description = "Access could not be created due to connection error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Access could not be created in the data service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseAccessDto> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                                    @PathVariable("userId") UUID userId,
                                                    @Valid @RequestBody UpdateDatabaseAccessDto data,
                                                    @NotNull Principal principal) throws NotAllowedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint give access to database, databaseId={}, userId={}, access.type={}", databaseId, userId,
                data.getType());
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to create access: not owner");
            throw new NotAllowedException("Failed to create access: not owner");
        }
        final User user = userService.findById(userId);
        try {
            accessService.find(database, user);
            log.error("Failed to create access to user with id {}: already has access", userId);
            throw new NotAllowedException("Failed to create access to user with id " + userId + ": already has access");
        } catch (AccessNotFoundException e) {
            /* ignore */
        }
        accessService.create(database, user, data.getType());
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{userId}")
    @Transactional
    @Observed(name = "dbrepo_access_modify")
    @PreAuthorize("hasAuthority('update-database-access')")
    @Operation(summary = "Modify access",
            description = "Modifies access of a user with given id to database with given id. Requires role `update-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified access"),
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
            @ApiResponse(responseCode = "502",
                    description = "Access could not be updated due to connection error in the data service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Access could not be updated in the data service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> update(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @PathVariable("userId") UUID userId,
                                       @Valid @RequestBody UpdateDatabaseAccessDto data,
                                       @NotNull Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint modify database access, databaseId={}, userId={}, access.type={}, principal.name={}",
                databaseId, userId, data.getType(), principal.getName());
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to update access: not owner");
            throw new NotAllowedException("Failed to update access: not owner");
        }
        if (database.getOwner().getId().equals(userId)) {
            log.error("Failed to update access: the owner must have write-all access");
            throw new NotAllowedException("Failed to update access: the owner must have write-all access");
        }
        final User user = userService.findById(userId);
        if (user.getIsInternal()) {
            log.error("Failed to update access: the internal user must have write-all access");
            throw new NotAllowedException("Failed to update access: the internal user must have write-all access");
        }
        accessService.find(database, user);
        accessService.update(database, user, data.getType());
        return ResponseEntity.accepted()
                .build();
    }

    @RequestMapping(value = "/{userId}", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_access_get")
    @PreAuthorize("hasAuthority('check-database-access') or hasAuthority('check-foreign-database-access')")
    @Operation(summary = "Find/Check access",
            description = "Finds or checks access of a user with given id to a database with given id. Requests with HTTP method **GET** return the access object, requests with HTTP method **HEAD** only the status. When the user has at least *READ* access, the status 200 is returned, 403 otherwise. Requires role `check-database-access` or `check-foreign-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found database access",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseAccessDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "No access to this database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseAccessDto> find(@NotNull @PathVariable("databaseId") Long databaseId,
                                                  @PathVariable("userId") UUID userId,
                                                  @NotNull Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, AccessNotFoundException, NotAllowedException {
        log.debug("endpoint get database access, databaseId={}, userId={}, principal.name={}", databaseId, userId,
                principal.getName());
        if (!userId.equals(getId(principal))) {
            if (!hasRole(principal, "check-foreign-database-access")) {
                log.error("Failed to find access: foreign user");
                throw new NotAllowedException("Failed to find access: foreign user");
            }
            log.trace("principal is allowed to check foreign user access");
        }
        final Database database = databaseService.findById(databaseId);
        final User user = userService.findById(userId);
        final DatabaseAccess access = accessService.find(database, user);
        return ResponseEntity.ok(databaseMapper.databaseAccessToDatabaseAccessDto(access));
    }

    @DeleteMapping("/{userId}")
    @Transactional
    @Observed(name = "dbrepo_access_delete")
    @PreAuthorize("hasAuthority('delete-database-access')")
    @Operation(summary = "Delete access",
            description = "Delete access of a user with id to a database with id. Requires role `delete-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted access"),
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
            @ApiResponse(responseCode = "502",
                    description = "Access could not be created due to connection error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Access could not be revoked in the data service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> revoke(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @PathVariable("userId") UUID userId,
                                       @NotNull Principal principal) throws NotAllowedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint revoke database access, databaseId={}, userId={}", databaseId, userId);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getId().equals(getId(principal))) {
            log.error("Failed to revoke access: not owner");
            throw new NotAllowedException("Failed to revoke access: not owner");
        }
        if (database.getOwner().getId().equals(userId)) {
            log.error("Failed to revoke access: the owner must have write-all access");
            throw new NotAllowedException("Failed to revoke access: the owner must have write-all access");
        }
        final User user = userService.findById(userId);
        if (user.getIsInternal()) {
            log.error("Failed to revoke access: the internal user must have write-all access");
            throw new NotAllowedException("Failed to revoke access: the internal user must have write-all access");
        }
        accessService.find(database, user);
        accessService.delete(database, user);
        return ResponseEntity.accepted()
                .build();
    }

}
