package at.tuwien.endpoints;

import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.DraftDto;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.service.DocumentService;
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


@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/document")
public class DocumentEndpoint {

    private final DocumentService documentService;

    @Autowired
    public DocumentEndpoint(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Create a draft", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DraftDto> create(@NotNull @Valid @RequestBody CreateDraftDto data,
                                           @NotNull Principal principal) throws DraftRecordCreateException {
        final DraftDto document = documentService.create(data, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(document);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Find a draft", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DraftDto> find(@NotNull @PathVariable("id") String documentId,
                                         @NotNull Principal principal) throws DraftRecordCreateException {
        final DraftDto document = documentService.findById(documentId, principal);
        log.info("Found draft record with id {}", documentId);
        log.debug("found draft record {}", document);
        return ResponseEntity.status(HttpStatus.OK)
                .body(document);
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Reserve draft DOI", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DraftDto> reserve(@NotNull @PathVariable("id") String documentId,
                                            @NotNull Principal principal) throws DraftRecordCreateException {
        final DraftDto document = documentService.reserveDoi(documentId, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(document);
    }

}
