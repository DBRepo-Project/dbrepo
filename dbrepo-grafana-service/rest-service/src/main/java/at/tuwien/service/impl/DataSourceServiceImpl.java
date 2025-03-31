package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.panels.AbstractPanel;
import at.tuwien.service.DataService;
import at.tuwien.service.DataSourceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class DataSourceServiceImpl implements DataSourceService {

    private final RestTemplate grafanaRestTemplate;
    private String addDatasourceJSON = "{\n" +
            "    \"id\": null,\n" +
            "    \"uid\": \"" + AbstractPanel.DATASRC_UID + "\",\n" +
            "    \"name\": \"infinity datasource\",\n" +
            "    \"type\": \"yesoreyeram-infinity-datasource\",\n" +
            "    \"access\": \"proxy\"\n" +
            "}";

    @Autowired
    public DataSourceServiceImpl(@Qualifier("grafanaTemplate") RestTemplate grafanaRestTemplate) {
        this.grafanaRestTemplate = grafanaRestTemplate;
    }

    @Override
    public String addDatasource() {
        String path = "/api/datasources";

        HttpEntity<String> requestEntity = new HttpEntity<>(addDatasourceJSON);

        try {
            ResponseEntity<String> responseEntity = grafanaRestTemplate.exchange(
                    path,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
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

    @Override
    public String getDatasource() {
        String path = "/api/datasources";

        try {
            ResponseEntity<String> responseEntity = grafanaRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    String.class
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