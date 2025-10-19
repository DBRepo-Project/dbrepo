package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import io.micrometer.observation.annotation.Observed;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/access")
public class AccessEndpoint extends AbstractEndpoint {

    private final AccessService accessService;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final DashboardService dashboardService;

    @Autowired
    public AccessEndpoint(AccessService accessService, MetadataMapper metadataMapper, DatabaseService databaseService,
                          DashboardService dashboardService) {
        this.accessService = accessService;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.dashboardService = dashboardService;
    }

    @PostMapping("/{username}")
    @Transactional
    @Observed(name = "dbrepo_access_give")
    @PreAuthorize("hasAuthority('create-database-access')")
    @Operation(summary = "Give access",
            description = "Give a user with given username access to some database with given id. Requires role `create-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Granting access succeeded",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseAccessDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Granting access query or database connection is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Failed giving access",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user not found",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Access could not be created due to connection error",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Access could not be created in the data service",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseAccessDto> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                    @PathVariable("username") String username,
                                                    @Valid @RequestBody CreateAccessDto data,
                                                    Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, SearchServiceException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        log.debug("endpoint give access to database, databaseId={}, username={}, access.type={}", databaseId, username,
                data.getType());
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwnedBy().equals(getUsername(principal))) {
            log.error("Failed to create access: not owner");
            throw new NotAllowedException("Failed to create access: not owner");
        }
        try {
            accessService.find(database, username);
            log.error("Failed to create access to user {}: already has access", username);
            throw new NotAllowedException("Failed to create access to user " + username + ": already has access");
        } catch (AccessNotFoundException e) {
            /* ignore */
        }
        accessService.create(database, username, data.getType());
        dashboardService.updateAccess(database, username, data.getType());
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{username}")
    @Transactional
    @Observed(name = "dbrepo_access_modify")
    @PreAuthorize("hasAuthority('update-database-access')")
    @Operation(summary = "Modify access",
            description = "Modifies access of a user with given username to database with given id. Requires role `update-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Modified access"),
            @ApiResponse(responseCode = "400",
                    description = "Modify access query or database connection is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Modify access not permitted when no access is granted in the first place",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user not found",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Access could not be updated due to connection error in the data service",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Access could not be updated in the data service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> update(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @PathVariable("username") String username,
                                       @Valid @RequestBody CreateAccessDto data,
                                       Principal principal) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, SearchServiceException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        log.debug("endpoint modify database access, databaseId={}, username={}, access.type={}", databaseId, username,
                data.getType());
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwnedBy().equals(getUsername(principal))) {
            log.error("Failed to update access: not owner");
            throw new NotAllowedException("Failed to update access: not owner");
        }
        if (database.getOwnedBy().equals(username)) {
            log.error("Failed to update access: the owner must have write-all access");
            throw new NotAllowedException("Failed to update access: the owner must have write-all access");
        }
        accessService.find(database, username);
        accessService.update(database, username, data.getType());
        dashboardService.updateAccess(database, username, data.getType());
        return ResponseEntity.accepted()
                .build();
    }

    @RequestMapping(value = "/{username}", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_access_get")
    @PreAuthorize("hasAuthority('check-database-access') or hasAuthority('check-foreign-database-access')")
    @Operation(summary = "Find/Check access",
            description = "Finds or checks access of a user with given username to a database with given id. Requests with HTTP method **GET** return the access object, requests with HTTP method **HEAD** only the status. When the user has at least *READ* access, the status 200 is returned, 403 otherwise. Requires role `check-database-access` or `check-foreign-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found database access",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseAccessDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "No access to this database",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database not found",
                    content = {@Content}),
    })
    public ResponseEntity<DatabaseAccessDto> find(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                  @PathVariable("username") String username,
                                                  Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, AccessNotFoundException, NotAllowedException {
        log.debug("endpoint get database access, databaseId={}, username={}", databaseId, username);
        if (!username.equals(getUsername(principal))) {
            if (!hasRole(principal, "check-foreign-database-access")) {
                log.error("Failed to find access: foreign user");
                throw new NotAllowedException("Failed to find access: foreign user");
            }
            log.trace("principal is allowed to check foreign user access");
        }
        final Database database = databaseService.findById(databaseId);
        final DatabaseAccess access = accessService.find(database, username);
        return ResponseEntity.ok(metadataMapper.databaseAccessToDatabaseAccessDto(access));
    }

    @DeleteMapping("/{username}")
    @Transactional
    @Observed(name = "dbrepo_access_delete")
    @PreAuthorize("hasAuthority('delete-database-access')")
    @Operation(summary = "Delete access",
            description = "Delete access of a user with given username to a database with id. Requires role `delete-database-access`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted access"),
            @ApiResponse(responseCode = "400",
                    description = "Modify access query or database connection is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Revoke of access not permitted as no access was found",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "User, database with access was not found",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Access could not be created due to connection error",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Access could not be revoked in the data service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> revoke(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @PathVariable("username") String username,
                                       Principal principal) throws NotAllowedException, DataServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException, DataServiceConnectionException {
        log.debug("endpoint revoke database access, databaseId={}, username={}", databaseId, username);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwnedBy().equals(getUsername(principal))) {
            log.error("Failed to revoke access: not owner");
            throw new NotAllowedException("Failed to revoke access: not owner");
        }
        if (database.getOwnedBy().equals(username)) {
            log.error("Failed to revoke access: the owner must have write-all access");
            throw new NotAllowedException("Failed to revoke access: the owner must have write-all access");
        }
        accessService.find(database, username);
        accessService.delete(database, username);
        dashboardService.updateAccess(database, username, null);
        return ResponseEntity.accepted()
                .build();
    }

}
