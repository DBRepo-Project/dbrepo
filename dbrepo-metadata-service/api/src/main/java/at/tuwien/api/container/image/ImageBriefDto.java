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
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ImageBriefDto {

    @NotNull
    @Schema(example = "5")
    private Long id;

    @NotBlank
    @Schema(example = "mariadb")
    private String name;

    @NotBlank
    @Schema(example = "10.5")
    private String version;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @NotNull
    @JsonProperty("default")
    @Schema(example = "false")
    private Boolean isDefault;

}
