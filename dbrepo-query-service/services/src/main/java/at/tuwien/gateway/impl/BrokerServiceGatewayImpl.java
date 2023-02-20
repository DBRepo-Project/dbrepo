package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.GatewayConfig;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final AmqpConfig amqpConfig;
    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    private final static String VIRTUAL_SERVER = "%2F";

    @Autowired
    public BrokerServiceGatewayImpl(AmqpConfig amqpConfig, RestTemplate restTemplate, GatewayConfig gatewayConfig) {
        this.amqpConfig = amqpConfig;
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public List<ConsumerDto> findAllConsumers() {
        log.trace("gateway broker find all consumers, virtual server={}", VIRTUAL_SERVER);
        final URI findUri = URI.create(gatewayConfig.getGatewayEndpoint() + "/api/broker/consumers/" + VIRTUAL_SERVER);
        final ResponseEntity<List<ConsumerDto>> response = restTemplate.exchange(findUri, HttpMethod.GET,
                new HttpEntity<>(null, getHeaders()), new ParameterizedTypeReference<>() {
                });
        return response.getBody();
    }

    /**
     * Retrieves the authentication headers from the configuration for the broker service.
     *
     * @return The headers.
     */
    private HttpHeaders getHeaders() {
        String auth = amqpConfig.getAmqpUsername() + ":" + amqpConfig.getAmqpPassword();
        log.trace("set Authorization header username={}, password=(redacted)", amqpConfig.getAmqpUsername());
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.defaultCharset()));
        String authHeader = "Basic " + new String(encodedAuth);
        return new HttpHeaders() {{
            set("Authorization", authHeader);
        }};
    }

}
