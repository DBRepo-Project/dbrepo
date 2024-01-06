package at.tuwien.api.database.query;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class QueryDto {

    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "container id is required")
    private Long cid;

    @NotNull(message = "database id is required")
    private Long dbid;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @NotNull(message = "created by is required")
    private UUID createdBy;

    @NotNull(message = "creator is required")
    private UserDto creator;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @NotBlank(message = "statement is required")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String queryNormalized;

    @Schema(example = "query")
    private QueryTypeDto type;

    private List<IdentifierDto> identifiers;

    @NotBlank(message = "query hash is required")
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

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
