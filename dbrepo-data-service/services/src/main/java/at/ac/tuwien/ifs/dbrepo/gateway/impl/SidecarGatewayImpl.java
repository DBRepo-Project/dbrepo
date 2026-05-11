package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.gateway.SidecarGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SidecarGatewayImpl implements SidecarGateway {

    private final RestTemplate sidecarRestTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public SidecarGatewayImpl(RestTemplate sidecarRestTemplate, GatewayConfig gatewayConfig) {
        this.sidecarRestTemplate = sidecarRestTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void importCsv(String filename) throws RemoteUnavailableException,
            ContainerNotFoundException, MetadataServiceException {
        final ResponseEntity<Void> response;
        final String path = "/sidecar/import/" + filename;
        log.debug("import a csv into the sidecar: {}", gatewayConfig.getSidecarEndpoint() + path);
        try {
            response = sidecarRestTemplate.exchange(path, HttpMethod.POST, HttpEntity.EMPTY, Void.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to import file: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to import file: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find file: {}", e.getMessage());
            throw new ContainerNotFoundException("Failed to find file: " + e.getMessage(), e);
        }
        if (response.getStatusCode() != HttpStatus.ACCEPTED) {
            log.error("Failed to find file: service responded unsuccessful: {}", response.getStatusCode());
            throw new MetadataServiceException("Failed to find file: service responded unsuccessful: " + response.getStatusCode());
        }
    }
}
