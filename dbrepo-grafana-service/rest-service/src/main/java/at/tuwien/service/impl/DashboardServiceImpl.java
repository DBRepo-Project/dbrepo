package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.dto.*;
import at.tuwien.exception.JsonProcessingException;
import at.tuwien.panels.*;
import at.tuwien.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.*;
import java.util.stream.Collectors;

import static at.tuwien.panels.TimeSeriesPanel.*;

@Log4j2
@Service
public class DashboardServiceImpl implements DashboardService {

    @Value("${dbrepo.endpoints.grafanaService}")
    private String grafanaServiceEndpoint;

    @Value("${application.baseurl}")
    private String baseUrl;

    @Value("${dbrepo.endpoints.grafanaPort}")
    private String grafanaPort;

    @Value("${dbrepo.grafana.default_refreshrate}")
    private int defaultRefreshrate;

    private final RestTemplate grafanaRestTemplate;
    private final TableService tableService;
    private final DataSourceService dataSourceService;
    private final ViewGeneratorService viewGeneratorService;
    private String token = "";

    @Autowired
    public DashboardServiceImpl(@Qualifier("grafanaTemplate") RestTemplate grafanaRestTemplate,
                                DataSourceService dataSourceService, TableService tableService,
                                ViewGeneratorService viewGeneratorService) {
        this.grafanaRestTemplate = grafanaRestTemplate;
        this.dataSourceService = dataSourceService;
        this.tableService = tableService;
        this.viewGeneratorService = viewGeneratorService;
    }

    @Override
    public Boolean checkIfDashboardExists(Long dbId) {
        return this.checkForSync(dbId) != null;
    }

    @Override
    public void removeDashboard(Long dbId) {
        String path = String.format("/api/dashboards/uid/%d", dbId);

        try {
            ResponseEntity<String> responseEntity = grafanaRestTemplate.exchange(
                    path,
                    HttpMethod.DELETE,
                    null,
                    String.class
            );

            log.warn(responseEntity.getBody());
        } catch (ResourceAccessException e) {
            log.error("Resource access error for accessing URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.DELETE, e);
            throw new ResourceAccessException("Resource access error occurred");
        } catch (RestClientException e) {
            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.DELETE, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.DELETE, e);
            throw new ResourceAccessException("Exception occurred");
        }
    }

