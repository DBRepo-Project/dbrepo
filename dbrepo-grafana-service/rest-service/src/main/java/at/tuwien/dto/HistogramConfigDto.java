package at.tuwien.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistogramConfigDto {
    private Integer min;
    private Integer max;
    private Long size;
}
