package at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LdDatasetDto {

    @NotNull
    @JsonProperty("@context")
    @Schema(description = "The context schema URI", example = "https://schema.org/")
    private String context;

    @NotNull
    @JsonProperty("@type")
    @Schema(description = "The type", example = "Dataset")
    private String type;

    @NotNull
    @Schema(description = "The name", example = "Machine Failure Prediction using Sensor data")
    private String name;

    @NotNull
    @Schema(description = "The description", example = "This dataset contains sensor data collected from various machines, with the aim of predicting machine failures in advance. It includes a variety of sensor readings as well as the recorded machine failures.")
    private String description;

    @NotNull
    @Schema(description = "The URL", example = "https://handle.test.datacite.org/10.82556/cdht-0x47")
    private String url;

    @NotNull
    private List<String> identifier = new LinkedList<>();

    private String license;

    @NotNull
    private List<LdCreatorDto> creator = new LinkedList<>();

    @NotNull
    @Schema(description = "The ciration URL", example = "https://handle.test.datacite.org/10.82556/cdht-0x47")
    private String citation;

    @NotNull
    private List<LdDatasetDto> hasPart = new LinkedList<>();

    @NotNull
    @Schema(description = "The temporal coverage", example = "2025")
    private String temporalCoverage;

    @NotNull
    @Schema(description = "The version", example = "2025-01-23T12:09:01")
    private Instant version;

}
