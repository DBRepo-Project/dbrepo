package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.UpdateDatabaseAccessDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
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
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/access")
public class AccessEndpoint {

    private final UserService userService;
    private final AccessService accessService;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;

    @Autowired
    public AccessEndpoint(UserService userService, AccessService accessService, DatabaseMapper databaseMapper,
                          DatabaseService databaseService) {
        this.userService = userService;
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
    }

    @PostMapping("/{userId}")
    @Transactional
    @Observed(name = "dbrepo_metadata_access_give")
    @PreAuthorize("hasAuthority('create-database-access')")
    @Operation(summary = "Give access to some database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<?> create(@NotBlank @PathVariable("databaseId") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @Valid @RequestBody UpdateDatabaseAccessDto data,
                                    @NotNull Principal principal) throws NotAllowedException, ServiceException,
            ServiceConnectionException, DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint give access to database, databaseId={}, userId={}, access.type={}", databaseId, userId,
                data.getType());
        final Database database = databaseService.findById(databaseId);
        final User user = userService.findByUsername(principal.getName());
        if (database.getOwner().equals(user)) {
            log.error("Failed to give access to user with id {}: not owner", userId);
            throw new NotAllowedException("Failed to give access to user with id " + userId + ": not owner");
        }
        try {
            accessService.find(database, user);
            log.error("Failed to give access to user with id {}: already has access", userId);
            throw new NotAllowedException("Failed to give access to user with id " + userId + ": already has access");
        } catch (AccessNotFoundException e) {
            /* ignore */
        }
        accessService.create(database, user, data.getType());
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{userId}")
    @Transactional
    @Observed(name = "dbrepo_metadata_access_modify")
    @PreAuthorize("hasAuthority('update-database-access')")
    @Operation(summary = "Modify access to some database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<?> update(@NotBlank @PathVariable("databaseId") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @Valid @RequestBody UpdateDatabaseAccessDto data,
                                    @NotNull Principal principal) throws NotAllowedException,
            ServiceException, ServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint modify database access, databaseId={}, userId={}, access.type={}", databaseId, userId,
                data.getType());
        final Database database = databaseService.findById(databaseId);
        final User user = userService.findByUsername(principal.getName());
        if (database.getOwner().equals(user)) {
            log.error("Failed to give access to user with id {}: not owner", userId);
            throw new NotAllowedException("Failed to give access to user with id " + userId + ": not owner");
        }
        accessService.find(database, user);
        accessService.update(database, user, data.getType());
        return ResponseEntity.accepted()
                .build();
    }

    @RequestMapping(value = "/{userId}", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_metadata_access_get")
    @PreAuthorize("hasAuthority('check-database-access') or hasAuthority('admin')")
    @Operation(summary = "Check access to some database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<DatabaseAccessDto> find(@NotBlank @PathVariable("databaseId") Long databaseId,
                                                  @NotBlank @PathVariable("userId") UUID userId,
                                                  @NotNull Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, AccessNotFoundException, NotAllowedException {
        log.debug("endpoint get database access, databaseId={}, userId={}, principal.name={}", databaseId, userId,
                principal.getName());
        if (!userId.equals(UserUtil.getId(principal))) {
            if (!UserUtil.hasRole(principal, "admin")) {
                log.error("Failed to find access: foreign user");
                throw new NotAllowedException("Failed to find access: foreign user");
            }
            log.trace("principal is allowed to check foreign user access");
        }
        final Database database = databaseService.findById(databaseId);
        final User user = userService.findById(userId);
        final DatabaseAccess access = accessService.find(database, user);
        final DatabaseAccessDto dto = databaseMapper.databaseAccessToDatabaseAccessDto(access);
        log.trace("check access resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{userId}")
    @Transactional
    @Observed(name = "dbrepo_metadata_access_delete")
    @PreAuthorize("hasAuthority('delete-database-access')")
    @Operation(summary = "Revoke access to some database", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<?> revoke(@NotBlank @PathVariable("databaseId") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @NotNull Principal principal) throws NotAllowedException, ServiceException,
            ServiceConnectionException, DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint revoke database access, databaseId={}, userId={}", databaseId, userId);
        final Database database = databaseService.findById(databaseId);
        final User user = userService.findByUsername(principal.getName());
        if (!database.getOwner().equals(user)) {
            log.error("Failed to revoke access to user with id {}: not owner", user.getId());
            throw new NotAllowedException("Failed to revoke access to user with id " + user.getId() + ": not owner");
        }
        accessService.find(database, user);
        accessService.delete(database, user);
        return ResponseEntity.accepted()
                .build();
    }

}