    @Override
    public String generateDashboard(Long dbId, String token, DashboardConfigDto configDto) {
        this.token = token;
        addDatasourceIfNotPresent();

        String url = this.checkForSync(dbId);
        if (url != null) {
            return String.format("%s:%s%s%n", baseUrl, grafanaPort, url);
        }

        Map<Long, List<TimeSeriesDto>> timeSeriesParamMap = new HashMap<>();
        Map<Long, Map<Long, PieChartConfigDto>> pieChartParamMap = new HashMap<>();
        Map<Long, Map<Long, HistogramConfigDto>> histogramParamMap = new HashMap<>();
        Map<Long, TableConfigDto> tableParamMap = new HashMap<>();
        DashboardConfigDto dashboardConfigDto = new DashboardConfigDto();

        if (configDto != null) {
            dashboardConfigDto = configDto;
        }

        if (dashboardConfigDto.getTimeSeriesDto() != null) {
            timeSeriesParamMap = dashboardConfigDto.getTimeSeriesDto();
        }

        if (dashboardConfigDto.getPieChartConfigDto() != null) {
            pieChartParamMap = dashboardConfigDto.getPieChartConfigDto();
        }

        if (dashboardConfigDto.getHistogramConfigDto() != null) {
            histogramParamMap = dashboardConfigDto.getHistogramConfigDto();
        }

        if (dashboardConfigDto.getTableConfigDto() != null) {
            tableParamMap = dashboardConfigDto.getTableConfigDto();
        }

        List<TableBriefDto> tableIds = this.tableService.getAllTables(dbId);
        List<Long> idList = tableIds.stream()
                .map(TableBriefDto::getId)
                .toList();

        AbstractPanel.setDataEndpoint(grafanaServiceEndpoint);
        AbstractPanel.resetCoordinates();
        List<String> rowPanels = new ArrayList<>();
        List<String> tablePanels;

        for (var tId : idList) {
            tablePanels = new ArrayList<>();
            TableDto tableSchema = this.tableService.getTableSchemas(dbId, tId);
            String tableName = tableSchema.getInternalName();

            List<ColumnDto> columns = tableSchema.getColumns();
            Set<Long> primaryKeys = tableSchema.getConstraints().getPrimaryKey().stream()
                    .map(pkDto -> pkDto.getColumn().getId())
                    .collect(Collectors.toSet());

            columns.removeIf(column -> primaryKeys.contains(column.getId()));

            AbstractPanel.addRowPlaceHolder();

            if (tableParamMap.containsKey(tId)) {
                tablePanels.add(generateTablePanel(dbId, tId, tableName, tableParamMap.get(tId).getSize()));
            } else {
                tablePanels.add(generateTablePanel(dbId, tId, tableName, null));
            }

            tablePanels.add(generateCntAllPanel(dbId, tableName));
            tablePanels.add(generateStatsPanel(dbId, tId, tableName));

            if (timeSeriesParamMap.containsKey(tId)) {
                addTimeSeriesPanel(dbId, tId, tableName, columns, timeSeriesParamMap, tablePanels);
            }

            for (var col : columns) {
                ColumnTypeDto columnType = col.getColumnType();

                if (isNumericalColumn(columnType)) {

                    if (histogramParamMap.containsKey(tId) && histogramParamMap.get(tId).containsKey(col.getId())) {
                        tablePanels.add(generateHistogramPanel(dbId, tableName, col.getInternalName(), histogramParamMap.get(tId).get(col.getId())));
                    } else {
                        tablePanels.add(generateHistogramPanel(dbId, tableName, col.getInternalName(), null));
                    }
                } else if (isStringColumn(columnType)) {

                    if (pieChartParamMap.containsKey(tId) && pieChartParamMap.get(tId).containsKey(col.getId())) {
                        tablePanels.add(generatePieChartPanel(dbId, tableName, col.getInternalName(), pieChartParamMap.get(tId).get(col.getId())));
                    } else {
                        tablePanels.add(generatePieChartPanel(dbId, tableName, col.getInternalName(), null));
                    }
                } else if (isTimeStamp(columnType)) {
                    List<String> numColumns = new ArrayList<>();

                    for(var other : columns) {
                        if (isNumericalColumn(other.getColumnType())) {
                            numColumns.add(other.getInternalName());
                        }
                    }

                    tablePanels.add(generateMultiTimeSeriesPanel(dbId, tableName, col.getInternalName(), numColumns));
                }
            }

            rowPanels.add(new RowPanel(tableName, tablePanels).getConstructedPanel());
            AbstractPanel.markNewRow();
        }

        int refreshRate = defaultRefreshrate;
        if (dashboardConfigDto.getRefreshRate() != null) {
            refreshRate = dashboardConfigDto.getRefreshRate();
        }

        Dashboard d = new Dashboard();
        String dashboardJson = d.getDashboard(rowPanels, dbId, refreshRate);

        String relativeUrl = createDashboard(dashboardJson);

        return String.format("%s:%s%s%n", baseUrl, grafanaPort, relativeUrl);
    }

    private String generateCntAllPanel(Long dbId, String tableName) {
        Long viewId = this.viewGeneratorService.genCntAllView(dbId, tableName, this.token);

        CntAllPanel panel = new CntAllPanel(dbId, viewId);
        return panel.getConstructedPanel();
    }

    private String generatePieChartPanel(Long dbId, String tableName, String colName, PieChartConfigDto config) {
        Long viewId = this.viewGeneratorService.genPieChartView(dbId, tableName, colName, config, this.token);
        PieChartPanel panel = new PieChartPanel(dbId, viewId, colName, config);
        return panel.getConstructedPanel();
    }

    private String generateTablePanel(Long dbId, Long tId, String tableName, Long size) {
        TablePanel panel = new TablePanel(dbId, tId, tableName, size);
        return panel.getConstructedPanel();
    }

    private String generateHistogramPanel(Long dbId, String tableName, String colName, HistogramConfigDto config) {
        Long viewId = this.viewGeneratorService.genHistogramView(dbId, tableName, colName, this.token);
        HistogramPanel panel = new HistogramPanel(dbId, viewId, colName, config);
        return panel.getConstructedPanel();
    }

    private String generateStatsPanel(Long dbId, Long tId, String tableName) {
        StatsPanel panel = new StatsPanel(dbId, tId, tableName);
        return panel.getConstructedPanel();
    }

    private String generateTimeSeriesPanel(Long dbId, String tableName, String valueName, Map<String, String> timeMap, Long size) {
        Long viewId = this.viewGeneratorService.genTimeSeriesView(dbId, tableName, timeMap, this.token);
        TimeSeriesPanel panel = new TimeSeriesPanel(dbId, viewId, valueName, size);
        return panel.getConstructedPanel();
    }

