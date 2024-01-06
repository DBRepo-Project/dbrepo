package at.tuwien.endpoints;

import at.tuwien.api.database.LicenseDto;
import at.tuwien.mapper.LicenseMapper;
import at.tuwien.service.LicenseService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/database")
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
    @Observed(name = "dbr_license_findall")
    @Operation(summary = "Get all licenses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of licenses",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = LicenseDto.class)))}),
    })
    public ResponseEntity<List<LicenseDto>> list() {
        log.debug("endpoint list licenses");
        final List<LicenseDto> licenses = licenseService.findAll()
                .stream()
                .map(licenseMapper::licenseToLicenseDto)
                .collect(Collectors.toList());
        log.trace("list licenses resulted in licenses {}", licenses);
        return ResponseEntity.status(HttpStatus.OK)
                .body(licenses);
    }

}
