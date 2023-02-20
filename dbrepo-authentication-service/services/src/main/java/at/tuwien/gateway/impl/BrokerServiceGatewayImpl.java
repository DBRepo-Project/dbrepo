package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.BrokerUserCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final AmqpConfig amqpConfig;
    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public BrokerServiceGatewayImpl(@Qualifier("gatewayRestTemplate") RestTemplate restTemplate, AmqpConfig amqpConfig,
                                    GatewayConfig gatewayConfig) {
        this.amqpConfig = amqpConfig;
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void createUser(String username, CreateUserDto data) throws BrokerUserCreationException {
        /* create user */
        final String createUrl = "/users/" + username;
        log.debug("create user, username={}, url={}, data={}", username, createUrl, data);
        final ResponseEntity<Void> createResponse = restTemplate.exchange(createUrl, HttpMethod.PUT,
                new HttpEntity<>(data, getHeaders()), Void.class);
        if (!createResponse.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user at broker service: {}", createResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to create user at broker service");
        }
        log.info("Created user at broker service with username {}", username);
    }

    @Override
    public UserDetailsDto findUser(String username) throws BrokerUserCreationException {
        /* create user */
        final String findUrl = "/users/" + username;
        log.debug("find user, username={}, url={}", username, findUrl);
        final ResponseEntity<UserDetailsDto> findResponse = restTemplate.exchange(findUrl, HttpMethod.GET,
                new HttpEntity<>(null, getHeaders()), UserDetailsDto.class);
        if (!findResponse.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find user at broker service: {}", findResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to create user at broker service");
        }
        log.info("Found user at broker service with username {}", username);
        return findResponse.getBody();
    }

    @Override
    public void modifyHostPermissions(String username, GrantVirtualHostPermissionsDto data) throws BrokerUserCreationException {
        /* create user */
        final URI modifyUri = URI.create(gatewayConfig.getGatewayEndpoint() + "/permissions/%2F/" + username);
        log.debug("modify host permissions, username= {}, url={}, data={}", username, modifyUri, data);
        final ResponseEntity<Void> createResponse = restTemplate.exchange(modifyUri, HttpMethod.PUT,
                new HttpEntity<>(data, getHeaders()), Void.class);
        if (!createResponse.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to modify user permissions at broker service: {}", createResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to modify user permissions at broker service");
        }
        log.info("Modified user permissions at broker service for user with username {}", username);
    }

    @Override
    public void modifyUserPassword(String username, CreateUserDto data) throws BrokerUserCreationException {
        /* modify at broker service */
        final String modifyUrl = "/users/" + username;
        log.debug("modify user password, username={}, url={}, data={}", username, modifyUrl, data);
        final ResponseEntity<Void> response = restTemplate.exchange(modifyUrl, HttpMethod.PUT,
                new HttpEntity<>(data, getHeaders()), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to update user password at queue service: {}", response.getStatusCode());
            throw new BrokerUserCreationException("Failed to update user password at queue service");
        }
        log.info("Updated user password at queue service for username {}", username);
    }

    /**
     * Retrieves the authentication headers from the configuration for the broker service.
     *
     * @return The headers.
     */
    private HttpHeaders getHeaders() {
        log.debug("authenticate at broker service with username={}", amqpConfig.getAmqpUsername());
        return new HttpHeaders() {{
            String auth = amqpConfig.getAmqpUsername() + ":" + amqpConfig.getAmqpPassword();
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.defaultCharset()));
            String authHeader = "Basic " + new String(encodedAuth);
            set("Authorization", authHeader);
        }};
    }

}
