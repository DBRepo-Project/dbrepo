package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class JoinDto {

    @NotNull
    @Schema(description = "The type of join", example = "inner", externalDocs = @ExternalDocumentation(url = "https://dbrepo-project.github.io/dbrepo/1.12/user-guide/create-subset/"))
    private JoinTypeDto type;

    @NotNull
    @JsonProperty("datasource_id")
    @Schema(description = "The id of the data source", example = "f7df2a7d-4ade-4c78-97b0-7c744d0893c7")
    private UUID datasourceId;

    @NotNull
    private Set<ConditionalDto> conditionals;

}
