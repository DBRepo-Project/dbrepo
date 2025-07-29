package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ColumnStatisticDto {

    @NotBlank
    @Schema(description = "The column name", example = "temperature")
    private String name;

    @NotNull
    @Schema(description = "The mean value", example = "23.1")
    private BigDecimal mean;

    @NotNull
    @Schema(description = "The median value", example = "22.0")
    private BigDecimal median;

    @NotNull
    @JsonProperty("std_dev")
    @Schema(description = "The standard deviation", example = "2.3")
    private BigDecimal stdDev;

    @NotNull
    @JsonProperty("val_min")
    @Schema(description = "The minimum value", example = "1.7")
    private BigDecimal min;

    @NotNull
    @JsonProperty("val_max")
    @Schema(description = "The maximum value", example = "73.1")
    private BigDecimal max;
}
