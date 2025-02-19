package at.tuwien.api.container;

import at.tuwien.api.CacheableDto;
import at.tuwien.api.container.image.ImageDto;
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
    @Schema(example = "7ddb7e87-b965-43a2-9a24-4fa406d998f4")
    private UUID id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "data-db")
    private String host;

    @Schema(example = "3306")
    private Integer port;

    @JsonProperty("ui_host")
    @Schema(example = "example.com")
    private String uiHost;

    @JsonProperty("ui_port")
    @Schema(example = "3306")
    private Integer uiPort;

    @NotNull
    private ImageDto image;

    @NotNull
    @Schema(example = "50")
    private Long quota;

    @NotNull
    @Schema(example = "10")
    private Long count;

    @ToString.Exclude
    @Schema(example = "username")
    private String username;

    @ToString.Exclude
    @Schema(example = "p4ssw0rd")
    private String password;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

}
