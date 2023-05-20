package at.tuwien.gateway.impl;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.gateway.ContainerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ContainerServiceGatewayImpl implements ContainerServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public ContainerServiceGatewayImpl(@Qualifier("gatewayRestTemplate") RestTemplate restTemplate,
                                       GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public ContainerDto find(Long id) throws ContainerNotFoundException {
        final String url = gatewayConfig.getGatewayEndpoint() + "/api/container/" + id;
        final ResponseEntity<ContainerDto> response = restTemplate.exchange(url, HttpMethod.GET, null, ContainerDto.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find container: {}", response.getStatusCode());
            throw new ContainerNotFoundException("Failed to find container");
        }
        return response.getBody();
    }

}
