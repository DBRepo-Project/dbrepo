package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import at.tuwien.panels.StatsPanel;
import at.tuwien.service.DataService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static at.tuwien.panels.MultiTimeSeriesPanel.*;
import static at.tuwien.panels.PieChartPanel.VIEW_PIE_PERCENTAGE_COL;
import static at.tuwien.panels.TimeSeriesPanel.*;

@Log4j2
@Service
public class DataServiceImpl implements DataService {
    private final RestTemplate dataRestTemplate;
    private final TableService tableService;
    private final RestTemplate grafanaRestTemplate;

    @Autowired
    public DataServiceImpl(@Qualifier("dataServiceRestTemplate") RestTemplate dataRestTemplate,
                           @Qualifier("grafanaTemplate") RestTemplate grafanaRestTemplate,
                           TableService tableService) {
        this.dataRestTemplate = dataRestTemplate;
        this.grafanaRestTemplate = grafanaRestTemplate;
        this.tableService = tableService;
    }

    @Override
    public Map<String, Object> getPieChartData(Long dbId, Long viewId, Long size) {

        Long page = 0L;
        String path = String.format("/api/database/%d/view/%d/data?page=%d&size=%d", dbId, viewId, page, size);

        try {
            ResponseEntity<QueryResultDto> responseEntity = dataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    QueryResultDto.class
            );
            QueryResultDto queryResultDto = responseEntity.getBody();
            assert queryResultDto != null;

            List<Map<String, Object>> res = queryResultDto.getResult();
            Map<String, Object> pieDataMap = new HashMap<>();
            double sum = 0;

            for (Map<String, Object> map : res) {
                String key = null;
                Object value = null;

                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (!entry.getKey().equals(VIEW_PIE_PERCENTAGE_COL)) { // string column
                        key = (String) entry.getValue();
                    } else {
                        value = entry.getValue();
                        sum += (double) value;
                    }
                }

                if (key != null && value != null) {
                    pieDataMap.put(key, value);
                }
            }

