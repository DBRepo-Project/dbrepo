package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @NotNull
    private BigDecimal mean;

    @NotNull
    private BigDecimal median;

    @NotNull
    @JsonProperty("std_dev")
    private BigDecimal stdDev;

    @NotNull
    @JsonProperty("val_min")
    private BigDecimal min;

    @NotNull
    @JsonProperty("val_max")
    private BigDecimal max;
}
