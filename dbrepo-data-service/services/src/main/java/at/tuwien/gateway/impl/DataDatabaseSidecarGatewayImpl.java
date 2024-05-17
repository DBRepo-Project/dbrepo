package at.tuwien.gateway.impl;

import at.tuwien.exception.SidecarExportException;
import at.tuwien.exception.SidecarImportException;
import at.tuwien.exception.StorageNotFoundException;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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
    public void importFile(String hostname, Integer port, String filename) throws SidecarImportException,
            StorageNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "http://" + hostname + ":" + port + "/sidecar/import/" + filename;
        log.debug("import file into data database sidecar");
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to import .csv in data-db sidecar: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to import .csv in data-db sidecar: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to import .csv in data-db sidecar");
            throw new SidecarImportException("Failed to import .csv in data-db sidecar");
        }
    }

    @Override
    public void exportFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            SidecarExportException {
        final ResponseEntity<Void> response;
        final String url = "http://" + hostname + ":" + port + "/sidecar/export/" + filename;
        log.debug("export file into data database sidecar: {}", url);
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to export .csv in data-db sidecar: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to export .csv in data-db sidecar: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to export .csv in data-db sidecar");
            throw new SidecarExportException("Failed to export .csv in data-db sidecar");
        }
    }
}
