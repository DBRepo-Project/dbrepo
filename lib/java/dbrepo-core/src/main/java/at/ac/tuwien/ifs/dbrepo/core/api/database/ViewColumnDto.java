package at.ac.tuwien.ifs.dbrepo.core.api.database;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.EnumDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.SetDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ViewColumnDto {

    @NotNull
    @Schema(description = "The id", example = "6aec3a91-2e0b-4e92-a16a-9c3c5e892da1")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The database id", example = "2b5b2b03-fdd0-40d6-afe0-e5d02fd839e4")
    private UUID databaseId;

    @NotNull
    @JsonProperty("ord")
    @Schema(description = "The ordinal position of the colum to order it", example = "0")
    private Integer ordinalPosition;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "The user-friendly column name", example = "Given Name")
    private String name;

    @NotBlank
    @Size(max = 64)
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly column name", example = "given_name")
    private String internalName;

    @JsonProperty("index_length")
    @Schema(description = "The length of the index", example = "255")
    private Long indexLength;

    @JsonProperty("length")
    @Schema(description = "The length of the total data in the table (index + data)", example = "255")
    private Long length;

    @NotNull
    @JsonProperty("type")
    @Schema(description = "The column type name", example = "varchar")
    private ColumnTypeDto columnType;

    @Schema(description = "The column size, determines the number of digits before the comma as x=size-d where size >= d", example = "255")
    private Long size;

    @Schema(description = "The column d", example = "0")
    private Long d;

    @Size(max = 2048)
    @Schema(example = "Column comment")
    private String description;

    @NotNull
    @JsonProperty("is_null_allowed")
    @Schema(example = "false")
    private Boolean isNullAllowed;

    @Schema(description = "enum values, only considered when type = ENUM")
    private List<EnumDto> enums;

    @Schema(description = "enum values, only considered when type = ENUM")
    private List<SetDto> sets;

}
