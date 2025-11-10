package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/image")
public class AnalyseEndpoint extends RestEndpoint {

    private final AnalyseService analyseService;
    private final MetadataService metadataService;

    @Autowired
    public AnalyseEndpoint(AnalyseService analyseService, MetadataService metadataService) {
        this.analyseService = analyseService;
        this.metadataService = metadataService;
    }

    @GetMapping("/{imageId}/analyse/schema/{key}")
    @PreAuthorize("hasAuthority('analyse-datatypes')")
    @Operation(summary = "Analyse schema",
            description = "Analyses a dataset stored at the Storage Service and attempts to map the datatypes, requires role `analyse-datatypes`.",
            security = {@SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Analysed schema successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SchemaAnalysisResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Schema is malformed or does not fit the image",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find image or dataset",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection to metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<SchemaAnalysisResultDto> analyseDatatypes(@PathVariable("imageId") UUID imageId,
                                                                    @PathVariable("key") String key)
            throws AnalyseDataTypesException, DatabaseUnavailableException, StorageNotFoundException,
            RemoteUnavailableException, MetadataServiceException, ImageNotFoundException,
            ColumnNotFoundException, ImageInvalidException {
        log.debug("endpoint analyse datatypes, imageId={}, key={}", imageId, key);
        final Image image = metadataService.getImage(imageId);
        return ResponseEntity.ok()
                .body(analyseService.determineDataTypes(image, key));
    }

}
