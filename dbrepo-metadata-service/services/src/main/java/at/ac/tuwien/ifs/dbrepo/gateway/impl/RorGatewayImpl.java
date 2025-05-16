package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.ror.RorDto;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.RorNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.RorGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class RorGatewayImpl implements RorGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public RorGatewayImpl(RestTemplate restTemplate, GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public RorDto findById(String id) throws RorNotFoundException {
        final String path = "/organizations/" + id;
        log.trace("find ror by id at endpoint {} with path {}", gatewayConfig.getRorEndpoint(), path);
        final ResponseEntity<RorDto> response;
        try {
            response = restTemplate.exchange(gatewayConfig.getRorEndpoint() + path, HttpMethod.GET, HttpEntity.EMPTY, RorDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to retrieve ror metadata: {}", e.getMessage());
            throw new RorNotFoundException("Failed to retrieve ror metadata: " + e.getMessage(), e);
        }
        return response.getBody();
    }

}
