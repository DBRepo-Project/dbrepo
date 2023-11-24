package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.service.AccessService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.MetadataService;
import at.tuwien.service.UserService;
import at.tuwien.utils.PrincipalUtil;
import at.tuwien.utils.UserUtil;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/identifier")
public class IdentifierEndpoint {

    private final AccessService accessService;
    private final MetadataService metadataService;
    private final IdentifierMapper identifierMapper;
    private final IdentifierService identifierService;

    @Autowired
    public IdentifierEndpoint(AccessService accessService, MetadataService metadataService,
                              IdentifierMapper identifierMapper, IdentifierService identifierService) {
        this.accessService = accessService;
        this.metadataService = metadataService;
        this.identifierMapper = identifierMapper;
        this.identifierService = identifierService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_identifier_findall")
    @Operation(summary = "Find identifiers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List identifiers",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = IdentifierDto.class)))}),
    })
    public ResponseEntity<List<IdentifierDto>> list(@RequestParam(required = false) Long dbid,
                                                    @RequestParam(required = false) Long qid,
                                                    @RequestParam(required = false) Long vid,
                                                    @RequestParam(required = false) IdentifierTypeDto type) {
        log.debug("endpoint find identifiers, dbid={}, qid={}, vid={}, type={}", dbid, qid, vid, type);
        final List<IdentifierDto> dto = identifierService.findAll(type, dbid, qid, vid)
                .stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .collect(Collectors.toList());
        log.info("Find identifiers resulted in {} identifiers", dto.size());
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Transactional
    @Observed(name = "dbr_identifier_create")
    @PreAuthorize("hasAuthority('create-identifier') or hasAuthority('create-foreign-identifier')")
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
            @ApiResponse(responseCode = "403",
                    description = "Insufficient access rights or authorities",
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
    public ResponseEntity<IdentifierDto> create(@NotNull @Valid @RequestBody IdentifierSaveDto data,
                                                @NotNull Principal principal)
            throws IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException, IdentifierRequestException,
            NotAllowedException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException {
        log.debug("endpoint create identifier, data={}, {}", data, PrincipalUtil.formatForDebug(principal));
        if (data.getType().equals(IdentifierTypeDto.SUBSET) && (data.getQueryId() == null || data.getViewId() != null)) {
            log.error("Identifier of type subset need to have a qid and not a vid present");
            throw new IdentifierRequestException("Identifier of type subset need to have a qid and not a vid present");
        } else if (data.getType().equals(IdentifierTypeDto.DATABASE) && (data.getQueryId() != null || data.getViewId() != null)) {
            log.error("Identifier of type database must not have a qid and not a vid present");
            throw new IdentifierRequestException("Identifier of type database must not have a qid and not a vid present");
        } else if (data.getType().equals(IdentifierTypeDto.VIEW) && data.getQueryId() != null) {
            log.error("Identifier of type view must not have a qid present");
            throw new IdentifierRequestException("Identifier of type database must not have a qid present");
        }
        try {
            accessService.find(data.getDatabaseId(), UserUtil.getId(principal));
        } catch (AccessDeniedException e) {
            if (!UserUtil.hasRole(principal, "create-foreign-identifier")) {
                log.error("Failed to create identifier: insufficient access");
                throw new NotAllowedException("Failed to create identifier: insufficient access");
            }
        }
        final Identifier identifier = identifierService.create(data, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(identifierMapper.identifierToIdentifierDto(identifier));
    }

    @GetMapping("/retrieve")
    @Observed(name = "dbr_identifier_retrieve")
    @Operation(summary = "Retrieve metadata from identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved metadata from identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
    })
    public ResponseEntity<ExternalMetadataDto> retrieve(@NotNull @Valid @RequestParam String url)
            throws OrcidNotFoundException, RorNotFoundException, RemoteUnavailableException, DoiNotFoundException {
        return ResponseEntity.ok(metadataService.findByUrl(url));
    }


}
