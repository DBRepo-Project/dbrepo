package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.PermissionTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.UpdateDashboardAccessDto;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceException;
import at.ac.tuwien.ifs.dbrepo.gateway.DashboardServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class DashboardServiceGatewayImpl implements DashboardServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public DashboardServiceGatewayImpl(@Qualifier("dashboardServiceRestTemplate") RestTemplate restTemplate,
                                       GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void update(DatabaseDto database) throws DashboardServiceConnectionException, DashboardServiceException {
        final ResponseEntity<Void> response;
        final String path = "/api/dashboard/" + database.getDashboardUid();
        log.trace("update dashboard at endpoint {} with path {}", gatewayConfig.getDashboardEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(database), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to update dashboard: {}", e.getMessage());
            throw new DashboardServiceConnectionException("Failed to update dashboard: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update dashboard: unauthorized: {}", e.getMessage());
            throw new DashboardServiceException("Failed to update dashboard: unauthorized: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update dashboard: response code is not 202");
            throw new DashboardServiceException("Failed to update dashboard: response code is not 202");
        }
        log.info("Updated dashboard with uid: {}", database.getDashboardUid());
    }

    @Override
    public CreateDashboardResponseDto create(CreateDashboardDto data) throws DashboardServiceConnectionException,
            DashboardServiceException {
        final ResponseEntity<CreateDashboardResponseDto> response;
        final String path = "/api/dashboard";
        log.trace("create dashboard at endpoint {} with path {}", gatewayConfig.getDashboardEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(data), CreateDashboardResponseDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to create dashboard: {}", e.getMessage());
            throw new DashboardServiceConnectionException("Failed to create dashboard: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create dashboard: malformed payload: {}", e.getMessage());
            throw new DashboardServiceException("Failed to create dashboard: malformed payload: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Conflict e) {
            log.error("Failed to create dashboard: exists: {}", e.getMessage());
            throw new DashboardServiceException("Failed to create dashboard: exists: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create dashboard: response code is not 201");
            throw new DashboardServiceException("Failed to create dashboard: response code is not 201");
        }
        final CreateDashboardResponseDto body = response.getBody();
        if (body == null) {
            log.error("Failed to create dashboard: body is empty");
            throw new DashboardServiceException("Failed to create dashboard: body is empty");
        }
        log.info("Created dashboard with uid: {}", body.getUid());
        return body;
    }

    @Override
    public void updateAccess(String dashboardUid, String username, PermissionTypeDto permission)
            throws DashboardServiceConnectionException, DashboardServiceException {
        final ResponseEntity<Void> response;
        final String path = "/api/dashboard/" + dashboardUid + "/access/" + username;
        log.trace("update dashboard access at endpoint {} with path {}", gatewayConfig.getDashboardEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(UpdateDashboardAccessDto.builder()
                    .permission(permission)
                    .build()), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to update dashboard access: {}", e.getMessage());
            throw new DashboardServiceConnectionException("Failed to update dashboard access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update dashboard access: malformed payload: {}", e.getMessage());
            throw new DashboardServiceException("Failed to update dashboard access: malformed payload: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update dashboard access: response code is not 202");
            throw new DashboardServiceException("Failed to update dashboard access: response code is not 202");
        }
        log.info("Updated dashboard access for user with username: {}", username);
    }

    @Override
    public void updateAnonymousAccess(String dashboardUid, DatabaseBriefDto database)
            throws DashboardServiceConnectionException, DashboardServiceException {
        final ResponseEntity<Void> response;
        final String path = "/api/dashboard/" + dashboardUid + "/access";
        log.trace("update dashboard anonymous access at endpoint {} with path {}", gatewayConfig.getDashboardEndpoint(),
                path);
        try {
            response = restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(database), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to update dashboard anonymous access: {}", e.getMessage());
            throw new DashboardServiceConnectionException("Failed to update dashboard anonymous access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update dashboard anonymous access: malformed payload: {}", e.getMessage());
            throw new DashboardServiceException("Failed to update dashboard anonymous access: malformed payload: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update dashboard access: response code is not 202");
            throw new DashboardServiceException("Failed to update dashboard access: response code is not 202");
        }
        log.info("Updated dashboard anonymous access");
    }
}
