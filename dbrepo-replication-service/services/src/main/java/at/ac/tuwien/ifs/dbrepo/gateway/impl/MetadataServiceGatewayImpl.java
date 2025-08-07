package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;
import java.util.UUID;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;

@Slf4j
@Service
public class MetadataServiceGatewayImpl implements MetadataServiceGateway {

    private final DataMapper dataMapper;
    private final RestTemplate internalRestTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public MetadataServiceGatewayImpl(DataMapper dataMapper,
                                      @Qualifier("internalRestTemplate") RestTemplate internalRestTemplate,
                                      GatewayConfig gatewayConfig) {
        this.dataMapper = dataMapper;
        this.internalRestTemplate = internalRestTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public ContainerDto getContainerById(UUID containerId) throws RemoteUnavailableException,
            ContainerNotFoundException, MetadataServiceException {
        final ResponseEntity<ContainerDto> response;
        final String url = "/api/container/" + containerId;
        log.debug("get container info from metadata service: {}", url);
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
        final List<String> expectedHeaders = List.of("X-Username", "X-Password", "X-Jdbc-Method", "X-Host", "X-Port");
        if (!response.getHeaders().keySet().containsAll(expectedHeaders)) {
            log.error("Failed to find all container headers");
            log.debug("expected headers: {}", expectedHeaders);
            log.debug("found headers: {}", response.getHeaders().keySet());
            throw new MetadataServiceException("Failed to find all container headers");
        }
        if (response.getBody() == null) {
            log.error("Failed to find container with id {}: body is empty", containerId);
            throw new MetadataServiceException("Failed to find container with id " + containerId + ": body is empty");
        }
        final ContainerDto container = dataMapper.containerDtoToContainerDto(response.getBody());
        container.setHost(response.getHeaders().get("X-Host").get(0));
        container.setPort(Integer.parseInt(response.getHeaders().get("X-Port").get(0)));
        container.setUsername(response.getHeaders().get("X-Username").get(0));
        container.setPassword(response.getHeaders().get("X-Password").get(0));
        container.getImage().setJdbcMethod(response.getHeaders().get("X-Jdbc-Method").get(0));
        container.setLastRetrieved(Instant.now());
        return container;
    }

    @Override
    public ImageDto getImageById(UUID imageId) throws RemoteUnavailableException,
            ImageNotFoundException, MetadataServiceException {
        final ResponseEntity<ImageDto> response;
        final String url = "/api/image/" + imageId;
        log.debug("get container info from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY,
                    ImageDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find image with id {}: {}", imageId, e.getMessage());
            throw new RemoteUnavailableException("Failed to find image: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find image with id {}: {}", imageId, e.getMessage());
            throw new ImageNotFoundException("Failed to find image: " + e.getMessage(), e);
        }
        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to find image with id {}: service responded unsuccessful: {}", imageId, response.getStatusCode());
            throw new MetadataServiceException("Failed to find image: service responded unsuccessful: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to find image with id {}: body is empty", imageId);
            throw new MetadataServiceException("Failed to find image with id " + imageId + ": body is empty");
        }
        return response.getBody();
    }

    @Override
    public DatabaseDto getDatabaseById(UUID id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final ResponseEntity<DatabaseDto> response;
        final String url = "/api/v1/database/" + id;
        log.debug("get database info from metadata service: {}", url);
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
        final List<String> expectedHeaders = List.of("X-Username", "X-Password", "X-Jdbc-Method", "X-Host", "X-Port");
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
        database.getContainer().setHost(response.getHeaders().get("X-Host").get(0));
        database.getContainer().setPort(Integer.parseInt(response.getHeaders().get("X-Port").get(0)));
        database.getContainer().setUsername(response.getHeaders().get("X-Username").get(0));
        database.getContainer().setPassword(response.getHeaders().get("X-Password").get(0));
        database.getContainer().getImage().setJdbcMethod(response.getHeaders().get("X-Jdbc-Method").get(0));
        database.setLastRetrieved(Instant.now());
        return database;
    }

    @Override
    public TableDto getTableById(UUID databaseId, UUID id) throws TableNotFoundException,
            RemoteUnavailableException, MetadataServiceException {
        final ResponseEntity<TableDto> response;
        final String url = "/api/v1/database/" + databaseId + "/table/" + id;
        log.debug("get table info from metadata service: {}", url);
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
        if (response.getBody() == null) {
            log.error("Failed to find table with id {}: body is empty", id);
            throw new MetadataServiceException("Failed to find table with id " + id + ": body is empty");
        }
        final TableDto table = dataMapper.tableDtoToTableDto(response.getBody());
        table.setLastRetrieved(Instant.now());
        return table;
    }

