package at.tuwien.api.database.query;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.persistence.Transient;
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
    @Schema(name = "query id", example = "1")
    private Long id;

    @NotNull(message = "container id is required")
    @Schema(name = "container id", example = "1")
    private Long containerId;

    @NotNull(message = "database id is required")
    @Schema(name = "database id", example = "1")
    private Long databaseId;

    @Transient
    @Schema(name = "identifier", example = "1")
    private IdentifierDto identifier;

    @JsonIgnore
    @NotNull(message = "created by is required")
    @Schema(name = "creator id", example = "1")
    private Long createdBy;

    @NotNull(message = "creator is required")
    @Schema(name = "creator")
    private UserDto creator;

    @Schema(name = "execution time", example = "2022-01-01 08:00:00.000")
    private Instant execution;

    @NotBlank(message = "statement is required")
    @Schema(name = "query raw", example = "select * from table")
    private String query;

    @JsonProperty("query_normalized")
    @Schema(name = "query normalized", example = "select id, name from table")
    private String queryNormalized;

    @NotBlank(message = "query hash is required")
    @JsonProperty("query_hash")
    @Schema(name = "query hash sha256", example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @JsonProperty("result_hash")
    @Schema(name = "result hash sha256", example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @JsonProperty("result_number")
    @Schema(name = "result number of records", example = "1")
    private Long resultNumber;

    @NotNull(message = "created timestamp is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
