package at.tuwien.endpoints;

import at.tuwien.api.document.file.FileDto;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.security.Principal;


@Log4j2
@RestController
@CrossOrigin(origins = "*")
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
    @Operation(summary = "Upload file", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<FileDto> uploadFile(@NotNull @PathVariable("id") String documentId,
                                              @NotNull @RequestParam("file") MultipartFile file,
                                              @NotNull Principal principal)
            throws DraftRecordCreateException, CommitFileUploadException, FileUploadException,
            org.apache.tomcat.util.http.fileupload.FileUploadException {
        final FileDto document = fileService.uploadFile(documentId, file, principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(document);
    }

}
