package at.tuwien.gateway.impl;

import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.DoiNotFoundException;
import at.tuwien.gateway.CrossrefGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class CrossrefGatewayImpl implements CrossrefGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public CrossrefGatewayImpl(RestTemplate restTemplate, GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public CrossrefDto findById(String id) throws DoiNotFoundException {
        final String path = "/fundingdata/funder/" + id;
        log.trace("find crossref metadata by id from endpoint {} with path {}", gatewayConfig.getCrossRefEndpoint(), path);
        final ResponseEntity<CrossrefDto> response;
        try {
            response = restTemplate.exchange(gatewayConfig.getCrossRefEndpoint() + path, HttpMethod.GET, HttpEntity.EMPTY, CrossrefDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to retrieve crossref metadata: {}", e.getMessage());
            throw new DoiNotFoundException("Failed to retrieve crossref metadata: " + e.getMessage(), e);
        }
        return response.getBody();
    }
}