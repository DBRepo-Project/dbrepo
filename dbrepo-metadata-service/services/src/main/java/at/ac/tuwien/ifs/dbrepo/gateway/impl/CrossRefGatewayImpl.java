package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.crossref.CrossRefDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DoiNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.CrossRefGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class CrossRefGatewayImpl implements CrossRefGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public CrossRefGatewayImpl(@Qualifier("crossRefServiceRestTemplate") RestTemplate restTemplate,
                               GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public CrossRefDto findById(String id) throws DoiNotFoundException {
        final String path = "/fundingdata/funder/" + id;
        log.trace("find crossref metadata by id from endpoint {} with path {}", gatewayConfig.getCrossRefEndpoint(), path);
        final ResponseEntity<CrossRefDto> response;
        try {
            response = restTemplate.exchange(gatewayConfig.getCrossRefEndpoint() + path, HttpMethod.GET, HttpEntity.EMPTY, CrossRefDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to retrieve crossref metadata: {}", e.getMessage());
            throw new DoiNotFoundException("Failed to retrieve crossref metadata: " + e.getMessage(), e);
        }
        return response.getBody();
    }
}