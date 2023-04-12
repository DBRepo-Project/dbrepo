package at.tuwien.utils;

import at.tuwien.api.amqp.CreateExchangeDto;
import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.amqp.QueueDto;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AmqpUtils {

    private final RestTemplate restTemplate;

    @Autowired
    public AmqpUtils(@Qualifier("brokerRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void createExchange(String exchange) {
        exchange = exchange.replace("/", "%2F");
        final URI uri = URI.create("http://dbrepo-broker-service:15672/api/exchanges/%2F/" + exchange);
        final CreateExchangeDto payload = CreateExchangeDto.builder()
                .type("fanout")
                .autoDelete(false)
                .durable(true)
                .internal(false)
                .build();
        final ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(payload), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create exchange, code is {}", response.getStatusCode());
            throw new RuntimeException("Failed to create exchange");
        }
    }

    public boolean exchangeExists(String exchange) {
        final ResponseEntity<ExchangeDto[]> response = restTemplate.exchange("/api/exchanges", HttpMethod.GET, null, ExchangeDto[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to retrieve exchanges, code is {}", response.getStatusCode());
            throw new RuntimeException("Failed to retrieve exchanges");
        }
        assert response.getBody() != null;
        final List<String> names = Arrays.stream(response.getBody())
                .map(ExchangeDto::getName)
                .collect(Collectors.toList());
        if (names.stream().filter(n -> n.equals(exchange)).count() != 1) {
            log.error("Failed to find exchange {} in exchanges {}", exchange, names);
            return false;
        }
        log.info("Found exchange {} in exchanges {}", exchange, names);
        return true;
    }

    public boolean queueExists(String queue) {
        final URI uri = URI.create("http://dbrepo-broker-service:15672/api/queues/%2F/");
        final ResponseEntity<QueueDto[]> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(null), QueueDto[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find queue, code is {}", response.getStatusCode());
            throw new RuntimeException("Failed to find queue");
        }
        assert response.getBody() != null;
        final List<String> names = Arrays.stream(response.getBody())
                .map(QueueDto::getName)
                .collect(Collectors.toList());
        if (names.stream().filter(n -> n.equals(queue)).count() != 1) {
            log.error("Failed to find queue {} in queues {}", queue, names);
            return false;
        }
        log.info("Found queue {} in queues {}", queue, names);
        return true;
    }

}
