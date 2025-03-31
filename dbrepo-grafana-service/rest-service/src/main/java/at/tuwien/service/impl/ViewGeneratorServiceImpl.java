package at.tuwien.service.impl;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.dto.PieChartConfigDto;
import at.tuwien.panels.StatsPanel;
import at.tuwien.service.ViewGeneratorService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static at.tuwien.panels.MultiTimeSeriesPanel.VIEW_MULTI_TIMECOL;
import static at.tuwien.panels.PieChartPanel.VIEW_PIE_PERCENTAGE_COL;
import static at.tuwien.panels.TimeSeriesPanel.TIME_VAL_COL;

@Log4j2
@Service
public class ViewGeneratorServiceImpl implements ViewGeneratorService {

    private final RestTemplate metaDataRestTemplate;
    private static final String PIE_DEFAULT_LIMIT = "10";
    private static final String PIE_DEFAULT_DEC_PLACE = "2";

    @Autowired
    public ViewGeneratorServiceImpl(@Qualifier("metaDataServiceRestTemplate") RestTemplate metaDataRestTemplate) {
        this.metaDataRestTemplate = metaDataRestTemplate;
    }

    @Override
    public Long genCntAllView(Long dbId, String tableName, String token) {
        final String query = String.format("select count(*) from %s", tableName);

        ViewCreateDto viewCreateDto = new ViewCreateDto();
        viewCreateDto.setName(String.format("%d_cntAll_%s", dbId, tableName));
        viewCreateDto.setQuery(query);
        viewCreateDto.setIsPublic(true);

        ViewBriefDto createdView = createView(dbId, viewCreateDto, token);
        if (createdView == null) {
            log.warn("failed to create view {}", viewCreateDto);
            return -1L;
        }

        return createdView.getId();
    }

    @Override
    public Long genPieChartView(Long dbId, String tableName, String colName, PieChartConfigDto config, String token) {
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("col_name", colName);
        valueMap.put("table_name", tableName);
        valueMap.put("limit", (config != null && config.getLimit() != null) ? config.getLimit() : PIE_DEFAULT_LIMIT);
        valueMap.put("dec_place", (config != null && config.getDecimalPlace() != null) ? config.getDecimalPlace() : PIE_DEFAULT_DEC_PLACE);
        valueMap.put("percentage", VIEW_PIE_PERCENTAGE_COL);

        final String templateQuery =
                "SELECT\n" +
                        "        ${col_name},\n" +
                        "        ROUND( (COUNT(*) / total_count) * 100, ${dec_place}) AS ${percentage}\n" +
                        "        FROM\n" +
                        "                ${table_name},\n" +
                        "        (SELECT COUNT(*) AS total_count FROM ${table_name}) AS t\n" +
                        "        GROUP BY\n" +
                        "        ${col_name}\n" +
                        "        ORDER BY\n" +
                        "        ${percentage} DESC\n" +
                        "        LIMIT ${limit}";

        StringSubstitutor sub = new StringSubstitutor(valueMap);

        ViewCreateDto viewCreateDto = new ViewCreateDto();
        viewCreateDto.setName(String.format("%d_piechart_%s_%s", dbId, tableName, colName));
        viewCreateDto.setQuery(sub.replace(templateQuery));
        viewCreateDto.setIsPublic(true);

        ViewBriefDto createdView = createView(dbId, viewCreateDto, token);
        if (createdView == null) {
            log.warn("failed to create view {}", viewCreateDto);
            return -1L;
        }

        return createdView.getId();
    }

    @Override
    public Long genHistogramView(Long dbId, String tableName, String colName, String token) {
        final String query = String.format("select %s from %s", colName, tableName);

        ViewCreateDto viewCreateDto = new ViewCreateDto();
        viewCreateDto.setName(String.format("%d_histogram_%s_%s", dbId, tableName, colName));
        viewCreateDto.setQuery(query);
        viewCreateDto.setIsPublic(true);

        ViewBriefDto createdView = createView(dbId, viewCreateDto, token);
        if (createdView == null) {
            log.warn("failed to create view {}", viewCreateDto);
            return -1L;
        }

        return createdView.getId();
    }

