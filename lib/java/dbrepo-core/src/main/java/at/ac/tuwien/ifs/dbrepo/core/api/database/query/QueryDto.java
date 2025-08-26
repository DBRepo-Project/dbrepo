package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
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
public class QueryDto {

    @NotNull
    @Schema(description = "The query id", example = "83ea2326-f8f6-4263-baf8-cdf88a54efc7")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The database id", example = "1a6fb0c0-49c3-4a22-a515-35f7a3dd8e62")
    private UUID databaseId;

    @NotNull
    private UserBriefDto owner;

    @NotNull
    @Schema(description = "The timestamp when the query was executed", example = "2022-01-01 08:00:00.000000")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant execution;

    @NotNull
    @JsonProperty("created")
    @Schema(description = "The timestamp when the query was created", example = "2022-01-01 08:00:00.000000")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant created;

    @NotBlank
    @Schema(description = "The mapped SQL query", example = "SELECT `id` FROM `air_quality`")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(description = "The normalized SQL query as executed", example = "SELECT `id` FROM `air_quality` FOR SYSTEM_TIME AS OF TIMESTAMP '2022-01-01 08:00:00.000000'")
    private String queryNormalized;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(description = "The sha256-hash of the mapped query", example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @JsonProperty("result_hash")
    @Schema(description = "The sha256-hash of the result", example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @JsonProperty("result_number")
    @Schema(description = "The row count of the result", example = "1")
    private Long resultNumber;

    @NotNull
    @JsonProperty("is_persisted")
    @Schema(description = "If false, the query is marked for deletion at a later point in time", example = "true")
    private Boolean isPersisted;

    @Schema(description = "The query type", example = "query")
    private QueryTypeDto type;

    @NotNull
    private List<IdentifierBriefDto> identifiers;

    @JsonProperty("creation_location")
    @Schema(description = "The site where the query was initially created (optional)", example = "http://localhost:8080")
    private String creationLocation;

}
