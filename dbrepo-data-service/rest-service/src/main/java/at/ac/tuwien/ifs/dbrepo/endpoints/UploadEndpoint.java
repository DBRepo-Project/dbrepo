package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/upload")
public class UploadEndpoint extends RestEndpoint {

    private final StorageService storageService;

    @Autowired
    public UploadEndpoint(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('upload-file')")
    @Operation(summary = "Uploads a multipart file",
            description = "Uploads a multipart file to the Storage Service. Requires role `upload-file`.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    headers = {@Header(name = "X-S3-Key", description = "The S3 filename", schema = @Schema(implementation = String.class), required = true),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose `X-S3-Key` custom header", schema = @Schema(implementation = String.class), required = true)},
                    description = "Uploaded the file",
                    content = {@Content}),
            @ApiResponse(responseCode = "204",
                    headers = {@Header(name = "X-S3-Key", description = "The S3 filename", schema = @Schema(implementation = String.class), required = true),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose `X-S3-Key` custom header", schema = @Schema(implementation = String.class), required = true)},
                    description = "File already present",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the storage service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> create(@NotNull @RequestParam("file") MultipartFile file)
            throws StorageUnavailableException {
        log.atDebug()
                .setMessage("endpoint upload file")
                .addKeyValue("file", file)
                .log();
        final String s3key = file.getOriginalFilename();
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Expose-Headers", "X-S3-Key");
        headers.set("X-S3-Key", s3key);
        try {
            storageService.putObject(s3key, file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .headers(headers)
                    .build();
        } catch (IOException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        } catch (StorageObjectExistsException e) {
            log.info("Object with key {} already exists, skip", s3key);
            return ResponseEntity.noContent()
                    .headers(headers)
                    .build();
        }
    }

}
