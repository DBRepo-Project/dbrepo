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
public class QueryBriefDto {

    @NotNull
    @Schema(example = "83ea2326-f8f6-4263-baf8-cdf88a54efc7")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1a6fb0c0-49c3-4a22-a515-35f7a3dd8e62")
    private UUID databaseId;

    @NotNull
    private UserBriefDto owner;

    @NotNull
    @Schema(example = "2022-01-01 08:00:00.000")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant execution;

    @NotBlank
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String queryNormalized;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @JsonProperty("result_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @JsonProperty("result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @NotNull
    @JsonProperty("is_persisted")
    @Schema(example = "true")
    private Boolean isPersisted;

    @Schema(example = "query")
    private QueryTypeDto type;

    private List<IdentifierBriefDto> identifiers;

}
