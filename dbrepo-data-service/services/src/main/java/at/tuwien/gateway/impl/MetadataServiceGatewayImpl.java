package at.tuwien.gateway.impl;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class MetadataServiceGatewayImpl implements MetadataServiceGateway {

    private final RestTemplate internalRestTemplate;
    private final GatewayConfig gatewayConfig;
    private final MetadataMapper metadataMapper;

    @Autowired
    public MetadataServiceGatewayImpl(@Qualifier("internalRestTemplate") RestTemplate internalRestTemplate,
                                      GatewayConfig gatewayConfig, MetadataMapper metadataMapper) {
        this.internalRestTemplate = internalRestTemplate;
        this.gatewayConfig = gatewayConfig;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public ContainerDto getContainerById(Long containerId) throws RemoteUnavailableException,
            ContainerNotFoundException, MetadataServiceException {
        final ResponseEntity<ContainerDto> response;
        final String url = "/api/container/" + containerId;
        log.debug("get  container info from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY,
                    ContainerDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find container with id {}: {}", containerId, e.getMessage());
            throw new RemoteUnavailableException("Failed to find container: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find container with id {}: {}", containerId, e.getMessage());
            throw new ContainerNotFoundException("Failed to find container: " + e.getMessage(), e);
        }
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to find container with id {}: service responded unsuccessful: {}", containerId, response.getStatusCode());
            throw new MetadataServiceException("Failed to find container: service responded unsuccessful: " + response.getStatusCode());
        }
        final List<String> expectedHeaders = List.of("X-Username", "X-Password");
        if (!response.getHeaders().keySet().containsAll(expectedHeaders)) {
            log.error("Failed to find all  container headers");
            log.debug("expected headers: {}", expectedHeaders);
            log.debug("found headers: {}", response.getHeaders().keySet());
            throw new MetadataServiceException("Failed to find all  container headers");
        }
        if (response.getBody() == null) {
            log.error("Failed to find container with id {}: body is empty", containerId);
            throw new MetadataServiceException("Failed to find container with id " + containerId + ": body is empty");
        }
        final ContainerDto container = metadataMapper.containerDtoToContainerDto(response.getBody());
        container.setUsername(response.getHeaders().get("X-Username").get(0));
        container.setPassword(response.getHeaders().get("X-Password").get(0));
        container.setLastRetrieved(Instant.now());
        return container;
    }

    @Override
    public DatabaseDto getDatabaseById(Long id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final ResponseEntity<DatabaseDto> response;
        final String url = "/api/database/" + id;
        log.debug("get  database info from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, DatabaseDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find database with id {}: {}", id, e.getMessage());
            throw new RemoteUnavailableException("Failed to find database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find database with id {}: body is null", id);
            throw new DatabaseNotFoundException("Failed to find database: body is null: " + e.getMessage(), e);
        }
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to find database with id {}: service responded unsuccessful: {}", id, response.getStatusCode());
            throw new MetadataServiceException("Failed to find database: service responded unsuccessful: " + response.getStatusCode());
        }
        final List<String> expectedHeaders = List.of("X-Username", "X-Password");
        if (!response.getHeaders().keySet().containsAll(expectedHeaders)) {
            log.error("Failed to find all  database headers");
            log.debug("expected headers: {}", expectedHeaders);
            log.debug("found headers: {}", response.getHeaders().keySet());
            throw new MetadataServiceException("Failed to find all  database headers");
        }
        if (response.getBody() == null) {
            log.error("Failed to find database with id {}: body is empty", id);
            throw new MetadataServiceException("Failed to find database with id " + id + ": body is empty");
        }
        final DatabaseDto database = response.getBody();
        database.getContainer().setUsername(response.getHeaders().get("X-Username").get(0));
        database.getContainer().setPassword(response.getHeaders().get("X-Password").get(0));
        database.setLastRetrieved(Instant.now());
        return database;
    }

    @Override
    public TableDto getTableById(Long databaseId, Long id) throws TableNotFoundException,
            RemoteUnavailableException, MetadataServiceException {
        final ResponseEntity<TableDto> response;
        final String url = "/api/database/" + databaseId + "/table/" + id;
        log.debug("get  table info from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, TableDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find table with id {}: {}", id, e.getMessage());
            throw new RemoteUnavailableException("Failed to find table: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find table with id {}: not found: {}", id, e.getMessage());
            throw new TableNotFoundException("Failed to find table: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find table with id {}: service responded unsuccessful: {}", id, response.getStatusCode());
            throw new MetadataServiceException("Failed to find table: service responded unsuccessful: " + response.getStatusCode());
        }
        final List<String> expectedHeaders = List.of("X-Username", "X-Password");
        if (!response.getHeaders().keySet().containsAll(expectedHeaders)) {
            log.error("Failed to find all  table headers");
            log.debug("expected headers: {}", expectedHeaders);
            log.debug("found headers: {}", response.getHeaders().keySet());
            throw new MetadataServiceException("Failed to find all  table headers");
        }
        if (response.getBody() == null) {
            log.error("Failed to find table with id {}: body is empty", id);
            throw new MetadataServiceException("Failed to find table with id " + id + ": body is empty");
        }
        final TableDto table = metadataMapper.tableDtoToTableDto(response.getBody());
        table.getDatabase().getContainer().setUsername(response.getHeaders().get("X-Username").get(0));
        table.getDatabase().getContainer().setPassword(response.getHeaders().get("X-Password").get(0));
        table.setLastRetrieved(Instant.now());
        return table;
    }

    @Override
    public ViewDto getViewById(Long databaseId, Long id) throws RemoteUnavailableException,
            ViewNotFoundException, MetadataServiceException {
        final ResponseEntity<ViewDto> response;
        final String url = "/api/database/" + databaseId + "/view/" + id;
        log.debug("get  view info from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, ViewDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find view with id {}: {}", id, e.getMessage());
            throw new RemoteUnavailableException("Failed to find view: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find view with id {}: not found: {}", id, e.getMessage());
            throw new ViewNotFoundException("Failed to find view: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find view with id {}: service responded unsuccessful: {}", id, response.getStatusCode());
            throw new MetadataServiceException("Failed to find view: service responded unsuccessful: " + response.getStatusCode());
        }
        final List<String> expectedHeaders = List.of("X-Username", "X-Password");
        if (!response.getHeaders().keySet().containsAll(expectedHeaders)) {
            log.error("Failed to find all  view headers");
            log.debug("expected headers: {}", expectedHeaders);
            log.debug("found headers: {}", response.getHeaders().keySet());
            throw new MetadataServiceException("Failed to find all  view headers");
        }
        if (response.getBody() == null) {
            log.error("Failed to find view with id {}: body is empty", id);
            throw new MetadataServiceException("Failed to find view with id " + id + ": body is empty");
        }
        final ViewDto view = metadataMapper.viewDtoToViewDto(response.getBody());
        view.getDatabase().getContainer().setUsername(response.getHeaders().get("X-Username").get(0));
        view.getDatabase().getContainer().setPassword(response.getHeaders().get("X-Password").get(0));
        view.setLastRetrieved(Instant.now());
        return view;
    }

    @Override
    public UserDto getUserById(UUID userId) throws RemoteUnavailableException, UserNotFoundException,
            MetadataServiceException {
        final ResponseEntity<UserDto> response;
        final String url = "/api/user/" + userId;
        log.debug("get  user info from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, UserDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find user with id {}: {}", userId, e.getMessage());
            throw new RemoteUnavailableException("Failed to find user: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find user with id {}: not found: {}", userId, e.getMessage());
            throw new UserNotFoundException("Failed to find user: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find user with id {}: service responded unsuccessful: {}", userId, response.getStatusCode());
            throw new MetadataServiceException("Failed to find user: service responded unsuccessful: " + response.getStatusCode());
        }
        final List<String> expectedHeaders = List.of("X-Username", "X-Password");
        if (!response.getHeaders().keySet().containsAll(expectedHeaders)) {
            log.error("Failed to find all  user headers");
            log.debug("expected headers: {}", expectedHeaders);
            log.debug("found headers: {}", response.getHeaders().keySet());
            throw new MetadataServiceException("Failed to find all  user headers");
        }
        if (response.getBody() == null) {
            log.error("Failed to find user with id {}: body is empty", userId);
            throw new MetadataServiceException("Failed to find user with id " + userId + ": body is empty");
        }
        final UserDto user = metadataMapper.userDtoToUserDto(response.getBody());
        user.setUsername(response.getHeaders().get("X-Username").get(0));
        user.setPassword(response.getHeaders().get("X-Password").get(0));
        user.setLastRetrieved(Instant.now());
        return user;
    }

    @Override
    public DatabaseAccessDto getAccess(Long databaseId, UUID userId) throws RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ResponseEntity<DatabaseAccessDto> response;
        final String url = "/api/database/" + databaseId + "/access/" + userId;
        log.debug("get database access from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, DatabaseAccessDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find database access for user with id {}: {}", userId, e.getMessage());
            throw new RemoteUnavailableException("Failed to find database access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound e) {
            log.error("Failed to find database access for user with id {}: foreign user: {}", userId, e.getMessage());
            throw new NotAllowedException("Failed to find database access: foreign user: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find database access for user with id {}: service responded unsuccessful: {}", userId, response.getStatusCode());
            throw new MetadataServiceException("Failed to find database access: service responded unsuccessful: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to find database access: body is empty");
            throw new MetadataServiceException("Failed to find database access: body is empty");
        }
        return response.getBody();
    }

    @Override
    public List<IdentifierBriefDto> getIdentifiers(@NotNull Long databaseId, Long subsetId) throws MetadataServiceException,
            RemoteUnavailableException, DatabaseNotFoundException {
        final ResponseEntity<IdentifierBriefDto[]> response;
        final String url = "/api/identifier?dbid=" + databaseId + (subsetId != null ? ("&qid=" + subsetId) : "");
        log.debug("get identifiers from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, IdentifierBriefDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find identifiers for database with id {} and subset with id {}: {}", databaseId, subsetId, e.getMessage());
            throw new RemoteUnavailableException("Failed to find identifiers: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find identifiers for database with id {} and subset with id {}: foreign user: {}", databaseId, subsetId, e.getMessage());
            throw new DatabaseNotFoundException("Failed to find identifiers: foreign user: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find identifiers for database with id {} and subset with id {}: service responded unsuccessful: {}", databaseId, subsetId, response.getStatusCode());
            throw new MetadataServiceException("Failed to find identifiers for database: service responded unsuccessful: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to find identifiers: body is null");
            throw new MetadataServiceException("Failed to find identifiers: body is null");
        }
        return List.of(response.getBody());
    }

    @Override
    public void updateTableStatistics(Long databaseId, Long tableId, String authorization) throws TableNotFoundException,
            MetadataServiceException, RemoteUnavailableException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/table/" + tableId + "/statistic";
        log.debug("update table statistics in metadata service: {}", url);
        internalRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(gatewayConfig.getMetadataEndpoint()));
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(null, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to update table statistic for table with id {}: {}", tableId, e.getMessage());
            throw new RemoteUnavailableException("Failed to update table statistic: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update table statistic for table with id {}: foreign user: {}", tableId, e.getMessage());
            throw new TableNotFoundException("Failed to update table statistic: foreign user: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update table statistic for table with id {}: service responded unsuccessful: {}", tableId, response.getStatusCode());
            throw new MetadataServiceException("Failed to update table statistic for database: service responded unsuccessful: " + response.getStatusCode());
        }
    }

}
