package at.ac.tuwien.ifs.dbrepo.core.api.container;

import at.ac.tuwien.ifs.dbrepo.core.api.CacheableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ContainerDto extends CacheableDto {

    @NotNull
    @Schema(description = "The container id", example = "7ddb7e87-b965-43a2-9a24-4fa406d998f4")
    private UUID id;

    @NotNull
    @JsonIgnore
    @Schema(description = "The container hash", example = "f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
    private String hash;

    @NotBlank
    @Schema(description = "The user-friendly container name", example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly container name", example = "air-quality")
    private String internalName;

    @Schema(description = "The container hostname", example = "data-db")
    private String host;

    @Schema(description = "The container port", example = "3306")
    private Integer port;

    @NotNull
    private ImageDto image;

    @NotNull
    @Schema(description = "The number of databases the container is capable to hold simultaneously, if null the container has no limit", example = "50")
    private Integer quota;

    @NotNull
    @Schema(description = "The number of databases currently in the container", example = "10")
    private Integer count;

    @ToString.Exclude
    @Schema(description = "The username of the privileged user", example = "root")
    private String username;

    @ToString.Exclude
    @Schema(description = "The password of the privileged user", example = "dbrepo")
    private String password;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

}