    private String generateMultiTimeSeriesPanel(Long dbId, String tableName, String timeCol, List<String> numValues) {
        Long viewId = this.viewGeneratorService.genMultiTimeSeriesView(dbId, tableName, timeCol, numValues, this.token);
        MultiTimeSeriesPanel panel = new MultiTimeSeriesPanel(dbId, viewId);
        return panel.getConstructedPanel();
    }

    private boolean isNumericalColumn(ColumnTypeDto type) {
        return switch (type) {
            case TINYINT, SMALLINT, MEDIUMINT, INT, BIGINT, FLOAT, DOUBLE, DECIMAL -> true;
            default -> false;
        };
    }

    private boolean isStringColumn(ColumnTypeDto type) {
        return switch (type) {
            case CHAR, VARCHAR, TINYTEXT, TEXT, MEDIUMTEXT, LONGTEXT -> true;
            default -> false;
        };
    }

    private boolean isTimeStamp(ColumnTypeDto type) {
        return switch (type) {
            case TIMESTAMP -> true;
            default -> false;
        };
    }

    private void addDatasourceIfNotPresent() {
        String jsonString = dataSourceService.getDatasource();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);

            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    if (node.has("uid") && node.get("uid").asText().equals(AbstractPanel.DATASRC_UID)) {
                        return;
                    }
                }
            }

            dataSourceService.addDatasource();
        } catch (Exception e) {
            log.error("failed to read json of datasource");
            throw new JsonProcessingException("Failed to process datasource json");
        }
    }

    private void addTimeSeriesPanel(Long dbId, Long tId, String tableName, List<ColumnDto> columns,
                                    Map<Long, List<TimeSeriesDto>> timeSeriesMap, List<String> tablePanels) {
        List<TimeSeriesDto> timeSeriesDtos = timeSeriesMap.get(tId);

        for (TimeSeriesDto timeSeriesDto : timeSeriesDtos) {
            TimeDto timeDto = timeSeriesDto.getTimeDto();

            Map<String, String> timeMap = new HashMap<>();
            String valueName = null;
            if (timeDto != null) {

                for (var col : columns) {
                    Long colId = col.getId();
                    String colName = col.getInternalName();

                    if (Objects.equals(timeDto.getYearColId(), colId)) {
                        timeMap.put(TIME_YEAR_COL, colName);
                    } else if (Objects.equals(timeDto.getMonthColId(), colId)) {
                        timeMap.put(TIME_MONTH_COL, colName);
                    } else if (Objects.equals(timeDto.getDayColId(), colId)) {
                        timeMap.put(TIME_DAY_COL, colName);
                    } else if (Objects.equals(timeDto.getHourColId(), colId)) {
                        timeMap.put(TIME_HOUR_COL, colName);
                    } else if (Objects.equals(timeDto.getMinuteColId(), colId)) {
                        timeMap.put(TIME_MIN_COL, colName);
                    } else if (Objects.equals(timeDto.getSecondColId(), colId)) {
                        timeMap.put(TIME_SECOND_COL, colName);
                    } else if (Objects.equals(timeSeriesDto.getValueColId(), colId)) {
                        valueName = colName;
                        timeMap.put(TIME_VAL_COL, valueName);
                    }
                }

                if (!timeMap.isEmpty()) {
                    tablePanels.add(generateTimeSeriesPanel(dbId, tableName, valueName, timeMap, timeSeriesDto.getSize()));
                }
            }
        }
    }

    private String checkForSync(Long dbId) {
        String path = String.format("/api/dashboards/uid/%d", dbId);

        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = grafanaRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.debug("dashboard with id {} already present", dbId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseEntity.getBody());

                return rootNode.get("meta").get("url").asText();
            }
        } catch (RestClientException e) {

            if (e.getMessage().startsWith("404")) {
                log.debug("dashboard with id {} not present", dbId);
                return null;
            }

            log.error("RestClient Exception occurred URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.GET, e);
            throw new ResourceAccessException("RestClient Exception occurred");
        } catch (Exception e) {
            log.error("Exception occurred, URL: {}, Exception message: {}, Request method: {}, Stack Trace: {}",
                    path, e.getMessage(), HttpMethod.GET, e);
            throw new ResourceAccessException("Exception occurred");
        }

        return null;
    }

    private String createDashboard(String dashboardJson) {
        String path = "/api/dashboards/db";

        HttpEntity<String> requestEntity = new HttpEntity<>(dashboardJson);

        try {
            ResponseEntity<String> responseEntity = grafanaRestTemplate.exchange(
                    path,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            String jsonString = responseEntity.getBody();
            JsonNode rootNode = mapper.readTree(jsonString);

            return rootNode.get("url").asText();

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

}