package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.UserService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/identifier")
public class IdentifierEndpoint extends AbstractEndpoint {

    private final IdentifierMapper identifierMapper;
    private final IdentifierService identifierService;

    @Autowired
    public IdentifierEndpoint(IdentifierMapper identifierMapper, IdentifierService identifierService,
                              DatabaseService databaseService, UserService userService) {
        super(userService, databaseService);
        this.identifierMapper = identifierMapper;
        this.identifierService = identifierService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "identifier.list", description = "Time needed to list the identifiers")
    @Operation(summary = "Find identifiers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List identifiers",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto[].class))}),
    })
    public ResponseEntity<List<IdentifierDto>> list(@RequestParam(required = false) Long dbid,
                                                    @RequestParam(required = false) Long qid,
                                                    @RequestParam(required = false) IdentifierTypeDto type) {
        log.debug("endpoint find identifiers, dbid={}, qid={}, type={}", dbid, qid, type);
        List<Identifier> identifiers = new LinkedList<>();
        try {
            identifiers = identifierService.findAll(dbid, qid);
        } catch (IdentifierNotFoundException e) {
            /* ignore */
        }
        final List<IdentifierDto> dto = identifiers.stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .filter(i -> {
                    if (type != null) {
                        return i.getType().equals(type);
                    }
                    return true;
                })
                .collect(Collectors.toList());
        log.info("Find identifiers resulted in {} identifiers", identifiers.size());
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Transactional
    @Timed(value = "identifier.create", description = "Time needed to create an identifier")
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Create identifier", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Identifier form contains invalid request data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Query, database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Creating identifier not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "Creating identifier not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Identifier for this resource already exists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "502",
                    description = "Query information could not be retrieved",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<IdentifierDto> create(@NotNull @Valid @RequestBody IdentifierCreateDto data,
                                                @NotNull @RequestHeader(name = "Authorization") String authorization,
                                                @NotNull Principal principal)
            throws IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException, IdentifierRequestException,
            NotAllowedException {
        log.debug("endpoint create identifier, data={}, authorization={}, principal={}", data, authorization, principal);
        if (!hasDatabasePermission(data.getCid(), data.getDbid(), "CREATE_IDENTIFIER", principal)) {
            log.error("Missing identifier create permission");
            throw new NotAllowedException("Missing identifier create permission");
        }
        if (data.getType().equals(IdentifierTypeDto.SUBSET) && data.getQid() == null) {
            log.error("Identifier of type subset need to have a qid present");
            throw new IdentifierRequestException("Identifier of type subset need to have a qid present");
        } else if (data.getType().equals(IdentifierTypeDto.DATABASE) && data.getQid() != null) {
            log.error("Identifier of type database must not have a qid present");
            throw new IdentifierRequestException("Identifier of type database must not have a qid present");
        }
        final Identifier identifier = identifierService.create(data, principal, authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(identifierMapper.identifierToIdentifierDto(identifier));
    }

    @PutMapping("/{id}")
    @Transactional
    @Timed(value = "identifier.update", description = "Time needed to update an identifier")
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Update some identifier", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Identifier could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "Updating identifier not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<IdentifierDto> update(@NotNull @PathVariable("id") Long id,
                                                @NotNull @Valid @RequestBody IdentifierDto data)
            throws IdentifierPublishingNotAllowedException, IdentifierNotFoundException {
        log.debug("endpoint update identifier, id={}, data={}", id, data);
        final Identifier identifier = identifierService.update(id, data);
        return ResponseEntity.accepted()
                .body(identifierMapper.identifierToIdentifierDto(identifier));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Timed(value = "identifier.delete", description = "Time needed to delete an identifier")
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Delete some identifier", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted identifier",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Identifier could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long id)
            throws IdentifierNotFoundException {
        log.debug("endpoint delete identifier, id={}", id);
        identifierService.delete(id);
        return ResponseEntity.accepted()
                .build();
    }
}
