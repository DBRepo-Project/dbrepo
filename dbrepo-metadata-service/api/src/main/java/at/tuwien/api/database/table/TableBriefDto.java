package at.tuwien.api.database.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableBriefDto {

    @NotNull
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    private Long databaseId;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @NotNull
    @JsonProperty("is_versioned")
    @Schema(example = "true")
    private Boolean isVersioned;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    @JsonProperty("owned_by")
    private UUID ownedBy;
}
