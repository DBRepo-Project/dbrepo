package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.AccessService;
import at.tuwien.utils.PrincipalUtil;
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
@RequestMapping("/api/database/{id}/access")
public class AccessEndpoint {

    private final AccessService accessService;
    private final DatabaseMapper databaseMapper;

    @Autowired
    public AccessEndpoint(AccessService accessService, DatabaseMapper databaseMapper) {
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
    }

    @PostMapping("/{userId}")
    @Transactional
    @Observed(name = "dbr_access_give")
    @PreAuthorize("hasAuthority('create-database-access')")
    @Operation(summary = "Give access to some database", security = @SecurityRequirement(name = "bearerAuth"))
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
    public ResponseEntity<?> create(@NotBlank @PathVariable("id") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @Valid @RequestBody DatabaseGiveAccessDto accessDto,
                                    @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        log.debug("endpoint give access to database, databaseId={}, userId={}, accessDto={}, {}", databaseId, userId, accessDto, PrincipalUtil.formatForDebug(principal));
        try {
            accessService.find(databaseId, userId);
            log.error("Failed to give access to user with id {}: already has access", userId);
            throw new NotAllowedException("Failed to give access to user with id " + userId + ": already has access");
        } catch (AccessDeniedException e) {
            /* ignore */
        }
        accessService.create(databaseId, userId, accessDto);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{userId}")
    @Transactional
    @Observed(name = "dbr_access_modify")
    @PreAuthorize("hasAuthority('update-database-access')")
    @Operation(summary = "Modify access to some database", security = @SecurityRequirement(name = "bearerAuth"))
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
    public ResponseEntity<?> update(@NotBlank @PathVariable("id") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @Valid @RequestBody DatabaseModifyAccessDto accessDto,
                                    @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException, AccessDeniedException {
        log.debug("endpoint modify access to database, databaseId={}, userId={}, accessDto={}, {}", databaseId, userId, accessDto, PrincipalUtil.formatForDebug(principal));
        accessService.find(databaseId, userId);
        accessService.update(databaseId, userId, accessDto);
        return ResponseEntity.accepted()
                .build();
    }

    @GetMapping
    @Transactional
    @Observed(name = "dbr_access_check")
    @PreAuthorize("hasAuthority('check-database-access')")
    @Operation(summary = "Check access to some database", security = @SecurityRequirement(name = "bearerAuth"))
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
    public ResponseEntity<DatabaseAccessDto> find(@NotBlank @PathVariable("id") Long databaseId,
                                                  @NotNull Principal principal) throws NotAllowedException,
            AccessDeniedException, DatabaseNotFoundException {
        log.debug("endpoint check access to database, databaseId={}, {}", databaseId, PrincipalUtil.formatForDebug(principal));
        final DatabaseAccess access = accessService.find(databaseId, UserUtil.getId(principal));
        final DatabaseAccessDto dto = databaseMapper.databaseAccessToDatabaseAccessDto(access);
        log.trace("check access resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{userId}")
    @Transactional
    @Observed(name = "dbr_access_delete")
    @PreAuthorize("hasAuthority('delete-database-access')")
    @Operation(summary = "Revoke access to some database", security = @SecurityRequirement(name = "bearerAuth"))
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
    public ResponseEntity<?> revoke(@NotBlank @PathVariable("id") Long databaseId,
                                    @NotBlank @PathVariable("userId") UUID userId,
                                    @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException, AccessDeniedException {
        log.debug("endpoint revoke access to database, databaseId={}, userId={}, {}", databaseId, userId, PrincipalUtil.formatForDebug(principal));
        accessService.find(databaseId, userId);
        accessService.delete(databaseId, userId);
        return ResponseEntity.accepted()
                .build();
    }

}
