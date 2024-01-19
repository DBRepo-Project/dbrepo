package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ViewCreateDto {

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank(message = "query is required")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @NotNull(message = "public attribute is required")
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

}
