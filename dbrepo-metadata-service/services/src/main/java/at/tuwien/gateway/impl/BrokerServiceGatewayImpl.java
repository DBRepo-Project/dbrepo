package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateUserDto;
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
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final Environment environment;
    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public BrokerServiceGatewayImpl(Environment environment, @Qualifier("brokerRestTemplate") RestTemplate restTemplate,
                                    GatewayConfig gatewayConfig) {
        this.environment = environment;
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    private URI parseUrl(String path) {
        final String prefix = !Arrays.asList(environment.getActiveProfiles()).contains("junit") ? "/broker" : "";
        final URI url = URI.create(gatewayConfig.getGatewayEndpoint() + "/api" + prefix + path);
        log.debug("parse url: {}", url);
        return url;
    }

    @Override
    public void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostCreationException {
        final ResponseEntity<Void> response = restTemplate.exchange(parseUrl("/vhost"), HttpMethod.POST,
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
        final ResponseEntity<Void> response = restTemplate.exchange(parseUrl("/topic-permissions/dbrepo/" + username), HttpMethod.PUT,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant exchange: {}", response.getStatusCode());
            throw new BrokerVirtualHostGrantException("Failed to grant exchange");
        }
        log.info("Grant exchange for user with username {}", username);
    }

    @Override
    public void createUser(String username) throws BrokerVirtualHostCreationException {
        final CreateUserDto data = CreateUserDto.builder()
                .passwordHash("")
                .tags("")
                .build();
        final ResponseEntity<Void> response = restTemplate.exchange(parseUrl("/users/" + username), HttpMethod.PUT,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to create user: {}", response.getStatusCode());
            throw new BrokerVirtualHostCreationException("Failed to create user");
        }
        log.info("Created user with username {}", username);
    }

    @Override
    public void grantPermission(String username, GrantVirtualHostPermissionsDto data)
            throws BrokerVirtualHostGrantException {
        final ResponseEntity<Void> response = restTemplate.exchange(parseUrl("/permissions/dbrepo/" + username), HttpMethod.PUT,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostGrantException("Failed to grant virtual host");
        }
        log.info("Grant permission for user with username {}", username);
    }

}
