package at.tuwien.gateway.impl;

import at.tuwien.exception.DataDbSidecarException;
import at.tuwien.gateway.DataDbSidecarGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class DataDbSidecarGatewayImpl implements DataDbSidecarGateway {

    private final RestTemplate restTemplate;

    public DataDbSidecarGatewayImpl(@Qualifier("sidecarRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void importFile(String hostname, Integer port, String filename) throws DataDbSidecarException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        try {
            restTemplate.exchange("http://" + hostname + ":" + port + "/sidecar/import/" + filename, HttpMethod.POST, new HttpEntity<>(null, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to import .csv in data-db sidecar: {}", e.getMessage());
            throw new DataDbSidecarException("Failed to import .csv in data-db sidecar: " + e.getMessage());
        }
    }

    @Override
    public void exportFile(String hostname, Integer port, String filename) throws DataDbSidecarException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        try {
            restTemplate.exchange("http://" + hostname + ":" + port + "/sidecar/export/" + filename, HttpMethod.POST, new HttpEntity<>(null, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to export .csv in data-db sidecar: {}", e.getMessage());
            throw new DataDbSidecarException("Failed to export .csv in data-db sidecar: " + e.getMessage());
        }
    }
}