    @Override
    public ViewDto getViewById(UUID databaseId, UUID id) throws RemoteUnavailableException,
            ViewNotFoundException, MetadataServiceException {
        final ResponseEntity<ViewDto> response;
        final String url = "/api/v1/database/" + databaseId + "/view/" + id;
        log.debug("get view info from metadata service: {}", url);
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
        if (response.getBody() == null) {
            log.error("Failed to find view with id {}: body is empty", id);
            throw new MetadataServiceException("Failed to find view with id " + id + ": body is empty");
        }
        final ViewDto view = dataMapper.viewDtoToViewDto(response.getBody());
        view.setLastRetrieved(Instant.now());
        return view;
    }

    @Override
    public UserDto getUserByUsername(String username) throws RemoteUnavailableException, UserNotFoundException,
            MetadataServiceException {
        final ResponseEntity<UserDto> response;
        final String url = "/api/user/" + username;
        log.debug("get user info from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, UserDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find user {}: {}", username, e.getMessage());
            throw new RemoteUnavailableException("Failed to find user: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find user {}: not found: {}", username, e.getMessage());
            throw new UserNotFoundException("Failed to find user: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find user {}: service responded unsuccessful: {}", username, response.getStatusCode());
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
            log.error("Failed to find user {}: body is empty", username);
            throw new MetadataServiceException("Failed to find user " + username + ": body is empty");
        }
        final UserDto user = dataMapper.userDtoToUserDto(response.getBody());
        user.setUsername(response.getHeaders().get("X-Username").get(0));
        user.setPassword(response.getHeaders().get("X-Password").get(0));
        user.setLastRetrieved(Instant.now());
        return user;
    }

    @Override
    public DatabaseAccessDto getAccess(UUID databaseId, String username) throws RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ResponseEntity<DatabaseAccessDto> response;
        final String url = "/api/v1/database/" + databaseId + "/access/" + username;
        log.debug("get database access from metadata service: {}", url);
        try {
            response = internalRestTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, DatabaseAccessDto.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to find database access for user {}: {}", username, e.getMessage());
            throw new RemoteUnavailableException("Failed to find database access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound e) {
            log.error("Failed to find database access for user {}: foreign user: {}", username, e.getMessage());
            throw new NotAllowedException("Failed to find database access: foreign user: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find database access for user {}: service responded unsuccessful: {}", username, response.getStatusCode());
            throw new MetadataServiceException("Failed to find database access: service responded unsuccessful: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to find database access: body is empty");
            throw new MetadataServiceException("Failed to find database access: body is empty");
        }
        return response.getBody();
    }

    @Override
    public List<IdentifierBriefDto> getIdentifiers(@NotNull UUID databaseId, UUID subsetId) throws MetadataServiceException,
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
    public void updateTableStatistics(UUID databaseId, UUID tableId, String authorization) throws TableNotFoundException,
            MetadataServiceException, RemoteUnavailableException {
        final ResponseEntity<Void> response;
        final String url = "/api/v1/database/" + databaseId + "/table/" + tableId + "/statistic";
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

    @Override
    public Map<String, Object> createReplicatedDatabase(String path, DatabaseNotificationDto databaseNotificationDto) 
            throws RemoteUnavailableException, MetadataServiceException {
        final ResponseEntity<Map> response;
        log.debug("create replicated database in metadata service: {}", path);
        final HttpEntity<DatabaseNotificationDto> requestEntity = new HttpEntity<>(databaseNotificationDto);
        try {
            response = internalRestTemplate.exchange(path, HttpMethod.POST, requestEntity, Map.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to create replicated database: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to create replicated database: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            log.error("Failed to create replicated database: {}", e.getMessage());
            throw new MetadataServiceException("Failed to create replicated database: " + e.getMessage(), e);
        }
        if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to create replicated database: service responded unsuccessful: {}", response.getStatusCode());
            throw new MetadataServiceException("Failed to create replicated database: service responded unsuccessful: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to create replicated database: body is empty");
            throw new MetadataServiceException("Failed to create replicated database: body is empty");
        }
        log.info("Created replicated database successfully");
        return response.getBody();
    }

    @Override
    public Map<String, Object> createReplicatedTable(String path, UUID databaseId, CreateTableDto createTableDto) 
            throws RemoteUnavailableException, MetadataServiceException {
        final ResponseEntity<Map> response;
        log.debug("create replicated table in metadata service: {}", path);
        final HttpEntity<CreateTableDto> requestEntity = new HttpEntity<>(createTableDto);
        try {
            response = internalRestTemplate.exchange(path, HttpMethod.POST, requestEntity, Map.class);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Failed to create replicated table: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to create replicated table: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            log.error("Failed to create replicated table: {}", e.getMessage());
            throw new MetadataServiceException("Failed to create replicated table: " + e.getMessage(), e);
        }
        if (response.getStatusCode() != HttpStatus.CREATED && response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to create replicated table: service responded unsuccessful: {}", response.getStatusCode());
            throw new MetadataServiceException("Failed to create replicated table: service responded unsuccessful: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to create replicated table: body is empty");
            throw new MetadataServiceException("Failed to create replicated table: body is empty");
        }
        log.info("Created replicated table successfully");
        return response.getBody();
    }
}
