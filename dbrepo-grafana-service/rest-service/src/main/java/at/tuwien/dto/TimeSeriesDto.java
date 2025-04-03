package at.tuwien.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDto {
    private TimeDto timeDto;
    private Long valueColId;
    private Long size;
}
