package at.tuwien.api.database.query;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
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
public class QueryDto {

    @NotNull(message = "id is required")
    @Parameter(name = "query id", example = "1")
    private Long id;

    @NotNull(message = "container id is required")
    @Parameter(name = "container id", example = "1")
    private Long cid;

    @NotNull(message = "database id is required")
    @Parameter(name = "database id", example = "1")
    private Long dbid;

    @JsonIgnore
    @NotNull(message = "created by is required")
    @Parameter(name = "creator id", example = "1")
    private Long createdBy;

    @Parameter(name = "execution time", example = "2022-01-01 08:00:00.000")
    private Instant execution;

    @NotBlank(message = "statement is required")
    @Parameter(name = "query raw", example = "select * from table")
    private String query;

    @JsonProperty("query_normalized")
    @Parameter(name = "query normalized", example = "select id, name from table")
    private String queryNormalized;

    @NotBlank(message = "query hash is required")
    @JsonProperty("query_hash")
    @Parameter(name = "query hash sha256", example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @JsonProperty("result_hash")
    @Parameter(name = "result hash sha256", example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @JsonProperty("result_number")
    @Parameter(name = "result number of records", example = "1")
    private Long resultNumber;

    @NotNull(message = "created timestamp is required")
    private Instant created;

    @JsonProperty("last_modified")
    private Instant lastModified;

}
