package at.tuwien.gateway.impl;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.gateway.QueryServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    public QueryDto find(IdentifierCreateDto identifier, String authorization) throws QueryNotFoundException,
            RemoteUnavailableException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        final String url = "/api/container/" + identifier.getCid() + "/database/" + identifier.getDbid() + "/query/" + identifier.getQid();
        final ResponseEntity<QueryDto> response;
        try {
            log.debug("call gateway path {}", url);
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), QueryDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Query service not available");
            log.debug("service not available for identifier {}", identifier);
            throw new RemoteUnavailableException("Query service not available", e);
        }
        if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            log.error("Query not found with id {}", identifier.getQid());
            log.debug("query not found for identifier {}", identifier);
            throw new QueryNotFoundException("Query not found");
        }
        if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            log.error("Query not authorized with id {}", identifier.getQid());
            log.debug("query not authorized for identifier {}", identifier);
            throw new RemoteUnavailableException("Query not authorized");
        }
        return response.getBody();
    }
}
