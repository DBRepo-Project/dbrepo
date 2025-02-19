package at.tuwien.api.database.query;

import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserBriefDto;
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
    @Schema(example = "83ea2326-f8f6-4263-baf8-cdf88a54efc7")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1a6fb0c0-49c3-4a22-a515-35f7a3dd8e62")
    private UUID databaseId;

    @NotNull
    private UserBriefDto owner;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @NotBlank
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String queryNormalized;

    @Schema(example = "query")
    private QueryTypeDto type;

    @NotNull
    private List<IdentifierBriefDto> identifiers;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @NotNull
    @JsonProperty("is_persisted")
    @Schema(example = "true")
    private Boolean isPersisted;

    @JsonProperty("result_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @JsonProperty("result_number")
    @Schema(example = "1")
    private Long resultNumber;

}
