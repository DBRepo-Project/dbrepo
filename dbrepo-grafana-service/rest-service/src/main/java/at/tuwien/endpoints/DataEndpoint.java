package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.panels.*;
import at.tuwien.service.DataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = DataEndpoint.API_PREFIX)
public class DataEndpoint {
    public static final String API_PREFIX = "/api/dashboard/data";
    private final DataService dataService;
    private static final Long DEFAULT_RESPONSE_SIZE = 100L;

    @Autowired
    public DataEndpoint(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping(PieChartPanel.RELATIVE_PATH + "/{dbId}/{viewId}")
    @Operation(summary = "Returns data for Pie Chart",
            description = "Returns data for Pie Chart for a specific database and view")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public Map<String, Object> getPieChart(@NotBlank @PathVariable Long dbId, @NotBlank @PathVariable Long viewId,
                                           @RequestParam(required = false) Long size) {
        if (size == null) {
            size = DEFAULT_RESPONSE_SIZE;
        }

        return dataService.getPieChartData(dbId, viewId, size);
    }

    @GetMapping(CntAllPanel.RELATIVE_PATH + "/{dbId}/{viewId}")
    @Operation(summary = "Returns data for the Count All Stats Visualization",
            description = "Returns data for the Count All Stats Visualization for a specific database and view")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public Map<String, Object> getCntAll(@NotBlank @PathVariable Long dbId, @NotBlank @PathVariable Long viewId) {

        return dataService.getCntAllData(dbId, viewId);
    }

    @GetMapping(TablePanel.RELATIVE_PATH + "/{dbId}/{tableId}")
    @Operation(summary = "Returns data for the Table Visualization",
            description = "Returns data for the Table Visualization for a specific database and table")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public List<Map<String, Object>> getTable(@NotBlank @PathVariable Long dbId,
                                              @NotBlank @PathVariable Long tableId,
                                              @RequestParam(required = false) Long size) {
        if (size == null) {
            size = DEFAULT_RESPONSE_SIZE;
        }

        return dataService.getTableData(dbId, tableId, size);
    }

    @GetMapping(HistogramPanel.RELATIVE_PATH + "/{dbId}/{viewId}")
    @Operation(summary = "Returns data for the Histogram Visualization",
            description = "Returns data for the Histogram Visualization for a specific database and view")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public Map<String, List<Object>> getHistogram(@NotBlank @PathVariable Long dbId, @NotBlank @PathVariable Long viewId,
                                                  @RequestParam(required = false) Long size) {
        if (size == null) {
            size = DEFAULT_RESPONSE_SIZE;
        }

        return dataService.getHistogramData(dbId, viewId, size);
    }

    @GetMapping(StatsPanel.RELATIVE_PATH + "/{dbId}/{tableId}")
    @Operation(summary = "Returns data for the Stats Visualization",
            description = "Returns data for the Stats Visualization for a specific database and table")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public List<Map<String, Object>> getStats(@NotBlank @PathVariable Long dbId, @NotBlank @PathVariable Long tableId) {
        return dataService.getStatsData(dbId, tableId);
    }

    @GetMapping(TimeSeriesPanel.RELATIVE_PATH + "/{dbId}/{viewId}")
    @Operation(summary = "Returns data for the Time Visualization",
            description = "Returns data for the Time Visualization for a specific database and view")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public Map<String, List<Map<String, Object>>> getTimeSeries(@NotBlank @PathVariable Long dbId, @NotBlank @PathVariable Long viewId,
                                                                @RequestParam(required = false) Long size) {
        if (size == null) {
            size = DEFAULT_RESPONSE_SIZE;
        }
        return dataService.getTimeSeriesData(dbId, viewId, size);
    }

    @GetMapping(MultiTimeSeriesPanel.RELATIVE_PATH + "/{dbId}/{viewId}")
    @Operation(summary = "Returns data for the Multi Time Visualization",
            description = "Returns data for the MUlti Time Visualization for a specific database and view")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "500",
                    description = "Internal error",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public Map<String, List<Map<String, Object>>> getMultiTimeSeries(@NotBlank @PathVariable Long dbId, @NotBlank @PathVariable Long viewId,
                                                                @RequestParam(required = false) Long size) {
        if (size == null) {
            size = DEFAULT_RESPONSE_SIZE;
        }
        return dataService.getMultiTimeSeriesData(dbId, viewId, size);
    }
}