            pieDataMap.put("Others", 100 - sum);
            return pieDataMap;
        } catch (ResourceAccessException e) {
            log.error("Resource access error for accessing URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Resource access error occurred");
        } catch (RestClientException e) {
            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Exception occurred");
        }
    }

    @Override
    public Map<String, Object> getCntAllData(Long dbId, Long viewId) {

        String path = String.format("/api/database/%d/view/%d/data", dbId, viewId);

        try {
            ResponseEntity<QueryResultDto> responseEntity = dataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    QueryResultDto.class
            );
            QueryResultDto queryResultDto = responseEntity.getBody();
            assert queryResultDto != null;
            return queryResultDto.getResult().get(0);
        } catch (ResourceAccessException e) {
            log.error("Resource access error for accessing URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Resource access error occurred");
        } catch (RestClientException e) {
            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Exception occurred");
        }
    }

    @Override
    public List<Map<String, Object>> getTableData(Long dbId, Long tableId, Long size) {
        return tableService.getTableData(dbId, tableId, size);
    }

    @Override
    public Map<String, List<Object>> getHistogramData(Long dbId, Long viewId, Long size) {

        Long page = 0L;
        String path = String.format("/api/database/%d/view/%d/data?page=%d&size=%d", dbId, viewId, page, size);

        try {
            ResponseEntity<QueryResultDto> responseEntity = dataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    QueryResultDto.class
            );
            QueryResultDto queryResultDto = responseEntity.getBody();
            assert queryResultDto != null;

            List<Map<String, Object>> resultMap = queryResultDto.getResult();
            List<Object> valueList = new ArrayList<>();

            for (Map<String, Object> map : resultMap) {
                if (!map.isEmpty()) {
                    valueList.add(map.entrySet().iterator().next().getValue());
                }
            }

            return Map.of("values", valueList);
        } catch (ResourceAccessException e) {
            log.error("Resource access error for accessing URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Resource access error occurred");
        } catch (RestClientException e) {
            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Exception occurred");
        }
    }

    @Override
    public List<Map<String, Object>> getStatsData(Long dbId, Long tableId) {

        String path = String.format("/api/database/%d/table/%d/statistic", dbId, tableId);

        try {
            ResponseEntity<TableStatisticDto> responseEntity = dataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    TableStatisticDto.class
            );
            TableStatisticDto columnStatisticDto = responseEntity.getBody();
            assert columnStatisticDto != null;
            Map<String, ColumnStatisticDto> map = columnStatisticDto.getColumns();

            map = map.entrySet().stream()
                    .filter(entry -> entry.getValue().getMin() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List<Map<String, Object>> res = new ArrayList<>();
            Map<String, Object> row;
            for (Map.Entry<String, ColumnStatisticDto> entry : map.entrySet()) {
                row = new HashMap<>();

                row.put(StatsPanel.HEADER_COL, entry.getKey());
                row.put(StatsPanel.HEADER_MIN, entry.getValue().getMin());
                row.put(StatsPanel.HEADER_MAX, entry.getValue().getMax());
                row.put(StatsPanel.HEADER_AVG, entry.getValue().getMedian());
                row.put(StatsPanel.HEADER_STDDEV, entry.getValue().getStdDev());

                res.add(row);
            }

            return res;
        } catch (ResourceAccessException e) {
            log.error("Resource access error for accessing URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Resource access error occurred");
        } catch (RestClientException e) {
            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Exception occurred");
        }
    }

    @Override
    public Map<String, List<Map<String, Object>>> getTimeSeriesData(Long dbId, Long viewId, Long size) {

        Long page = 0L;
        String path = String.format("/api/database/%d/view/%d/data?page=%d&size=%d", dbId, viewId, page, size);

        try {
            ResponseEntity<QueryResultDto> responseEntity = dataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    QueryResultDto.class
            );
            QueryResultDto queryResultDto = responseEntity.getBody();
            assert queryResultDto != null;

            List<Map<String, Object>> resultMap = queryResultDto.getResult();


            Map<String, List<Map<String, Object>>> res = new HashMap<>();
            res.put("time_series", formatTimeData(resultMap));
            
            return res;
        } catch (ResourceAccessException e) {
            log.error("Resource access error for accessing URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Resource access error occurred");
        } catch (RestClientException e) {
            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Exception occurred");
        }
    }

    public Map<String, List<Map<String, Object>>> getMultiTimeSeriesData(Long dbId, Long viewId, Long size) {
        Long page = 0L;
        String path = String.format("/api/database/%d/view/%d/data?page=%d&size=%d", dbId, viewId, page, size);

        try {
            ResponseEntity<QueryResultDto> responseEntity = dataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    QueryResultDto.class
            );
            QueryResultDto queryResultDto = responseEntity.getBody();
            assert queryResultDto != null;

            List<Map<String, Object>> resultMap = queryResultDto.getResult();
            List<Map<String, Object>> timeMap = new ArrayList<>();

            for (Map<String, Object> map : resultMap) {
                Object time = map.get(VIEW_MULTI_TIMECOL);

                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Map<String, Object> multiTimeEntry = new HashMap<>();
                    if (!entry.getKey().equals(VIEW_MULTI_TIMECOL)) {
                        multiTimeEntry.put(VIEW_MULTI_SELECTOR_NAME, entry.getKey());
                        multiTimeEntry.put(VIEW_MULTI_SELECTOR_VALUE, entry.getValue());
                        multiTimeEntry.put(VIEW_MULTI_SELECTOR_TIME, time);

                        timeMap.add(multiTimeEntry);
                    }
                }
            }
            Map<String, List<Map<String, Object>>> res = new HashMap<>();
            res.put("time_series", timeMap);

            return res;
        } catch (ResourceAccessException e) {
            log.error("Resource access error for accessing URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Resource access error occurred");
        } catch (RestClientException e) {
            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.POST, e);
            throw new ResourceAccessException("Exception occurred");
        }
    }

    private List<Map<String, Object>> formatTimeData(List<Map<String, Object>> resultMap) {
        int year, month, day, hour, min, sec;
        Object val;

        List<Map<String, Object>> timeValues = new ArrayList<>();
        Map<String, Object> timeValue;

        for (Map<String, Object> map : resultMap) {
            year = 0;
            month = 1;
            day = 1;
            hour = 0;
            min = 0;
            sec = 0;
            val = null;
            timeValue = new HashMap<>();

            if(map.containsKey(TIME_YEAR_COL)) {
                Object y = map.get(TIME_YEAR_COL);
                if (y instanceof Integer) {
                    year = (int) y;
                }
            }

            if(map.containsKey(TIME_MONTH_COL)) {
                Object m = map.get(TIME_MONTH_COL);
                if (m instanceof String) {
                    month = getMonth(m.toString());
                } else if (m instanceof Integer) {
                    month = (int) m;
                }
            }

            if(map.containsKey(TIME_DAY_COL)) {
                Object d = map.get(TIME_DAY_COL);
                if (d instanceof Integer) {
                    day = (int) d;
                }
            }

            if(map.containsKey(TIME_HOUR_COL)) {
                Object h = map.get(TIME_HOUR_COL);
                if (h instanceof Integer) {
                    hour = (int) h;
                }
            }

            if(map.containsKey(TIME_MIN_COL)) {
                Object m = map.get(TIME_MIN_COL);
                if (m instanceof Integer) {
                    min = (int) m;
                }
            }

            if(map.containsKey(TIME_SECOND_COL)) {
                Object s = map.get(TIME_SECOND_COL);
                if (s instanceof Integer) {
                    sec = (int) s;
                }
            }

            if(map.containsKey(TIME_VAL_COL)) {
                val = map.get(TIME_VAL_COL);
            }

            timeValue.put("time", String.format("%d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, min, sec));
            timeValue.put("value", val);
            timeValues.add(timeValue);
        }

        return timeValues;
    }

    private int getMonth(String month) {
        return switch (month.toLowerCase()) {
            case "february" -> 2;
            case "march" -> 3;
            case "april" -> 4;
            case "may" -> 5;
            case "june" -> 6;
            case "july" -> 7;
            case "august" -> 8;
            case "september" -> 9;
            case "october" -> 10;
            case "november" -> 11;
            case "december" -> 12;
            default -> 1;
        };
    }
}
