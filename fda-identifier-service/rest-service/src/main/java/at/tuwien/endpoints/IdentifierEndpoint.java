package at.tuwien.endpoints;

import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.service.IdentifierService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/identifier")
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
    @Operation(summary = "Find identifiers")
    public ResponseEntity<List<IdentifierDto>> findAll(@NotNull @PathVariable("id") Long containerId,
                                                       @NotNull @PathVariable("databaseId") Long databaseId,
                                                       @RequestParam(required = false) Long qid)
            throws IdentifierNotFoundException {
        if (qid != null) {
            final Identifier identifier = identifierService.find(containerId, databaseId, qid);
            log.info("Found identifier with id {} filtered by query id {}", identifier.getId(), qid);
            log.debug("found identifier {} filtered by query id {}", identifier, qid);
            return ResponseEntity.ok(List.of(identifierMapper.identifierToIdentifierDto(identifier)));
        }
        final List<Identifier> identifiers = identifierService.findAll(containerId, databaseId);
        log.info("Found {} identifiers", identifiers.size());
        log.debug("found identifiers {}", identifiers);
        return ResponseEntity.ok(identifiers.stream()
                .map(identifierMapper::identifierToIdentifierDto)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasPermission(#databaseId, 'IDENTIFIER_CREATE')")
    @Operation(summary = "Create identifier", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<IdentifierDto> create(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @Valid @RequestBody IdentifierCreateDto data,
                                                @NotNull @RequestHeader(name = "Authorization") String authorization,
                                                @NotNull Principal principal)
            throws IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException {
        final Identifier identifier = identifierService.create(containerId, databaseId, data, principal, authorization);
        log.info("Found identifier with id {}", identifier.getId());
        log.debug("found identifier {}", identifier);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(identifierMapper.identifierToIdentifierDto(identifier));
    }

    @PutMapping("/{identiferId}")
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Update some identifier", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<IdentifierDto> update(@NotNull @PathVariable("id") Long containerId,
                                                @NotNull @PathVariable("databaseId") Long databaseId,
                                                @NotNull @Valid @RequestParam("identiferId") Long identiferId,
                                                @NotNull @Valid @RequestBody IdentifierDto data)
            throws IdentifierPublishingNotAllowedException, IdentifierNotFoundException {
        final Identifier identifier = identifierService.update(containerId, databaseId, identiferId, data);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(identifierMapper.identifierToIdentifierDto(identifier));
    }

    @DeleteMapping("/{identiferId}")
    @PreAuthorize("hasRole('ROLE_DATA_STEWARD')")
    @Operation(summary = "Delete some identifer", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @Valid @RequestParam("identiferId") Long identiferId)
            throws IdentifierNotFoundException {
        identifierService.delete(containerId, databaseId, identiferId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .build();
    }
}
