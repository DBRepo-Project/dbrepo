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

    @NotNull
    private Integer port;

    @JsonProperty("ui_host")
    private String uiHost;

    @JsonProperty("ui_port")
    private Integer uiPort;

    @NotNull
    private ImageDto image;

    @ToString.Exclude
    private String username;

    @ToString.Exclude
    private String password;

}
