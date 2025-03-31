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
    @Schema(example = "816f55d5-1098-4f60-a4af-c8121c04dcca")
    private UUID id;

    @NotBlank
    @JsonProperty("display_name")
    @Schema(example = "TIME(fsp)")
    private String displayName;

    @NotBlank
    @Schema(example = "time")
    private String value;

    @JsonProperty("size_min")
    @Schema(example = "0")
    private Integer sizeMin;

    @JsonProperty("size_max")
    @Schema(example = "6")
    private Integer sizeMax;

    @JsonProperty("size_default")
    @Schema(example = "0")
    private Integer sizeDefault;

    @JsonProperty("size_required")
    @Schema(example = "false")
    private Boolean sizeRequired;

    @JsonProperty("d_min")
    private Integer dMin;

    @JsonProperty("d_max")
    private Integer dMax;

    @JsonProperty("d_default")
    private Integer dDefault;

    @JsonProperty("d_required")
    private Boolean dRequired;

    @NotNull
    @Schema(example = "https://mariadb.com/kb/en/time/")
    private String documentation;

    @JsonProperty("data_hint")
    @Schema(example = "e.g. HH:MM:SS, HH:MM, HHMMSS, H:M:S")
    private String dataHint;

    @JsonProperty("type_hint")
    @Schema(example = "fsp=microsecond precision, min. 0, max. 6")
    private String typeHint;

    @NotNull
    @JsonProperty("is_quoted")
    @Schema(example = "false", description = "frontend needs to quote this data type")
    private Boolean quoted;

    @NotNull
    @JsonProperty("is_buildable")
    @Schema(example = "true", description = "frontend can build this data type")
    private Boolean buildable;

}
