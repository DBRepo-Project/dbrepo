package at.tuwien.api.container;

import at.tuwien.api.container.image.ImageBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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

    @NotBlank
    @Field(name = "name", type = FieldType.Keyword)
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Field(name = "internal_name", type = FieldType.Keyword)
    @Schema(example = "data-db")
    private String internalName;

    @NotBlank
    @Field(name = "host", type = FieldType.Keyword)
    private String host;

    @Field(name = "port", type = FieldType.Integer)
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

    @Field(name = "image", type = FieldType.Nested)
    private ImageBriefDto image;

    @NotNull
    @Field(type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
