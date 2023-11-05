package at.tuwien.api.container;

import at.tuwien.api.container.image.ImageBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
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
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Field(name = "internal_name")
    @Schema(example = "data-db")
    private String internalName;

    @NotBlank
    private String host;

    @NotNull
    private Integer port;

    @NotBlank
    @JsonProperty("ui_host")
    private String uiHost;

    @NotNull
    @JsonProperty("ui_port")
    private Integer uiPort;

    @JsonProperty("ui_additional_flags")
    private String uiAdditionalFlags;

    private ImageBriefDto image;

    @NotNull
    @Field(type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
