package at.tuwien.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ContainerCreateRequestDto {

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("image_id")
    @Schema(description = "Image ID")
    private Long imageId;

    @NotBlank
    @Schema(description = "Hostname of container")
    private String host;

    @Schema(description = "Port of container")
    private Integer port;

    @NotBlank
    @JsonProperty("sidecar_host")
    @Field(name = "sidecar_host", type = FieldType.Keyword)
    private String sidecarHost;

    @NotNull
    @JsonProperty("sidecar_port")
    @Field(name = "sidecar_port", type = FieldType.Integer)
    private Integer sidecarPort;

    @JsonProperty("ui_host")
    @Field(name = "ui_host", type = FieldType.Keyword)
    private String uiHost;

    @JsonProperty("ui_port")
    @Field(name = "ui_port", type = FieldType.Integer)
    private Integer uiPort;

    @NotBlank
    @JsonProperty("privileged_username")
    @Schema(description = "Username of privileged user", example = "root")
    private String privilegedUsername;

    @NotBlank
    @JsonProperty("privileged_password")
    @Schema(description = "Password of privileged user")
    private String privilegedPassword;
}
