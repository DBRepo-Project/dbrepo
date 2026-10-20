package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
public class DataServiceGatewayImpl implements DataServiceGateway {

    public static final String AMQP_USER_HEADER = "X-DBRepo-AMQP-User";

    private final RestTemplate restTemplate;

    @Autowired
    public DataServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Observed(name = "dbrepo_consumer_insert_tuple")
    public void insertRawTuple(UUID databaseId, UUID tableId, TupleDto tuple, String amqpUsername)
            throws RemoteUnavailableException, TableNotFoundException, DataServiceException {
        final ResponseEntity<Void> response;
        final String url = "/api/v1/database/" + databaseId + "/table/" + tableId + "/data";
        final HttpHeaders headers = new HttpHeaders();
        if (amqpUsername != null && !amqpUsername.isBlank()) {
            headers.set(AMQP_USER_HEADER, amqpUsername);
        }
        log.debug("insert raw tuple into data service: {}", url);
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(tuple, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to insert raw tuple: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to insert raw tuple: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find table with id {}: {}", tableId, e.getMessage());
            throw new TableNotFoundException("Failed to find table: " + e.getMessage(), e);
        }
        if (response.getStatusCode() != HttpStatus.CREATED) {
            log.error("Failed to insert raw tuple into data service: service responded unsuccessful: {}", response.getStatusCode());
            throw new DataServiceException("Failed to insert raw tuple into data service: service responded unsuccessful: " + response.getStatusCode());
        }
    }

}
