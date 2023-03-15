package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.ConsumerDto;
import at.tuwien.config.AmqpConfig;
import at.tuwien.config.GatewayConfig;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    private final AmqpConfig amqpConfig;
    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;
    private final Environment environment;

    private final static String VIRTUAL_SERVER = "%2F";

    @Autowired
    public BrokerServiceGatewayImpl(AmqpConfig amqpConfig, RestTemplate restTemplate, GatewayConfig gatewayConfig,
                                    Environment environment) {
        this.amqpConfig = amqpConfig;
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
        this.environment = environment;
    }

    @Override
    public List<ConsumerDto> findAllConsumers() {
        final StringBuilder urlBuilder = new StringBuilder(gatewayConfig.getGatewayEndpoint())
                .append("/api");
        if (Arrays.stream(environment.getActiveProfiles()).noneMatch(p -> p.equals("junit"))) {
            urlBuilder.append("/broker");
        }
        urlBuilder.append("/consumers/")
                .append(VIRTUAL_SERVER);
        log.trace("gateway broker find all consumers, virtual server={}", VIRTUAL_SERVER);
        final URI findUri = URI.create(urlBuilder.toString());
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
