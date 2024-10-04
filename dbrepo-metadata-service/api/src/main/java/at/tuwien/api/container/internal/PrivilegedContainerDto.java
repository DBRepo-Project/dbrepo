package at.tuwien.api.container.internal;

import at.tuwien.api.container.image.ImageDto;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class PrivilegedContainerDto {

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

    private Integer port;

    @NotBlank
    @JsonProperty("sidecar_host")
    private String sidecarHost;

    @NotNull
    @JsonProperty("sidecar_port")
    private Integer sidecarPort;

    @JsonProperty("ui_host")
    private String uiHost;

    @JsonProperty("ui_port")
    private Integer uiPort;

    @NotNull
    private ImageDto image;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @ToString.Exclude
    private String username;

    @ToString.Exclude
    private String password;

    private Long defaultTimestampFormatId;

    private Long defaultDateFormatId;

}
