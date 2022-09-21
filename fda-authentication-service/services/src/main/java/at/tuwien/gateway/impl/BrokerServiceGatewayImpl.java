package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.exception.BrokerUserCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final AmqpConfig amqpConfig;
    private final RestTemplate restTemplate;

    @Autowired
    public BrokerServiceGatewayImpl(RestTemplate restTemplate, AmqpConfig amqpConfig) {
        this.amqpConfig = amqpConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public void createUser(String username, CreateUserDto data) throws BrokerUserCreationException {
        /* create user */
        final String createUrl = "/api/broker/users/" + username;
        final ResponseEntity<Void> createResponse = restTemplate.exchange(createUrl, HttpMethod.PUT,
                new HttpEntity<>(data, getHeaders()), Void.class);
        if (!createResponse.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user at broker service: {}", createResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to create user at broker service");
        }
        log.info("Created user at broker service with username {}", username);
    }

    @Override
    public void modifyUserPassword(String username, CreateUserDto data) throws BrokerUserCreationException {
        /* modify at broker service */
        log.debug("modify user at broker service {}", data);
        final String modifyUrl = "/api/broker/users/" + username;
        final ResponseEntity<Void> response = restTemplate.exchange(modifyUrl, HttpMethod.PUT,
                new HttpEntity<>(data, getHeaders()), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to update user password at queue service: {}", response.getStatusCode());
            throw new BrokerUserCreationException("Failed to update user password at queue service");
        }
        log.info("Updated user password at queue service for username {}", username);
        log.debug("updated user password at queue service {}", data);
    }

    /**
     * Retrieves the authentication headers from the configuration for the broker service.
     *
     * @return The headers.
     */
    private HttpHeaders getHeaders() {
        return new HttpHeaders() {{
            String auth = amqpConfig.getAmqpUsername() + ":" + amqpConfig.getAmqpPassword();
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.defaultCharset()));
            String authHeader = "Basic " + new String(encodedAuth);
            set("Authorization", authHeader);
        }};
    }

}
