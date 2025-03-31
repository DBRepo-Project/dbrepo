package at.ac.tuwien.ifs.dbrepo.core.api.grafana;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateDashboardDto {

    @NotNull
    @JsonProperty("database_name")
    @Schema(example = "Some Database")
    private String databaseName;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @NotBlank
    @JsonProperty("owner_username")
    @Schema(example = "foobar")
    private String ownerUsername;
}
