package at.tuwien.endpoints;

import at.tuwien.api.document.file.FileStartDto;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;


@Log4j2
@RestController
@CrossOrigin(origins = "*")
@ControllerAdvice
@RequestMapping("/api/document/{id}/file")
public class FileEndpoint {

    private final FileService fileService;

    @Autowired
    public FileEndpoint(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Transactional(readOnly = true)
    @Operation(summary = "Start draft files", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<FileStartDto> start(@NotNull @PathVariable("id") String documentId,
                                           @NotNull Principal principal) throws DraftRecordCreateException {
        final FileStartDto document = fileService.start(documentId, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(document);
    }

}
