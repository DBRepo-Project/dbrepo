package at.tuwien.api.container;

import at.tuwien.api.container.image.ImageBriefDto;
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
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ContainerBriefDto {

    @NotNull
    @Schema(example = "4")
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

    @NotNull
    private ImageBriefDto image;

    @NotNull
    @Schema(example = "50")
    private Integer quota;

    @NotNull
    @Schema(example = "10")
    private Integer count;
}
