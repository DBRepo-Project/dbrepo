package at.tuwien.endpoints;

import at.tuwien.api.database.ViewDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.file.UploadResponseDto;
import at.tuwien.exception.*;
import at.tuwien.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/upload")
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
                    description = "Uploaded the file",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the storage service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<UploadResponseDto> create(@NotNull @RequestParam("file") MultipartFile file) throws DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException, MetadataServiceException {
        log.debug("endpoint upload file, file.originalFilename={}", file.getOriginalFilename());
        try {
            final String key = storageService.putObject(file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UploadResponseDto.builder()
                            .s3Key(key)
                            .build());
        } catch (IOException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

}
