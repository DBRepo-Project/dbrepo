package at.tuwien.api.database.table.columns;

import at.tuwien.api.database.table.columns.concepts.ConceptBriefDto;
import at.tuwien.api.database.table.columns.concepts.UnitBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

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
    @Schema(example = "1")
    private Long id;

    @NotNull
    @Schema(example = "2")
    @JsonProperty("database_id")
    private Long databaseId;

    @NotNull
    @Schema(example = "3")
    @JsonProperty("table_id")
    private Long tableId;

    @NotNull
    @Schema(example = "0")
    @JsonProperty("ord")
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

    @Schema(example = "firstname")
    private String alias;

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

    @Schema(example = "34300")
    @JsonProperty("data_length")
    private Long dataLength;

    @Schema(example = "34300")
    @JsonProperty("max_data_length")
    private Long maxDataLength;

    @Schema(example = "32")
    @JsonProperty("num_rows")
    private Long numRows;

    @Schema(example = "0")
    @JsonProperty("val_min")
    private BigDecimal valMin;

    @Schema(example = "100")
    @JsonProperty("val_max")
    private BigDecimal valMax;

    @Schema(example = "45.4")
    private BigDecimal mean;

    @Schema(example = "51")
    private BigDecimal median;

    @Schema(example = "5.32")
    @JsonProperty("std_dev")
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

    @Parameter(description = "enum values, only considered when type = ENUM")
    private List<String> enums;

    @Parameter(description = "enum values, only considered when type = ENUM")
    private List<String> sets;

}
