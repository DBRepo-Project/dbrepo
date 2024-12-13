package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(min = 1, max = 63)
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

}
