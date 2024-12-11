package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseModifyVisibilityDto {

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

}
