package at.tuwien.api.database.query;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class QueryBriefDto {

    @NotNull(message = "id is required")
    private Long id;

    @NotNull(message = "container id is required")
    private Long cid;

    @NotNull(message = "database id is required")
    private Long dbid;

    @JsonIgnore
    @NotNull(message = "created by is required")
    private Long createdBy;

    @NotNull(message = "creator is required")
    private UserDto creator;

    @Schema(example = "2022-01-01 08:00:00.000")
    private Instant execution;

    @NotBlank(message = "statement is required")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String queryNormalized;

    @NotBlank(message = "query hash is required")
    @JsonProperty("query_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @JsonProperty("result_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @JsonProperty("result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @Schema(example = "query")
    private QueryTypeDto type;

    @NotNull(message = "created timestamp is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
