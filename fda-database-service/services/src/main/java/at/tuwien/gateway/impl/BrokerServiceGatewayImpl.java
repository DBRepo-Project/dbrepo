package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final AmqpConfig amqpConfig;
    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public BrokerServiceGatewayImpl(AmqpConfig amqpConfig, @Qualifier("brokerRestTemplate") RestTemplate restTemplate,
                                    GatewayConfig gatewayConfig) {
        this.amqpConfig = amqpConfig;
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostCreationException {
        final ResponseEntity<Void> response = restTemplate.exchange(gatewayConfig.getGatewayEndpoint() + "/api/broker/vhost", HttpMethod.POST,
                new HttpEntity<>(data, httpHeaders()), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostCreationException("Failed to create virtual host");
        }
        log.info("Create virtual host with name {}", data.getName());
    }

    @Override
    public void grantPermission(String username, ExchangeUpdatePermissionsDto data)
            throws BrokerVirtualHostCreationException {
        final URI grantUri = URI.create(gatewayConfig.getGatewayEndpoint() + "/api/broker/topic-permissions/%2F/" + username);
        final ResponseEntity<Void> response = restTemplate.exchange(grantUri, HttpMethod.PUT,
                new HttpEntity<>(data, httpHeaders()), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant exchange: {}", response.getStatusCode());
            throw new BrokerVirtualHostCreationException("Failed to grant exchange");
        }
        log.info("Grant exchange for user with username {}", username);
    }

    @Override
    public void grantPermission(String username, GrantVirtualHostPermissionsDto data)
            throws BrokerVirtualHostCreationException {
        final URI grantUri = URI.create(gatewayConfig.getGatewayEndpoint() + "/api/broker/permissions/%2F/" + username);
        final ResponseEntity<Void> response = restTemplate.exchange(grantUri, HttpMethod.PUT,
                new HttpEntity<>(data, httpHeaders()), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostCreationException("Failed to grant virtual host");
        }
        log.info("Grant permission for user with username {}", username);
    }

    private HttpHeaders httpHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", Base64.getEncoder()
                .encodeToString((amqpConfig.getAmpqUsername() + ":" + amqpConfig.getAmpqPassword())
                        .getBytes(StandardCharsets.UTF_8)));
        return headers;
    }

}
