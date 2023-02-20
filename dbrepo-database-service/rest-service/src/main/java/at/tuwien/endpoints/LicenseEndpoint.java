package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.mapper.LicenseMapper;
import at.tuwien.service.LicenseService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/container/{id}/database")
public class LicenseEndpoint {

    private final LicenseMapper licenseMapper;
    private final LicenseService licenseService;

    @Autowired
    public LicenseEndpoint(LicenseMapper licenseMapper, LicenseService licenseService) {
        this.licenseMapper = licenseMapper;
        this.licenseService = licenseService;
    }

    @GetMapping("/license")
    @Transactional(readOnly = true)
    @Timed(value = "license.list", description = "Time needed to list the licenses")
    @Operation(summary = "Get all licenses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of licenses",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseBriefDto.class))}),
    })
    public ResponseEntity<List<LicenseDto>> list(@NotBlank @PathVariable("id") Long containerId) {
        log.debug("endpoint list licenses, containerId={}", containerId);
        final List<LicenseDto> licenses = licenseService.findAll()
                .stream()
                .map(licenseMapper::licenseToLicenseDto)
                .collect(Collectors.toList());
        log.trace("list licenses resulted in licenses {}", licenses);
        return ResponseEntity.status(HttpStatus.OK)
                .body(licenses);
    }

}
