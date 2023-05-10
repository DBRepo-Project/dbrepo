package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseCreateDto {

    @NotBlank(message = "database name is required")
    @Schema(example = "Air Quality")
    private String name;

    @NotNull(message = "public attribute is required")
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

}
