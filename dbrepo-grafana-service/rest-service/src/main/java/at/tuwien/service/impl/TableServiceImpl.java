package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Log4j2
@Service
public class TableServiceImpl implements TableService {
    private final RestTemplate dataRestTemplate;
    private final RestTemplate metaDataRestTemplate;

    @Autowired
    public TableServiceImpl(@Qualifier("dataServiceRestTemplate") RestTemplate dataRestTemplate,
                            @Qualifier("metaDataServiceRestTemplate") RestTemplate metaDataRestTemplate) {
        this.dataRestTemplate = dataRestTemplate;
        this.metaDataRestTemplate = metaDataRestTemplate;
    }

    @Override
    public List<TableBriefDto> getAllTables(Long dbId) {
        String path = String.format("/api/database/%d/table", dbId);

        try {
            ResponseEntity<List<TableBriefDto>> responseEntity = metaDataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TableBriefDto>>() {}
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
    public TableDto getTableSchemas(Long dbId, Long tId) {
        String path = String.format("/api/database/%d/table/%d", dbId, tId);
        try {
            ResponseEntity<TableDto> responseEntity = metaDataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    TableDto.class
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
    public List<Map<String, Object>> getTableData(Long dbId, Long tId, Long size) {
        Long page = 0L;
        String path = String.format("/api/database/%d/table/%d/data?page=%d&size=%d", dbId, tId, page, size);

        try {
            // Send GET request with query parameters
            ResponseEntity<QueryResultDto> responseEntity = dataRestTemplate.exchange(
                    path,
                    HttpMethod.GET,
                    null,
                    QueryResultDto.class
            );

            QueryResultDto responseBody = responseEntity.getBody();

            return responseBody.getResult();

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