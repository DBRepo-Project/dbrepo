package at.tuwien.gateway.impl;

import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.AnalyseServiceException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.StorageNotFoundException;
import at.tuwien.gateway.AnalyseServiceGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class AnalyseServiceGatewayImpl implements AnalyseServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public AnalyseServiceGatewayImpl(RestTemplate restTemplate, GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void importDataset(Long databaseId, Long tableId, String filename) throws StorageNotFoundException,
            RemoteUnavailableException, AnalyseServiceException {
        final ResponseEntity<Void> response;
        final String url = new StringBuilder(gatewayConfig.getAnalyseEndpoint())
                .append("/api/analyse/import?database_id=")
                .append(databaseId)
                .append("&table_id=")
                .append(tableId)
                .append("&filename=")
                .append(filename)
                .toString();
        log.debug("import file into data database: {}", url);
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity.EMPTY, Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to import dataset with filename: {}: {}", filename, e.getMessage());
            throw new RemoteUnavailableException("Failed to import dataset: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to import dataset with filename: {}: not found: {}", filename, e.getMessage());
            throw new StorageNotFoundException("Failed to import dataset: not found: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to import dataset with filename: {}: service responded unsuccessful: {}", filename, response.getStatusCode());
            throw new AnalyseServiceException("Failed to import dataset: service responded unsuccessful: " + response.getStatusCode());
        }
    }

    @Override
    public void exportTable(Long databaseId, Long tableId) throws StorageNotFoundException, RemoteUnavailableException,
            AnalyseServiceException {
        final ResponseEntity<Void> response;
        final String url = new StringBuilder(gatewayConfig.getAnalyseEndpoint())
                .append("/api/analyse/export?database_id=")
                .append(databaseId)
                .append("&table_id=")
                .append(tableId)
                .toString();
        log.debug("export file from data database: {}", url);
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, HttpEntity.EMPTY, Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to export dataset: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to export dataset: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to export dataset: not found: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to export dataset: not found: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to export dataset: service responded unsuccessful: {}", response.getStatusCode());
            throw new AnalyseServiceException("Failed to export dataset: service responded unsuccessful: " + response.getStatusCode());
        }
    }
}
