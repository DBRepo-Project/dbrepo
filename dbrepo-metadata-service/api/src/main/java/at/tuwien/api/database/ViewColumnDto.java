package at.tuwien.api.database;

import at.tuwien.api.database.table.columns.ColumnTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ViewColumnDto {

    @NotNull
    @Schema(example = "12")
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1")
    private Long databaseId;

    @NotNull
    @JsonProperty("ord")
    @Schema(example = "0")
    private Integer ordinalPosition;

    @NotBlank
    @Size(max = 64)
    @Schema(example = "Given Name")
    private String name;

    @NotBlank
    @Size(max = 64)
    @JsonProperty("internal_name")
    @Schema(example = "given_name")
    private String internalName;

    @JsonProperty("index_length")
    @Schema(example = "255")
    private Long indexLength;

    @JsonProperty("length")
    @Schema(example = "255")
    private Long length;

    @NotNull
    @JsonProperty("type")
    @Schema(example = "varchar")
    private ColumnTypeDto columnType;

    @Schema(example = "255")
    private Long size;

    @Schema(example = "0")
    private Long d;

    @Size(max = 2048)
    @Schema(example = "Column comment")
    private String description;

    @NotNull
    @JsonProperty("is_null_allowed")
    @Schema(example = "false")
    private Boolean isNullAllowed;

}
