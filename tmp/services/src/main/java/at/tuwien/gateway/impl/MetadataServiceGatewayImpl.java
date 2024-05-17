package at.tuwien.gateway.impl;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.user.PrivilegedUserDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class MetadataServiceGatewayImpl implements MetadataServiceGateway {

    private final RestTemplate restTemplate;
    private final MetadataMapper metadataMapper;

    @Autowired
    public MetadataServiceGatewayImpl(RestTemplate restTemplate,
                                      MetadataMapper metadataMapper) {
        this.restTemplate = restTemplate;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public PrivilegedContainerDto getContainerById(Long containerId) throws RemoteUnavailableException,
            ContainerNotFoundException {
        final ResponseEntity<ContainerDto> response;
        try {
            response = restTemplate.exchange("/api/container/" + containerId, HttpMethod.GET, new HttpEntity<>(null),
                    ContainerDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find container: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to find container: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find container: body is null");
            throw new ContainerNotFoundException("Failed to find container: body is null");
        }
        final PrivilegedContainerDto container = metadataMapper.containerDtoToPrivilegedContainerDto(response.getBody());
        container.setUsername(response.getHeaders().get("X-Username").get(0));
        container.setPassword(response.getHeaders().get("X-Password").get(0));
        return container;
    }

    @Override
    public List<PrivilegedDatabaseDto> getDatabases() throws RemoteUnavailableException {
        final ResponseEntity<PrivilegedDatabaseDto[]> response;
        try {
            response = restTemplate.exchange("/api/database", HttpMethod.GET, new HttpEntity<>(null),
                    PrivilegedDatabaseDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find databases: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to find databases: " + e.getMessage(), e);
        }
        if (response.getBody() == null) {
            log.error("Failed to find databases: body is null");
            throw new RemoteUnavailableException("Failed to find databases: body is null");
        }
        return List.of(response.getBody());
    }

    @Override
    public PrivilegedDatabaseDto getDatabaseById(Long id) throws DatabaseNotFoundException, RemoteUnavailableException {
        final ResponseEntity<PrivilegedDatabaseDto> response;
        try {
            response = restTemplate.exchange("/api/database/" + id, HttpMethod.GET, new HttpEntity<>(null),
                    PrivilegedDatabaseDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find database with id {}: {}", id, e.getMessage());
            throw new RemoteUnavailableException("Failed to find database with id " + id + ": " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find database with id {}: body is null", id);
            throw new DatabaseNotFoundException("Failed to find database id " + id + ": body is null", e);
        }
        final PrivilegedDatabaseDto database = response.getBody();
        database.getContainer().setUsername(response.getHeaders().get("X-Username").get(0));
        database.getContainer().setPassword(response.getHeaders().get("X-Password").get(0));
        log.debug("found privileged database username={}, password={}", database.getContainer().getUsername(),
                database.getContainer().getPassword().isEmpty() ? "(empty)" : "(hidden)");
        return database;
    }

    @Override
    public PrivilegedDatabaseDto getDatabaseByInternalName(String internalName) throws DatabaseNotFoundException,
            RemoteUnavailableException {
        final ResponseEntity<PrivilegedDatabaseDto[]> response;
        try {
            response = restTemplate.exchange("/api/database/", HttpMethod.GET, new HttpEntity<>(null), PrivilegedDatabaseDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find database with internal name {}: {}", internalName, e.getMessage());
            throw new RemoteUnavailableException("Failed to find database with internal name " + internalName + ": " + e.getMessage(), e);
        }
        if (response.getBody() == null || response.getBody().length != 1) {
            log.error("Failed to find database with internal name {}: body is null", internalName);
            throw new DatabaseNotFoundException("Failed to find database with internal name " + internalName + ": body is null");
        }
        return response.getBody()[0];
    }

    @Override
    public PrivilegedTableDto getTableById(Long databaseId, Long id) throws TableNotFoundException, RemoteUnavailableException {
        final ResponseEntity<TableDto> response;
        try {
            response = restTemplate.exchange("/api/database/" + databaseId + "/table/" + id, HttpMethod.GET, new HttpEntity<>(null), TableDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find table with id {}: {}", id, e.getMessage());
            throw new RemoteUnavailableException("Failed to find table with id " + id + ": " + e.getMessage(), e);
        }
        if (response.getBody() == null) {
            log.error("Failed to find table with id {}: body is null", id);
            throw new TableNotFoundException("Failed to find table with id " + id + ": body is null");
        }
        final PrivilegedTableDto table = metadataMapper.tableDtoToPrivilegedTableDto(response.getBody());
        table.getDatabase().getContainer().getImage().setJdbcMethod(response.getHeaders().get("X-Type").get(0));
        table.getDatabase().getContainer().setHost(response.getHeaders().get("X-Host").get(0));
        table.getDatabase().getContainer().setPort(Integer.parseInt(response.getHeaders().get("X-Port").get(0)));
        table.getDatabase().getContainer().setUsername(response.getHeaders().get("X-Username").get(0));
        table.getDatabase().getContainer().setPassword(response.getHeaders().get("X-Password").get(0));
        table.getDatabase().setInternalName(response.getHeaders().get("X-Database").get(0));
        table.getDatabase().getContainer().setSidecarHost(response.getHeaders().get("X-Sidecar-Host").get(0));
        table.getDatabase().getContainer().setSidecarPort(Integer.parseInt(response.getHeaders().get("X-Sidecar-Port").get(0)));
        log.debug("found privileged database username={}, password={}",
                table.getDatabase().getContainer().getUsername(),
                table.getDatabase().getContainer().getPassword().isEmpty() ? "(empty)" : "(hidden)");
        return table;
    }

    @Override
    public PrivilegedViewDto getViewById(Long databaseId, Long id) throws TableNotFoundException, RemoteUnavailableException {
        final ResponseEntity<ViewDto> response;
        try {
            response = restTemplate.exchange("/api/database/" + databaseId + "/view/" + id, HttpMethod.GET, new HttpEntity<>(null), ViewDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find view with id {}: {}", id, e.getMessage());
            throw new RemoteUnavailableException("Failed to find view with id " + id + ": " + e.getMessage(), e);
        }
        if (response.getBody() == null) {
            log.error("Failed to find view with id {}: body is null", id);
            throw new TableNotFoundException("Failed to find view with id " + id + ": body is null");
        }
        final PrivilegedViewDto table = metadataMapper.viewDtoToPrivilegedViewDto(response.getBody());
        table.getDatabase().getContainer().getImage().setJdbcMethod(response.getHeaders().get("X-Type").get(0));
        table.getDatabase().getContainer().setHost(response.getHeaders().get("X-Host").get(0));
        table.getDatabase().getContainer().setPort(Integer.parseInt(response.getHeaders().get("X-Port").get(0)));
        table.getDatabase().getContainer().setUsername(response.getHeaders().get("X-Username").get(0));
        table.getDatabase().getContainer().setPassword(response.getHeaders().get("X-Password").get(0));
        table.getDatabase().setInternalName(response.getHeaders().get("X-Database").get(0));
        return table;
    }

    @Override
    public PrivilegedUserDto getUserById(UUID userId) throws RemoteUnavailableException, UserNotFoundException {
        final ResponseEntity<PrivilegedUserDto> response;
        try {
            response = restTemplate.exchange("/api/user/" + userId, HttpMethod.GET, new HttpEntity<>(null), PrivilegedUserDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find user with id {}: {}", userId, e.getMessage());
            throw new RemoteUnavailableException("Failed to find user with id " + userId + ": " + e.getMessage(), e);
        }
        if (response.getBody() == null) {
            log.error("Failed to find User: body is null");
            throw new UserNotFoundException("Failed to find User: body is null");
        }
        return response.getBody();
    }

}
