package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.time.Instant;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/export")
public class ExportEndpoint extends AbstractEndpoint {

    private final QueryService queryService;

    @Autowired
    public ExportEndpoint(QueryService queryService, DatabaseService databaseService, AccessService accessService,
                          IdentifierService identifierService, TableService tableService, QueryConfig queryConfig) {
        super(tableService, accessService, databaseService, identifierService, queryConfig);
        this.queryService = queryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "table.export", description = "Time needed to export table data")
    @Operation(summary = "Export table", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Exported table successfully",
                    content = {@Content(
                            mediaType = "text/csv",
                            schema = @Schema(implementation = IdentifierDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Pagination is not within the allowed range or the export query is malformed or the accept header is invalid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, container, user or database could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Table export is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Exported resource could not be retrieved from the database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "Exported resource could not be temporarily stored in the filesystem",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Container image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long containerId,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      @NotNull @PathVariable("tableId") Long tableId,
                                                      @RequestParam(required = false) Instant timestamp,
                                                      Principal principal,
                                                      @RequestHeader(HttpHeaders.ACCEPT) String accept)
            throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            FileStorageException, NotAllowedException, QueryMalformedException, UserNotFoundException, HeaderInvalidException {
        log.debug("endpoint export table, id={}, databaseId={}, tableId={}, timestamp={}, principal={}", containerId, databaseId,
                tableId, timestamp, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_EXPORT", principal)) {
            log.error("Missing data export permission");
            throw new NotAllowedException("Missing data export permission");
        }
        if (accept == null || accept.equals("text/csv")) {
            final HttpHeaders headers = new HttpHeaders();
            final ExportResource resource = queryService.findAll(containerId, databaseId, tableId, timestamp, principal);
            headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
            log.trace("export table resulted in resource {}", resource);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource.getResource());
        }
        log.error("Failed to export, non-csv exports are not supported");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .build();
    }


}
