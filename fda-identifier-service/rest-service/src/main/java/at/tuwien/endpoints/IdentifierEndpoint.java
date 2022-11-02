package at.tuwien.endpoints;

import at.tuwien.ExportResource;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.service.IdentifierService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/identifier")
public class IdentifierEndpoint {

    private final IdentifierMapper identifierMapper;
    private final IdentifierService identifierService;

    @Autowired
    public IdentifierEndpoint(IdentifierMapper identifierMapper, IdentifierService identifierService) {
        this.identifierMapper = identifierMapper;
        this.identifierService = identifierService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "identifier.list", description = "Time needed to list the identifiers")
    @Operation(summary = "Find identifiers")
    public ResponseEntity<List<IdentifierDto>> list(@RequestParam(required = false) Long dbid,
                                                       @RequestParam(required = false) Long qid) {
        log.debug("endpoint find identifiers, dbid={}, qid={}", dbid, qid);
        final List<Identifier> identifiers = identifierService.findAll(dbid, qid);
        final List<IdentifierDto> dto = identifiers.stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .collect(Collectors.toList());
        log.info("Find identifiers resulted in {} identifiers", identifiers.size());
        log.trace("endpoint find identifiers, list={}", dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    @Deprecated
    @Transactional(readOnly = true)
    @Timed(value = "identifier.export", description = "Time needed to export an identifier")
    @Operation(summary = "Export some identifier metadata")
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long id)
            throws IdentifierNotFoundException {
        log.debug("endpoint export identifier, id={}", id);
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = identifierService.exportMetadata(id);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }

    @PostMapping
    @Transactional
    @Timed(value = "identifier.create", description = "Time needed to create an identifier")
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Create identifier", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<IdentifierDto> create(@NotNull @Valid @RequestBody IdentifierCreateDto data,
                                                @NotNull @RequestHeader(name = "Authorization") String authorization,
                                                @NotNull Principal principal)
            throws IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException, IdentifierRequestException {
        log.debug("endpoint create identifier, data={}, authorization={}, principal={}", data, authorization, principal);
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
    @PreAuthorize("hasRole('ROLE_RESEARCHER') or hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Update some identifier", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<IdentifierDto> update(@NotNull @Valid @RequestParam("id") Long id,
                                                @NotNull @Valid @RequestBody IdentifierDto data)
            throws IdentifierPublishingNotAllowedException, IdentifierNotFoundException {
        log.debug("endpoint update identifier, id={}, data={}", id, data);
        final Identifier identifier = identifierService.update(id, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(identifierMapper.identifierToIdentifierDto(identifier));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Timed(value = "identifier.delete", description = "Time needed to delete an identifier")
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Delete some identifer", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @Valid @RequestParam("id") Long id)
            throws IdentifierNotFoundException {
        identifierService.delete(id);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .build();
    }
}
