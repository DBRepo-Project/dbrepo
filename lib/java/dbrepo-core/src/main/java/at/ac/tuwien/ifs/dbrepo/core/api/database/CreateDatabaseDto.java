package at.ac.tuwien.ifs.dbrepo.core.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateDatabaseDto {

    @NotNull
    @JsonProperty("container_id")
    @Schema(example = "0888e108-d521-46e2-9d3e-82099185305b")
    private UUID cid;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @JsonProperty("replica_urls")
    @Schema(example = "[\"https://replica1.example.com\", \"https://replica2.example.com\"]", nullable = true)
    private List<String> replicaUrls;

    private String creationLocation;

}
