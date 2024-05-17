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
    public void grantTopicPermission(String username, ExchangeUpdatePermissionsDto data)
            throws ServiceConnectionException, ServiceException {
        final String url = "/api/topic-permissions/" + rabbitConfig.getVirtualHost() + "/" + username;
        log.debug("grant topic permission in url {}{}", gatewayConfig.getBrokerEndpoint(), url);
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
        log.debug("grant virtual host permissions in url {}{}", gatewayConfig.getBrokerEndpoint(), url);
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
        log.debug("grant topic permissions in url {}{}", gatewayConfig.getBrokerEndpoint(), url);
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

    @Override
    public QueueDto findQueue(String name) throws ServiceConnectionException, ServiceException, QueueNotFoundException {
        final String url = "/api/queues/" + rabbitConfig.getVirtualHost() + "/" + name;
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        log.trace("gateway broker find queue, virtual host={}, queue={}", rabbitConfig.getVirtualHost(), name);
        log.debug("find queue from url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<QueueDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), QueueDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find queue: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to find queue: " + e.getMessage());
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find queue: not found: {}", e.getMessage());
            throw new QueueNotFoundException("Failed to find queue: not found: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to find queue: unexpected response: {}", e.getMessage());
            throw new ServiceException("Failed to find queue: unexpected response: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find queue: unexpected status: {}", response.getStatusCode().value());
            throw new ServiceException("Failed to find queue: unexpected status: " + response.getStatusCode().value());
        }
        return response.getBody();
    }

    @Override
    public ExchangeDto findExchange(String name) throws ServiceException, ServiceConnectionException, ExchangeNotFoundException {
        final String url = "/api/exchanges/" + rabbitConfig.getVirtualHost() + "/" + name;
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        log.trace("gateway broker find exchange, virtual host={}, exchange={}", rabbitConfig.getVirtualHost(), name);
        log.debug("find exchange from url {}{}", gatewayConfig.getBrokerEndpoint(), url);
        final ResponseEntity<ExchangeDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), ExchangeDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find exchange: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to find exchange: " + e.getMessage());
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find exchange: not found: {}", e.getMessage());
            throw new ExchangeNotFoundException("Failed to find exchange: not found: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to find exchange: unexpected response: {}", e.getMessage());
            throw new ServiceException("Failed to find exchange: unexpected response: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find exchange: unexpected status: {}", response.getStatusCode().value());
            throw new ServiceException("Failed to find exchange: unexpected status: " + response.getStatusCode().value());
        }
        return response.getBody();
    }

}
