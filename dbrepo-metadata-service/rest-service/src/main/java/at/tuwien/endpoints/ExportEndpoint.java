package at.tuwien.endpoints;

import at.tuwien.ExportResource;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.utils.PrincipalUtil;
import at.tuwien.utils.UserUtil;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/database/{id}/table/{tableId}/export")
public class ExportEndpoint {

    private final QueryService queryService;
    private final DatabaseService databaseService;

    @Autowired
    public ExportEndpoint(QueryService queryService, DatabaseService databaseService) {
        this.queryService = queryService;
        this.databaseService = databaseService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_table_export")
    @Operation(summary = "Export table", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created identifier",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Images is not supported or table/query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Operation is not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database or user was not found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Failed to export file from sidecar",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "Blob storage operation could not be completed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Sidecar operation could not be completed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Database connection could not be established",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long databaseId,
                                                      @NotNull @PathVariable("tableId") Long tableId,
                                                      @RequestParam(required = false) Instant timestamp,
                                                      Principal principal)
            throws TableNotFoundException, DatabaseNotFoundException, FileStorageException, QueryMalformedException,
            NotAllowedException, DataDbSidecarException, DataProcessingException {
        log.debug("endpoint export table, id={}, tableId={}, timestamp={}, {}", databaseId, tableId, timestamp, PrincipalUtil.formatForDebug(principal));
        final Database database = databaseService.find(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to export private table: principal is null");
                throw new NotAllowedException("Failed to export private table: principal is null");
            }
            if (!UserUtil.hasRole(principal, "export-table-data")) {
                log.error("Failed to export private table: role missing");
                throw new NotAllowedException("Failed to export private table: role missing");
            }
        }
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = queryService.tableFindAll(databaseId, tableId, timestamp, principal);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        log.trace("export table resulted in resource {}", resource);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }


}
