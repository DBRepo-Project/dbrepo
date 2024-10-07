package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataTypeDto {

    @NotBlank
    @JsonProperty("display_name")
    @Schema(example = "BIGINT")
    private String displayName;

    @NotBlank
    @Schema(example = "bigint")
    private String value;

    @JsonProperty("size_min")
    private Integer sizeMin;

    @JsonProperty("size_max")
    private Integer sizeMax;

    @JsonProperty("size_default")
    private Integer sizeDefault;

    @JsonProperty("size_required")
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
    @Schema(example = "https://mariadb.com/kb/en/bigint/")
    private String documentation;

    @NotNull
    @Schema(description = "frontend needs to quote this data type")
    @JsonProperty("is_quoted")
    private Boolean quoted;

    @NotNull
    @JsonProperty("is_buildable")
    @Schema(description = "frontend can build this data type")
    private Boolean buildable;

}
