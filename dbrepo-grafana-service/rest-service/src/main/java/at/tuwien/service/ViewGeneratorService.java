package at.tuwien.service;

import at.tuwien.dto.PieChartConfigDto;

import java.util.List;
import java.util.Map;

public interface ViewGeneratorService {
    Long genCntAllView(Long dbId, String tableName, String token);
    Long genPieChartView(Long dbId, String tableName, String colName, PieChartConfigDto config, String token);
    Long genHistogramView(Long dbId, String tableName, String colName, String token);
    Long genStatisticsView(Long dbId, String tableName, String colName, String token);
    Long genTimeSeriesView(Long dbId, String tableName, Map<String, String> timeMap, String token);
    Long genMultiTimeSeriesView(Long dbId, String tableName, String timeCol, List<String> numValues, String token);
}
