package at.tuwien.api.container;

import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ContainerDto {

    @NotNull
    private Long id;

    @NotNull
    @Schema(example = "f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
    private String hash;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air-quality")
    private String internalName;

    @Schema(example = "running")
    private ContainerStateDto state;

    @Schema
    @ToString.Exclude
    private DatabaseDto database;

    @JsonProperty("ip_address")
    private String ipAddress;

    @NotNull
    @Schema(example = "true")
    private Boolean running;

    private ImageBriefDto image;

    private Integer port;

    private UserBriefDto owner;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21.678396092Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
