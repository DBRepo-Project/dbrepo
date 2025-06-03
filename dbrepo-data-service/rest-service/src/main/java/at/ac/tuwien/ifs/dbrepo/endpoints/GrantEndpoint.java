package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseGrantsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.GrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.SQLException;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/grant")
public class GrantEndpoint extends RestEndpoint {

    private final CacheService cacheService;
    private final GrantService grantService;

    @Autowired
    public GrantEndpoint(CacheService cacheService, GrantService grantService) {
        this.cacheService = cacheService;
        this.grantService = grantService;
    }

    @RequestMapping(path = "/{userId}", method = {RequestMethod.GET, RequestMethod.HEAD})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get grants",
            description = "Get the grant permissions for a user of a given database.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Access found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseAccessDto[].class))}),
            @ApiResponse(responseCode = "401",
                    description = "Not authenticated",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not database owner or foreign user",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Grants malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<DatabaseGrantsDto> find(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                  @PathVariable("userId") UUID userId,
                                                  Principal principal,
                                                  @NotNull HttpServletRequest request) throws DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseMalformedException,
            DatabaseUnavailableException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint check access to database, databaseId={}", databaseId);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        final UserDto user = cacheService.getUser(userId);
        if (!database.getOwner().getId().equals(getId(principal)) && !user.getId().equals(getId(principal))) {
            log.error("Failed to find access: not owner or foreign user");
            throw new NotAllowedException("Failed to find access: not owner or foreign user");
        }
        try {
            final DatabaseGrantsDto grants = grantService.find(database, user);
            final DatabaseGrantsDto body = request.getMethod().equals("HEAD") ? null : grants;
            if (grants.getType() == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(body);
            }
            return ResponseEntity.ok()
                    .body(body);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
