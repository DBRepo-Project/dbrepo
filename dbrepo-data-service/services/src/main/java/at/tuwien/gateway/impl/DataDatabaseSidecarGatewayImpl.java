package at.tuwien.gateway.impl;

import at.tuwien.exception.*;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class DataDatabaseSidecarGatewayImpl implements DataDatabaseSidecarGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public DataDatabaseSidecarGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void importFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            RemoteUnavailableException, ServiceException {
        final ResponseEntity<Void> response;
        final String url = "http://" + hostname + ":" + port + "/sidecar/import/" + filename;
        log.debug("import file into data database sidecar");
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to import dataset with filename: {}: {}", filename, e.getMessage());
            throw new RemoteUnavailableException("Failed to import dataset: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to import dataset with filename: {}: not found: {}", filename, e.getMessage());
            throw new StorageNotFoundException("Failed to import dataset: not found: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to import dataset with filename: {}: service responded unsuccessful: {}", filename, response.getStatusCode());
            throw new ServiceException("Failed to import dataset: service responded unsuccessful: " + response.getStatusCode());
        }
    }

    @Override
    public void exportFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            RemoteUnavailableException, ServiceException {
        final ResponseEntity<Void> response;
        final String url = "http://" + hostname + ":" + port + "/sidecar/export/" + filename;
        log.debug("export file from data database sidecar: {}", url);
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to export dataset with filename: {}: {}", filename, e.getMessage());
            throw new RemoteUnavailableException("Failed to export dataset: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to export dataset with filename: {}: not found: {}", filename, e.getMessage());
            throw new StorageNotFoundException("Failed to export dataset: not found: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to export dataset with filename: {}: service responded unsuccessful: {}", filename, response.getStatusCode());
            throw new ServiceException("Failed to export dataset: service responded unsuccessful: " + response.getStatusCode());
        }
    }
}
