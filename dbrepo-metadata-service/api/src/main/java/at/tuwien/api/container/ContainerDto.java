package at.tuwien.api.container;

import at.tuwien.api.CacheableDto;
import at.tuwien.api.container.image.ImageDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ContainerDto extends CacheableDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "data-db")
    private String internalName;

    @NotBlank
    private String host;

    @NotNull
    private Integer port;

    @JsonProperty("ui_host")
    private String uiHost;

    @JsonProperty("ui_port")
    private Integer uiPort;

    @NotNull
    private ImageDto image;

    @NotNull
    @Schema(example = "50")
    private Long quota;

    @NotNull
    @Schema(example = "10")
    private Long count;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    private Instant lastRetrieved;

    @ToString.Exclude
    @JsonIgnore
    private String jdbcMethod;

    @ToString.Exclude
    @JsonIgnore
    private String username;

    @ToString.Exclude
    @JsonIgnore
    private String password;

    @ToString.Exclude
    @JsonIgnore
    private String database;

}
