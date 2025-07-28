package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ConceptBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.UnitBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
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
public class ColumnDto {

    @NotNull
    @Schema(description = "The id", example = "a453e444-e00d-41ca-902c-11e9c54b39f1")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The database id", example = "911f9052-c58c-4e1c-b3f2-66af2107be16")
    private UUID databaseId;

    @NotNull
    @JsonProperty("table_id")
    @Schema(description = "The table id", example = "bfffa915-a547-4466-9c65-ddc0d38fdb08")
    private UUID tableId;

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

    @Schema(description = "The data source alias name", example = "firstname")
    private String alias;

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

    @Schema(description = "The data length", example = "34300")
    @JsonProperty("data_length")
    private Long dataLength;

    @Schema(description = "The maximum data length", example = "34300")
    @JsonProperty("max_data_length")
    private Long maxDataLength;

    @Schema(description = "The number of rows", example = "32")
    @JsonProperty("num_rows")
    private Long numRows;

    @Schema(description = "The statistically highest numerical value", example = "0")
    @JsonProperty("val_min")
    private BigDecimal valMin;

    @Schema(description = "The statistically lowest numerical value",example = "100")
    @JsonProperty("val_max")
    private BigDecimal valMax;

    @Schema(description = "The statistically average numerical value",example = "45.4")
    private BigDecimal mean;

    @Schema(description = "The statistically most middle numerical value", example = "51")
    private BigDecimal median;

    @JsonProperty("std_dev")
    @Schema(description = "The statistically determined standard deviation", example = "5.32")
    private BigDecimal stdDev;

    private ConceptBriefDto concept;

    private UnitBriefDto unit;

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
