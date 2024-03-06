package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.*;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.config.RabbitConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;
    private final RabbitConfig rabbitConfig;

    @Autowired
    public BrokerServiceGatewayImpl(GatewayConfig gatewayConfig,
                                    @Qualifier("brokerRestTemplate") RestTemplate restTemplate,
                                    RabbitConfig rabbitMqConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
        this.rabbitConfig = rabbitMqConfig;
    }

    @Override
    public void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostModificationException, BrokerRemoteException {
        final String url = "/api/vhost";
        log.debug("create virtual host in url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data), Void.class);
        } catch (Exception e) {
            log.error("Failed to create virtual host: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to create virtual host: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostModificationException("Failed to create virtual host");
        }
        log.info("Create virtual host with name {}", data.getName());
    }

    @Override
    public void grantPermission(String username, ExchangeUpdatePermissionsDto data)
            throws BrokerVirtualHostGrantException, BrokerRemoteException {
        final String url = "/api/topic-permissions/" + rabbitConfig.getVirtualHost() + "/" + username;
        log.debug("grant topic permission in url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (Exception e) {
            log.error("Failed to grant permissions: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to grant permissions: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant topic: {}", response.getStatusCode());
            throw new BrokerVirtualHostGrantException("Failed to grant topic");
        }
        log.info("grant topic for user with username {}", username);
    }

    @Override
    public void createUser(String username, String password) throws BrokerRemoteException, BrokerVirtualHostModificationException {
        final CreateUserDto data = CreateUserDto.builder()
                .password(password)
                .tags("")
                .build();
        final String url = "/api/users/" + username;
        log.debug("create user from url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (Exception e) {
            log.error("Failed to create user: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to create user: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to create user: {}", response.getStatusCode());
            throw new BrokerVirtualHostModificationException("Failed to create user");
        }
        log.info("Created user with username {}", username);
    }

    @Override
    public void deleteUser(String username) throws BrokerRemoteException, BrokerVirtualHostModificationException {
        final String url = "/api/users/" + username;
        log.debug("delete user from url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null), Void.class);
        } catch (Exception e) {
            log.error("Failed to delete user: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to delete user: remote host answered unexpected: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to delete user: {}", response.getStatusCode());
            throw new BrokerVirtualHostModificationException("Failed to create user");
        }
        log.info("Deleted user with username {}", username);
    }

    @Override
    public void grantPermission(String username, GrantVirtualHostPermissionsDto data) throws BrokerRemoteException,
            BrokerVirtualHostGrantException {
        final String url = "/api/permissions/" + rabbitConfig.getVirtualHost() + "/" + username;
        log.debug("grant virtual host permissions in url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (Exception e) {
            log.error("Failed to grant virtual host permissions: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to create permissions: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant virtual host permissions at broker service");
            throw new BrokerVirtualHostGrantException("Failed to grant virtual host permissions at broker service");
        }
        log.trace("Grant virtual host permissions for user with username {}", username);
    }

    @Override
    public void grantTopicPermission(String username, GrantExchangePermissionsDto data) throws BrokerRemoteException,
            BrokerVirtualHostGrantException {
        final String url = "/api/topic-permissions/" + rabbitConfig.getVirtualHost() + "/" + username;
        log.debug("grant topic permissions in url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (Exception e) {
            log.error("Failed to grant topic permissions: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to grant topic permissions: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant topic permissions at broker service");
            throw new BrokerVirtualHostGrantException("Failed to grant topic permissions at broker service");
        }
        log.trace("Grant topic permissions for user with username {}", username);
    }

    @Override
    public QueueDto findQueue(String name) throws BrokerRemoteException, QueueNotFoundException {
        final String url = "/api/queues/" + rabbitConfig.getVirtualHost() + "/" + name;
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        log.trace("gateway broker find queue, virtual host={}, queue={}", rabbitConfig.getVirtualHost(), name);
        log.debug("find queue from url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<QueueDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), QueueDto.class);
        } catch (Exception e) {
            log.error("Failed to find queue: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to find queue: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed find queue at broker service");
            throw new QueueNotFoundException("Failed to find queue at broker service");
        }
        return response.getBody();
    }

    @Override
    public ExchangeDto findExchange(String name) throws BrokerRemoteException, ExchangeNotFoundException {
        final String url = "/api/exchanges/" + rabbitConfig.getVirtualHost() + "/" + name;
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        log.trace("gateway broker find exchange, virtual host={}, exchange={}", rabbitConfig.getVirtualHost(), name);
        log.debug("find exchange from url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<ExchangeDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), ExchangeDto.class);
        } catch (Exception e) {
            log.error("Failed to find exchange: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to find exchange: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed find exchange: {}", response.getStatusCode());
            throw new ExchangeNotFoundException("Failed to find exchange");
        }
        return response.getBody();
    }

}
