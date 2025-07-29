package at.ac.tuwien.ifs.dbrepo.core.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataTypeDto {

    @NotNull
    @Schema(description = "The id of the data type", example = "816f55d5-1098-4f60-a4af-c8121c04dcca")
    private UUID id;

    @NotBlank
    @JsonProperty("display_name")
    @Schema(description = "The human-friendly name of the data type", example = "TIME(fsp)")
    private String displayName;

    @NotBlank
    @Schema(description = "The machine-friendly value of the data type", example = "time")
    private String value;

    @JsonProperty("size_min")
    @Schema(description = "The minimum size", example = "0")
    private Integer sizeMin;

    @JsonProperty("size_max")
    @Schema(description = "The maximum size", example = "6")
    private Integer sizeMax;

    @JsonProperty("size_default")
    @Schema(description = "The default size", example = "0")
    private Integer sizeDefault;

    @JsonProperty("size_required")
    @Schema(description = "If true, the size parameter cannot be empty", example = "true")
    private Boolean sizeRequired;

    @JsonProperty("size_step")
    @Schema(description = "The step increment", example = "1")
    private Integer sizeStep;

    @JsonProperty("d_min")
    @Schema(description = "The minimum d")
    private Integer dMin;

    @JsonProperty("d_max")
    @Schema(description = "The maximum d")
    private Integer dMax;

    @JsonProperty("d_default")
    @Schema(description = "The default d")
    private Integer dDefault;

    @JsonProperty("d_required")
    @Schema(description = "If true, the d parameter cannot be empty")
    private Boolean dRequired;

    @JsonProperty("d_step")
    @Schema(description = "The d increment", example = "1")
    private Integer dStep;

    @NotNull
    @Schema(description = "The documentation link", example = "https://mariadb.com/kb/en/time/")
    private String documentation;

    @JsonProperty("data_hint")
    @Schema(description = "The user-friendly description of the data format", example = "e.g. HH:MM:SS, HH:MM, HHMMSS, H:M:S")
    private String dataHint;

    @JsonProperty("type_hint")
    @Schema(description = "The user-friendly description of the data type", example = "fsp=microsecond precision, min. 0, max. 6")
    private String typeHint;

    @NotNull
    @JsonProperty("is_quoted")
    @Schema(description = "frontend needs to quote this data type", example = "false")
    private Boolean quoted;

    @NotNull
    @JsonProperty("is_buildable")
    @Schema(description = "frontend can build this data type", example = "true")
    private Boolean buildable;

}
