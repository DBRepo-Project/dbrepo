package at.tuwien.service;

import java.util.List;
import java.util.Map;

public interface DataService {
    Map<String, Object> getPieChartData(Long dbId, Long viewId, Long size);
    Map<String, Object> getCntAllData(Long dbId, Long viewId);
    List<Map<String, Object>> getTableData(Long dbId, Long tableId, Long size);
    Map<String, List<Object>> getHistogramData(Long dbId, Long viewId, Long size);
    List<Map<String, Object>> getStatsData(Long dbId, Long tableId);
    Map<String, List<Map<String, Object>>> getTimeSeriesData(Long dbId, Long viewId, Long size);
    Map<String, List<Map<String, Object>>> getMultiTimeSeriesData(Long dbId, Long viewId, Long size);
}
