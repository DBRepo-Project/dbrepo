package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.ExchangeUpdatePermissionsDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final Environment environment;
    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    private final static String VIRTUAL_SERVER = "dbrepo";

    @Autowired
    public BrokerServiceGatewayImpl(Environment environment, GatewayConfig gatewayConfig,
                                    @Qualifier("brokerRestTemplate") RestTemplate restTemplate) {
        this.environment = environment;
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    private String parseUrl(String path) {
        final String url = "/api" + path;
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

    @Override
    public List<ConsumerDto> findAllConsumers() {
        final StringBuilder urlBuilder = new StringBuilder(gatewayConfig.getBrokerEndpoint())
                .append("/api");
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch(p -> p.equals("junit"))) {
            urlBuilder.append("/broker");
        }
        urlBuilder.append("/consumers/")
                .append(VIRTUAL_SERVER);
        log.trace("gateway broker find all consumers, virtual server={}", VIRTUAL_SERVER);
        final URI findUri = URI.create(urlBuilder.toString());
        final ResponseEntity<List<ConsumerDto>> response = restTemplate.exchange(findUri, HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<>() {
                });
        return response.getBody();
    }

}
