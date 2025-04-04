package at.tuwien.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieChartConfigDto {
    private String limit;
    private String decimalPlace;
    private Long size;
}
