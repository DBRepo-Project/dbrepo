package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public BrokerServiceGatewayImpl(@Qualifier("brokerRestTemplate") RestTemplate restTemplate,
                                    GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostCreationException {
        final ResponseEntity<Void> response = restTemplate.exchange(gatewayConfig.getGatewayEndpoint() + "/api/broker/vhost", HttpMethod.POST,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostCreationException("Failed to create virtual host");
        }
        log.info("Create virtual host with name {}", data.getName());
    }

    @Override
    public void grantPermission(String username, ExchangeUpdatePermissionsDto data)
            throws BrokerVirtualHostGrantException {
        final URI grantUri = URI.create(gatewayConfig.getGatewayEndpoint() + "/api/broker/topic-permissions/%2F/" + username);
        final ResponseEntity<Void> response = restTemplate.exchange(grantUri, HttpMethod.PUT,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant exchange: {}", response.getStatusCode());
            throw new BrokerVirtualHostGrantException("Failed to grant exchange");
        }
        log.info("Grant exchange for user with username {}", username);
    }

    @Override
    public void grantPermission(String username, GrantVirtualHostPermissionsDto data)
            throws BrokerVirtualHostGrantException {
        final URI grantUri = URI.create(gatewayConfig.getGatewayEndpoint() + "/api/broker/permissions/%2F/" + username);
        final ResponseEntity<Void> response = restTemplate.exchange(grantUri, HttpMethod.PUT,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostGrantException("Failed to grant virtual host");
        }
        log.info("Grant permission for user with username {}", username);
    }

}
