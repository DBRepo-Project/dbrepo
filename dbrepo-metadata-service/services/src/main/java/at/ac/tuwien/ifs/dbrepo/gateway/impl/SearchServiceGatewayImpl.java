package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
public class SearchServiceGatewayImpl implements SearchServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;
    private final MetadataMapper metadataMapper;

    @Autowired
    public SearchServiceGatewayImpl(@Qualifier("searchServiceRestTemplate") RestTemplate restTemplate,
                                    GatewayConfig gatewayConfig, MetadataMapper metadataMapper) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public DatabaseBriefDto update(Database database) throws SearchServiceConnectionException, SearchServiceException,
            DatabaseNotFoundException {
        final ResponseEntity<DatabaseBriefDto> response;
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        final String path = "/api/search/database/" + database.getId();
        log.trace("update database at endpoint {} with path {}", gatewayConfig.getSearchEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(
                    metadataMapper.databaseToDatabaseDto(database), headers), DatabaseBriefDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to update database: {}", e.getMessage());
            throw new SearchServiceConnectionException("Failed to update database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update database: not found");
            throw new DatabaseNotFoundException("Failed to update database: not found", e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update database: malformed payload: {}", e.getMessage());
            throw new SearchServiceException("Failed to update database: malformed payload: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update database: response code is not 202");
            throw new SearchServiceException("Failed to update database: response code is not 202");
        }
        return response.getBody();
    }

    @Override
    public void delete(UUID databaseId) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/search/database/" + databaseId;
        log.trace("delete database at endpoint {} with path {}", gatewayConfig.getSearchEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to delete database: {}", e.getMessage());
            throw new SearchServiceConnectionException("Failed to delete database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete database: not found");
            throw new DatabaseNotFoundException("Failed to delete database: not found", e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete database: body is null");
            throw new SearchServiceException("Failed to delete database: body is null", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete database: response code is not 202");
            throw new SearchServiceException("Failed to delete database: response code is not 202");
        }
    }
}
