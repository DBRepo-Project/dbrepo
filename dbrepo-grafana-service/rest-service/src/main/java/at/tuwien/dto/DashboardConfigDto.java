package at.tuwien.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardConfigDto {
    /**
     * Map containing information to create time series data.
     * <p>
     * The key of the map represents the table id.
     * The value is a list of {@link TimeSeriesDto} objects in this table.
     */
    private Map<Long, List<TimeSeriesDto>> timeSeriesDto;

    /**
     * Map containing information about params for the PieChartPanel.
     * <p>
     * The key of the map represents the table id.
     * The value is a Map with key = column id and value = {@link PieChartConfigDto}.
     */
    private Map<Long, Map<Long, PieChartConfigDto>> pieChartConfigDto;

    /**
     * Map containing information about params for the Histogram.
     * <p>
     * The key of the map represents the table id.
     * The value is a Map with key = column id and value = {@link HistogramConfigDto}.
     */
    private Map<Long, Map<Long, HistogramConfigDto>> histogramConfigDto;

    /**
     * Map containing information about params for the tables.
     * <p>
     * The key of the map represents the table id.
     * The value is a dto {@link TableConfigDto} holding the properties.
     */
    private Map<Long, TableConfigDto> tableConfigDto;
    private Integer refreshRate;
}