    @Override
    public Long genStatisticsView(Long dbId, String tableName, String colName, String token) {
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("col_name", colName);
        valueMap.put("table_name", tableName);
        valueMap.put("header_col", StatsPanel.HEADER_COL);
        valueMap.put("header_min", StatsPanel.HEADER_MIN);
        valueMap.put("header_max", StatsPanel.HEADER_MAX);
        valueMap.put("header_avg", StatsPanel.HEADER_AVG);
        valueMap.put("header_stddev", StatsPanel.HEADER_STDDEV);

        final String templateQuery = "select " +
                "min(${col_name}) as ${header_min}, " +
                "max(${col_name}) as ${header_max}, " +
                "avg(${col_name}) as ${header_avg}, " +
                "STDDEV(${col_name}) as ${header_stddev} " +
                "from ${table_name}";

        StringSubstitutor sub = new StringSubstitutor(valueMap);

        ViewCreateDto viewCreateDto = new ViewCreateDto();
        viewCreateDto.setName(String.format("%d_statschart_%s_%s", dbId, tableName, colName));
        viewCreateDto.setQuery(sub.replace(templateQuery));
        viewCreateDto.setIsPublic(true);

        ViewBriefDto createdView = createView(dbId, viewCreateDto, token);
        if (createdView == null) {
            log.warn("failed to create view {}", viewCreateDto);
            return -1L;
        }

        return createdView.getId();
    }

    @Override
    public Long genTimeSeriesView(Long dbId, String tableName, Map<String, String> timeMap, String token) {

        List<String> colNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : timeMap.entrySet()) {
            colNames.add(String.format("%s AS %s", entry.getValue(), entry.getKey()));
        }

        final String query = String.format("select %s from %s", String.join(",", colNames), tableName);

        ViewCreateDto viewCreateDto = new ViewCreateDto();
        viewCreateDto.setName(String.format("%d_timeseries_%s_%s", dbId, tableName, timeMap.get(TIME_VAL_COL)));
        viewCreateDto.setQuery(query);
        viewCreateDto.setIsPublic(true);

        ViewBriefDto createdView = createView(dbId, viewCreateDto, token);
        if (createdView == null) {
            log.warn("failed to create view {}", viewCreateDto);
            return -1L;
        }

        return createdView.getId();
    }

    @Override
    public Long genMultiTimeSeriesView(Long dbId, String tableName, String timeCol, List<String> numValues, String token) {

        final String query = String.format("select %s AS %s, %s from %s", timeCol, VIEW_MULTI_TIMECOL, String.join(",", numValues), tableName);
        ViewCreateDto viewCreateDto = new ViewCreateDto();
        viewCreateDto.setName(String.format("%d_multitimeseries_%s_%s", dbId, tableName, timeCol));
        viewCreateDto.setQuery(query);
        viewCreateDto.setIsPublic(true);

        ViewBriefDto createdView = createView(dbId, viewCreateDto, token);
        if (createdView == null) {
            log.warn("failed to create view {}", viewCreateDto);
            return -1L;
        }

        return createdView.getId();
    }


    private ViewBriefDto createView(Long dbId, ViewCreateDto viewCreateDto, String token) {
        String path = String.format("/api/database/%d/view", dbId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("Content-Type", "application/json");

        HttpEntity<ViewCreateDto> requestEntity = new HttpEntity<>(viewCreateDto, headers);

        try {
            ResponseEntity<ViewBriefDto> responseEntity = metaDataRestTemplate.exchange(
                    path,
                    HttpMethod.POST,
                    requestEntity,
                    ViewBriefDto.class
            );

            return responseEntity.getBody();
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