package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.*;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostModificationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    private final static String VIRTUAL_SERVER = "dbrepo";

    @Autowired
    public BrokerServiceGatewayImpl(GatewayConfig gatewayConfig,
                                    @Qualifier("brokerRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostModificationException, BrokerRemoteException {
        final String url = "/api/vhost";
        log.trace("POST {}{}", gatewayConfig.getBrokerEndpoint(), url);
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
        final String url = "/api/topic-permissions/dbrepo/" + username;
        log.trace("PUT {}{}", gatewayConfig.getBrokerEndpoint(), url);
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
        log.trace("PUT {}{}", gatewayConfig.getBrokerEndpoint(), url);
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
        log.trace("DELETE {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null), Void.class);
        } catch (Exception e) {
            log.error("Failed to delete user: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to delete user: remote host answered unexpected", e);
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
        final String url = "/api/permissions/dbrepo/" + username;
        log.trace("PUT {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (Exception e) {
            log.error("Failed to grant virtual host permissions: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to create permissions: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant virtual host permissions: {}", response.getStatusCode());
            throw new BrokerVirtualHostGrantException("Failed to grant virtual host permissions");
        }
        log.trace("Grant virtual host permissions for user with username {}", username);
    }

    @Override
    public void grantTopicPermission(String username, GrantExchangePermissionsDto data) throws BrokerRemoteException,
            BrokerVirtualHostGrantException {
        final String url = "/api/topic-permissions/dbrepo/" + username;
        log.trace("PUT {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (Exception e) {
            log.error("Failed to grant topic permissions: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to grant topic permissions: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant topic permissions: {}", response.getStatusCode());
            throw new BrokerVirtualHostGrantException("Failed to grant topic permissions");
        }
        log.trace("Grant topic permissions for user with username {}", username);
    }

    @Override
    public List<ConsumerDto> findAllConsumers() throws BrokerRemoteException {
        final String url = "/api/consumers/" + VIRTUAL_SERVER;
        log.trace("gateway broker find all consumers, virtual server={}", VIRTUAL_SERVER);
        log.trace("GET {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<List<ConsumerDto>> response;
        try {
            response = restTemplate.exchange(URI.create(url), HttpMethod.GET, HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {
                    });
        } catch (Exception e) {
            log.error("Failed to find consumers: remote host answered unexpected: {}", e.getMessage());
            throw new BrokerRemoteException("Failed to find consumers: remote host answered unexpected", e);
        }
        return response.getBody();
    }

}
