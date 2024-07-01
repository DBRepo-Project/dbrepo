package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.*;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.config.RabbitConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final RestTemplate restTemplate;
    private final RabbitConfig rabbitConfig;

    @Autowired
    public BrokerServiceGatewayImpl(@Qualifier("brokerRestTemplate") RestTemplate restTemplate,
                                    RabbitConfig rabbitMqConfig) {
        this.restTemplate = restTemplate;
        this.rabbitConfig = rabbitMqConfig;
    }

    @Override
    public void grantTopicPermission(String username, ExchangeUpdatePermissionsDto data)
            throws ServiceConnectionException, ServiceException {
        final String url = "/api/topic-permissions/" + rabbitConfig.getVirtualHost() + "/" + username;
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to grant topic permissions: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to grant topic permissions: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to grant topic permissions: unexpected response: {}", e.getMessage());
            throw new ServiceException("Failed to grant topic permissions: unexpected response: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant topic permissions: unexpected status: {}", response.getStatusCode().value());
            throw new ServiceException("Failed to grant topic permissions: unexpected status: " + response.getStatusCode().value());
        }
    }

    @Override
    public void grantVirtualHostPermission(String username, GrantVirtualHostPermissionsDto data) throws ServiceConnectionException, ServiceException {
        final String url = "/api/permissions/" + rabbitConfig.getVirtualHost() + "/" + username;
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to grant virtual host permissions: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to grant virtual host permissions: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to grant virtual host permissions: unexpected response: {}", e.getMessage());
            throw new ServiceException("Failed to grant virtual host permissions: unexpected response: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant virtual host permissions: unexpected status: {}", response.getStatusCode().value());
            throw new ServiceException("Failed to grant virtual host permissions: unexpected status: " + response.getStatusCode().value());
        }
    }

    @Override
    public void grantExchangePermission(String username, GrantExchangePermissionsDto data) throws ServiceConnectionException, ServiceException {
        final String url = "/api/topic-permissions/" + rabbitConfig.getVirtualHost() + "/" + username;
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to grant exchange permissions: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to grant exchange permissions: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to grant exchange permissions: unexpected response: {}", e.getMessage());
            throw new ServiceException("Failed to grant exchange permissions: unexpected response: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED) && !response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to grant exchange permissions: unexpected status: {}", response.getStatusCode().value());
            throw new ServiceException("Failed to grant exchange permissions: unexpected status: " + response.getStatusCode().value());
        }
    }

}
