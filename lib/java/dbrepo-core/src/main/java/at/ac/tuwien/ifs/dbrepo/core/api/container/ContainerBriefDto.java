package at.ac.tuwien.ifs.dbrepo.core.api.container;

import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageBriefDto;
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
public class ContainerBriefDto {

    @NotNull
    @Schema(description = "The container id", example = "7ddb7e87-b965-43a2-9a24-4fa406d998f4")
    private UUID id;

    @NotNull
    @Schema(description = "The container hash", example = "f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
    private String hash;

    @NotBlank
    @Schema(description = "The user-friendly container name", example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly container name", example = "air-quality")
    private String internalName;

    @NotNull
    private ImageBriefDto image;

    @NotNull
    @Schema(description = "The number of databases the container is capable to hold simultaneously, if null the container has no limit", example = "50")
    private Integer quota;

    @NotNull
    @Schema(description = "The number of databases currently in the container", example = "10")
    private Integer count;
}
