package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.ld.LdDatasetDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.service.IdentifierService;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/pid",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class PersistenceEndpoint {

    private final EndpointConfig endpointConfig;
    private final IdentifierMapper identifierMapper;
    private final IdentifierService identifierService;

    @Autowired
    public PersistenceEndpoint(EndpointConfig endpointConfig, IdentifierMapper identifierMapper,
                               IdentifierService identifierService) {
        this.endpointConfig = endpointConfig;
        this.identifierMapper = identifierMapper;
        this.identifierService = identifierService;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json"})
    @Transactional(readOnly = true)
    @Observed(name = "dbr_pid_findall")
    @Operation(summary = "Find all identifiers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found identifiers successfully",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = IdentifierDto[].class)),
                            @Content(mediaType = "application/ld+json", schema = @Schema(implementation = LdDatasetDto[].class))
                    }),
            @ApiResponse(responseCode = "406",
                    description = "Identifier could not be exported, the requested style is not known",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> findAll(@Valid @RequestParam(value = "dbid", required = false) Long dbid,
                                     @Valid @RequestParam(value = "qid", required = false) Long qid,
                                     @Valid @RequestParam(value = "vid", required = false) Long vid,
                                     @Valid @RequestParam(value = "tid", required = false) Long tid,
                                     @RequestHeader(HttpHeaders.ACCEPT) String accept) throws FormatNotAvailableException {
        log.debug("endpoint find identifiers, dbid={}, qid={}, vid={}, tid={}, accept={}", dbid, qid, vid, tid, accept);
        final List<Identifier> identifiers = identifierService.findAll()
                .stream()
                .filter(i -> !Objects.nonNull(dbid) || i.getDatabaseId().equals(dbid))
                .filter(i -> !Objects.nonNull(qid) || i.getQueryId().equals(qid))
                .filter(i -> !Objects.nonNull(vid) || i.getViewId().equals(vid))
                .filter(i -> !Objects.nonNull(tid) || i.getTableId().equals(tid))
                .toList();
        if (identifiers.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        log.trace("found persistent identifiers {}", identifiers);
        switch (accept) {
            case "application/json":
                log.trace("accept header matches json");
                final List<IdentifierDto> resource1 = identifiers.stream()
                        .map(identifierMapper::identifierToIdentifierDto)
                        .toList();
                log.debug("find identifier resulted in identifiers {}", resource1);
                return ResponseEntity.ok(resource1);
            case "application/ld+json":
                log.trace("accept header matches json-ld");
                final List<LdDatasetDto> resource2 = identifiers.stream()
                        .map(i -> identifierMapper.identifierToLdDatasetDto(i, endpointConfig.getWebsiteUrl()))
                        .toList();
                log.debug("find identifier resulted in identifiers {}", resource2);
                return ResponseEntity.ok(resource2);
        }
        throw new FormatNotAvailableException("Must provide either application/json or application/ld+json headers");
    }


    @GetMapping(value = "/{pid}", produces = {MediaType.APPLICATION_JSON_VALUE, "application/ld+json",
            MediaType.TEXT_XML_VALUE, "text/csv", "text/bibliography", "text/bibliography; style=apa",
            "text/bibliography; style=ieee", "text/bibliography; style=bibtex"})
    @Transactional(readOnly = true)
    @Observed(name = "dbr_pid_find")
    @Operation(summary = "Find some identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found identifier successfully",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = IdentifierDto.class)),
                            @Content(mediaType = "application/ld+json", schema = @Schema(implementation = LdDatasetDto.class)),
                            @Content(mediaType = "text/csv"),
                            @Content(mediaType = "text/xml"),
                            @Content(mediaType = "text/bibliography"),
                            @Content(mediaType = "text/bibliography; style=apa"),
                            @Content(mediaType = "text/bibliography; style=ieee"),
                            @Content(mediaType = "text/bibliography; style=bibtex"),
                    }),
            @ApiResponse(responseCode = "400",
                    description = "Identifier could not be exported, the requested style is not known",
                    content = {@Content(
                            mediaType = "text/bibliography",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Identifier could not be found",
                    content = {@Content(
                            mediaType = "text/csv",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Exported resource was not found",
                    content = {@Content(
                            mediaType = "text/csv",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "Failed to retrieve from S3 endpoint",
                    content = {@Content(
                            mediaType = "text/csv",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Failed to retrieve from database sidecar",
                    content = {@Content(
                            mediaType = "text/csv",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Identifier could not exported from database as it is not reachable",
                    content = {@Content(
                            mediaType = "text/csv",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> find(@Valid @PathVariable("pid") Long pid,
                                  @RequestHeader(HttpHeaders.ACCEPT) String accept,
                                  @NotNull Principal principal) throws IdentifierNotFoundException,
            QueryNotFoundException, IdentifierRequestException, UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, FileStorageException, DataDbSidecarException, DataProcessingException {
        log.debug("endpoint find identifier, pid={}, accept={}", pid, accept);
        final Identifier identifier = identifierService.find(pid);
        log.info("Found persistent identifier with id {}", identifier.getId());
        log.trace("found persistent identifier {}", identifier);
        if (accept != null) {
            log.trace("accept header present: {}", accept);
            switch (accept) {
                case "application/json":
                    log.trace("accept header matches json");
                    final IdentifierDto resource1 = identifierMapper.identifierToIdentifierDto(identifier);
                    log.debug("find identifier resulted in identifier {}", resource1);
                    return ResponseEntity.ok(resource1);
                case "application/ld+json":
                    log.trace("accept header matches json-ld");
                    final LdDatasetDto resource2 = identifierMapper.identifierToLdDatasetDto(identifier, endpointConfig.getWebsiteUrl());
                    log.debug("find identifier resulted in identifier {}", resource2);
                    return ResponseEntity.ok(resource2);
                case "text/csv":
                    log.trace("accept header matches csv");
                    final InputStreamResource resource3;
                    try {
                        resource3 = identifierService.exportResource(pid, principal);
                        log.debug("find identifier resulted in resource {}", resource3);
                        return ResponseEntity.ok(resource3);
                    } catch (IdentifierRequestException e) {
                        /* ignore */
                    }
                case "text/xml":
                    log.trace("accept header matches xml");
                    final InputStreamResource resource4 = identifierService.exportMetadata(pid);
                    log.debug("find identifier resulted in resource {}", resource4);
                    return ResponseEntity.ok(resource4);
            }
            final Pattern regex = Pattern.compile("text\\/bibliography(; ?style=(apa|ieee|bibtex))?");
            final Matcher matcher = regex.matcher(accept);
            if (matcher.find()) {
                log.trace("accept header matches bibliography");
                final BibliographyTypeDto style;
                if (matcher.group(2) != null) {
                    style = BibliographyTypeDto.valueOf(matcher.group(2).toUpperCase());
                    log.trace("bibliography style matches {}", style);
                } else {
                    style = BibliographyTypeDto.APA;
                    log.trace("no bibliography style provided, default: {}", style);
                }
                final String resource = identifierService.exportBibliography(pid, style);
                log.debug("find identifier resulted in resource {}", resource);
                return ResponseEntity.ok(resource);
            }
        } else {
            log.trace("no accept header present");
        }
        final HttpHeaders headers = new HttpHeaders();
        final String url = identifierMapper.identifierToLocationUrl(endpointConfig.getWebsiteUrl(), identifier);
        headers.add("Location", url);
        log.debug("find identifier resulted in http redirect, headers={}, url={}", headers, url);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .headers(headers)
                .build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Observed(name = "dbr_pid_delete")
    @PreAuthorize("hasAuthority('delete-identifier')")
    @Operation(summary = "Delete some identifier", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted identifier"),
            @ApiResponse(responseCode = "403",
                    description = "Deleting identifier not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Identifier or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long id)
            throws IdentifierNotFoundException, NotAllowedException, DatabaseNotFoundException {
        log.debug("endpoint delete identifier, id={}", id);
        final Identifier identifier = identifierService.find(id);
        if (identifier.getDoi() != null) {
            log.error("Failed to delete identifier: a DOI is already attached");
            throw new NotAllowedException("Failed to delete identifier: a DOI is already attached");
        }
        identifierService.delete(id);
        log.info("Deleted identifier with pid: {}", id);
        return ResponseEntity.accepted()
                .build();
    }

}
