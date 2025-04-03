package at.ac.tuwien.ifs.dbrepo.core.api.container.image;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ImageDto {

    @NotNull
    @Schema(example = "816f55d5-1098-4f60-a4af-c8121c04dcce")
    private UUID id;

    @NotBlank
    @Schema(example = "mariadb")
    private String name;

    @NotBlank
    @Schema(example = "10.5")
    private String version;

    @NotBlank
    @JsonIgnore
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @NotNull
    @JsonProperty("default")
    @Schema(example = "false")
    private Boolean isDefault;

    @NotNull
    @JsonProperty("data_types")
    private List<DataTypeDto> dataTypes;

    @NotNull
    private List<OperatorDto> operators;

}
