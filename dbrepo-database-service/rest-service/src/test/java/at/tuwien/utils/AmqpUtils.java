package at.tuwien.utils;

import at.tuwien.api.amqp.ExchangeDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

}
