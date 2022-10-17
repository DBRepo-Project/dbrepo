package at.tuwien.gateway.impl;

import at.tuwien.api.database.query.ExportDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.gateway.QueryServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class QueryServiceGatewayImpl implements QueryServiceGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public QueryServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public QueryDto find(Long containerId, Long databaseId, IdentifierCreateDto identifier, String authorization) throws QueryNotFoundException,
            RemoteUnavailableException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        final String url =
                "/api/container/" + containerId + "/database/" + databaseId + "/query/" + identifier.getQid();
        final ResponseEntity<QueryDto> response;
        try {
            log.trace("call gateway path {}", url);
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), QueryDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Query service not available for container with id {} and database with id {} for query with id {}, reason {}",
                    containerId, databaseId, identifier.getQid(), e.getMessage());
            throw new RemoteUnavailableException("Query service not available", e);
        }
        if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.error("Query not found for container with id {} and database with id {} for query with id {}",
                    containerId, databaseId, identifier.getQid());
            throw new QueryNotFoundException("Query not found");
        }
        if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            log.error("Query not authorized for container with id {} and database with id {} for query with id {}",
                    containerId, databaseId, identifier.getQid());
            throw new RemoteUnavailableException("Query not authorized");
        }
        log.debug("found query {}", response.getBody());
        return response.getBody();
    }

    @Override
    public ExportDto export(Long containerId, Long databaseId, Long queryId)
            throws RemoteUnavailableException, QueryNotFoundException {
        final String url = "/api/container/" + containerId + "/database/" + databaseId + "/query/" + queryId + "/export";
        final ResponseEntity<ExportDto> response;
        try {
            log.trace("call gateway path {}", url);
            response = restTemplate.exchange(url, HttpMethod.GET, null, ExportDto.class);
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            log.error("Query service not available: {}", e.getMessage());
            throw new RemoteUnavailableException("Query service not available", e);
        }
        if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.error("Query not found for container with id {} and database with id {} for query with id {}",
                    containerId, databaseId, queryId);
            throw new QueryNotFoundException("Query not found");
        }
        if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            log.error("Query not authorized for container with id {} and database with id {} for query with id {}",
                    containerId, databaseId, queryId);
            throw new RemoteUnavailableException("Query not authorized");
        }
        log.debug("found query {}", response.getBody());
        return response.getBody();
    }
}
