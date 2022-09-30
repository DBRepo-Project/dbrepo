package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewCreateDto {

    @Schema(example = "Air Quality")
    private String name;

    @NotNull(message = "query is required")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @NotNull(message = "public attribute is required")
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

}
