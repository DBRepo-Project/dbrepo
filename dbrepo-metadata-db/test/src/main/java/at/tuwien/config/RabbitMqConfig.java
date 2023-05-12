package at.tuwien.config;

import at.tuwien.api.amqp.ConsumerDto;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Configuration
public class RabbitMqConfig {

    private final RestTemplate restTemplate;

    @Autowired
    public RabbitMqConfig(@Qualifier("brokerRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<ConsumerDto> findAllConsumers() throws IOException {
        log.trace("gateway broker find all consumers");
        final URI findUri = URI.create("http://dbrepo-broker-service:15672/api/consumers/dbrepo");
        final ResponseEntity<List<ConsumerDto>> response = restTemplate.exchange(findUri, HttpMethod.GET,
                new HttpEntity<>(null, getHeaders()), new ParameterizedTypeReference<>() {
                });
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            log.error("Failed to get consumers");
            throw new IOException("Failed to get consumers");
        }
        final Map<String, Integer> consumers = new HashMap<>();
        for (ConsumerDto consumer : response.getBody()) {
            final String key = consumer.getQueue().getName();
            if (consumers.containsKey(key)) {
                final Integer value = consumers.get(key);
                consumers.replace(key, value + 1);
            } else {
                consumers.put(key, 1);
            }
        }
        for (Map.Entry<String, Integer> consumer : consumers.entrySet()) {
            log.trace("queue {} has {} consumers", consumer.getKey(), consumer.getValue());
        }
        return response.getBody();
    }

    private HttpHeaders getHeaders() {
        String auth = "guest:guest";
        log.trace("set Authorization header username={}, password={}", "guest", "guest");
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.defaultCharset()));
        String authHeader = "Basic " + new String(encodedAuth);
        return new HttpHeaders() {{
            set("Authorization", authHeader);
        }};
    }

}
