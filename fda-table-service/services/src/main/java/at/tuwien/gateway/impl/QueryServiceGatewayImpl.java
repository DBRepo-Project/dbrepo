package at.tuwien.gateway.impl;

import at.tuwien.exception.AmqpException;
import at.tuwien.gateway.QueryServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class QueryServiceGatewayImpl implements QueryServiceGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public QueryServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void declareConsumer(Long containerId, Long databaseId, Long tableId, String authorization) throws AmqpException {
        final String url = "/api/container/" + containerId + "/database/" + databaseId + "/table/" + tableId + "/consumer";
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(null, headers), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to declare consumer for table with id {}", tableId);
            log.debug("failed to declare consumer for container with id {} database with id {} table with id {}",
                    containerId, databaseId, tableId);
            throw new AmqpException("Failed to declare consumer");
        }
    }

}
