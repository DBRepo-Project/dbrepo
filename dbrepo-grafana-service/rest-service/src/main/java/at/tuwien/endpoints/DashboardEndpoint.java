package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.dto.DashboardConfigDto;
import at.tuwien.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/dashboard")
public class DashboardEndpoint {
    private final DashboardService dashboardService;

    @Autowired
    public DashboardEndpoint(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/generate/{dbId}")
    @Operation(summary = "Generate dashboard",
            description = "Generates dashboard for a provided database id.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created a new dashboard",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class))}),
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<String> generateDashboard(@RequestHeader("Authorization") String token,
                            @NotBlank @PathVariable("dbId") Long dbId,
                            @RequestBody(required = false) DashboardConfigDto configDto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.dashboardService.generateDashboard(dbId, token, configDto));
    }

    @RequestMapping(value = "/exists/{dbId}", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Operation(summary = "Check if dashboard exists",
            description = "Checks if a dashboard for a provided database id exists.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Returns Boolean if dashboard exsists",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class))}),
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<Boolean> checkIfDashboardExists(@NotBlank @PathVariable("dbId") Long dbId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(this.dashboardService.checkIfDashboardExists(dbId));
    }

    @DeleteMapping("/{dbId}")
    @Operation(summary = "Delete dashboard in Grafana",
            description = "Deletes a dashboard in Grafana for a provided id.",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted grafana dashbaord"),
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> removeDashboard(@NotBlank @PathVariable("dbId") Long dbId) {
        this.dashboardService.removeDashboard(dbId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
