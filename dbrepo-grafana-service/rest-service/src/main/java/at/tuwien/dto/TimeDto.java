package at.tuwien.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeDto {
    private Long yearColId;
    private Long monthColId;
    private Long dayColId;
    private Long hourColId;
    private Long minuteColId;
    private Long secondColId;
}
