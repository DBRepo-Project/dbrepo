package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintAddDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DataServiceGatewayImpl implements DataServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;
    private final ObjectMapper objectMapper;

    public DataServiceGatewayImpl(@Qualifier("dataServiceRestTemplate") RestTemplate restTemplate,
                                  GatewayConfig gatewayConfig,
                                  ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public void createAccess(UUID databaseId, String username, AccessTypeDto access)
            throws DataServiceConnectionException, DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId + "/access/" + username;
        log.trace("create access at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.POST,
                    new HttpEntity<>(CreateAccessDto.builder().type(access).build()), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to create access: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to create access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to create access: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to create access: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create access: {}", e.getMessage());
            throw new DataServiceException("Failed to create access: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create access: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to create access: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void updateAccess(UUID databaseId, String username, AccessTypeDto access)
            throws DataServiceConnectionException, DataServiceException, AccessNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId + "/access/" + username;
        log.trace("update access at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.PUT,
                    new HttpEntity<>(CreateAccessDto.builder().type(access).build()), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to update access: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to update access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update access: not found: {}", e.getMessage());
            throw new AccessNotFoundException("Failed to update access: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update access: {}", e.getMessage());
            throw new DataServiceException("Failed to update access: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update access: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to update access: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void deleteAccess(UUID databaseId, String username) throws DataServiceConnectionException, DataServiceException,
            AccessNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId + "/access/" + username;
        log.trace("delete access at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to delete access: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to delete access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete access: not found: {}", e.getMessage());
            throw new AccessNotFoundException("Failed to delete access: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete access: {}", e.getMessage());
            throw new DataServiceException("Failed to delete access: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete access: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to delete access: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void createDatabase(CreateDatabaseDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<DatabaseDto> response;
        final String path = "/api/v1/database";
        log.trace("create database at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(data), DatabaseDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to create database: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to create database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create database: malformed: {}", e.getMessage());
            throw new DataServiceException("Failed to create database: malformed: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to create database: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to create database: not found: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create database: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to create database: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void updateDatabase(UUID databaseId, UpdateUserPasswordDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId;
        log.trace("update database at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to update user password in database: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to update user password in database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update user password in database: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to update user password in database: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update user password in database: {}", e.getMessage());
            throw new DataServiceException("Failed to update user password in database: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update user password in database: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to update user password in database: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void updateTable(UUID databaseId, UUID tableId, TableUpdateDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId + "/table/" + tableId;
        log.trace("update table at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to update table in database: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to update table in database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update table in database: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to update table in database: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update table in database: {}", e.getMessage());
            throw new DataServiceException("Failed to update table in database: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update table in database: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to update table in database: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void createTable(UUID databaseId, CreateTableDto data) throws DataServiceConnectionException, DataServiceException,
            DatabaseNotFoundException, TableExistsException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId + "/table";
        log.trace("create table at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(data), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to create table: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to create table: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to create table: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to create table: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Conflict e) {
            log.error("Failed to create table: already exists: {}", e.getMessage());
            throw new TableExistsException("Failed to create table: already exists", e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create table: {}", e.getMessage());
            throw new DataServiceException("Failed to create table: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create table: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to create table: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public TableDto getTableSchema(UUID databaseId, UUID tableId) throws DataServiceConnectionException,
            DataServiceException, TableNotFoundException {
        final ResponseEntity<TableDto> response;
        final String path = "/api/v1/database/" + databaseId + "/table/" + tableId + "/schema";
        log.trace("get table schema at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, TableDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to get table schema: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to get table schema: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to get table schema: not found: {}", e.getMessage());
            throw new TableNotFoundException("Failed to get table schema: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to get table schema: {}", e.getMessage());
            throw new DataServiceException("Failed to get table schema: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to get table schema: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to get table schema: wrong http code: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to get table schema: empty body: {}", response.getStatusCode());
            throw new DataServiceException("Failed to get table schema: empty body: " + response.getStatusCode());
        }
        return response.getBody();
    }

    @Override
    public TableDto addTableConstraint(UUID databaseId, UUID tableId, ConstraintAddDto data)
            throws DataServiceConnectionException, DataServiceException, TableNotFoundException, MalformedException {
        return alterTableConstraint(databaseId, tableId, HttpMethod.POST, data, "add table constraint");
    }

    @Override
    public TableDto deleteTableConstraint(UUID databaseId, UUID tableId, ConstraintDeleteDto data)
            throws DataServiceConnectionException, DataServiceException, TableNotFoundException, MalformedException {
        return alterTableConstraint(databaseId, tableId, HttpMethod.DELETE, data, "delete table constraint");
    }

    private TableDto alterTableConstraint(UUID databaseId, UUID tableId, HttpMethod method, Object data,
                                          String action)
            throws DataServiceConnectionException, DataServiceException, TableNotFoundException, MalformedException {
        final ResponseEntity<TableDto> response;
        final String path = "/api/v1/database/" + databaseId + "/table/" + tableId + "/constraint";
        log.trace("{} at endpoint {} with path {}", action, gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, method, new HttpEntity<>(data), TableDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to {}: {}", action, e.getMessage());
            throw new DataServiceConnectionException("Failed to " + action + ": " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to {}: not found: {}", action, e.getMessage());
            throw new TableNotFoundException("Failed to " + action + ": not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to {}: {}", action, e.getMessage());
            throw new MalformedException(dataServiceErrorMessage(e, "Failed to " + action));
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to {}: unauthorized: {}", action, e.getMessage());
            throw new DataServiceException("Failed to " + action + ": " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to {}: wrong http code: {}", action, response.getStatusCode());
            throw new DataServiceException("Failed to " + action + ": wrong http code: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to {}: empty body: {}", action, response.getStatusCode());
            throw new DataServiceException("Failed to " + action + ": empty body: " + response.getStatusCode());
        }
        return response.getBody();
    }

    private String dataServiceErrorMessage(HttpClientErrorException e, String fallback) {
        try {
            final ApiErrorDto error = objectMapper.readValue(e.getResponseBodyAsString(), ApiErrorDto.class);
            if (error.getMessage() != null && !error.getMessage().isBlank()) {
                return error.getMessage();
            }
        } catch (JsonProcessingException ignored) {
            log.debug("Failed to parse data service error response: {}", e.getResponseBodyAsString());
        }
        return fallback + ": " + e.getStatusCode();
    }

    @Override
    public void deleteTable(UUID databaseId, UUID tableId) throws DataServiceConnectionException, DataServiceException,
            TableNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId + "/table/" + tableId;
        log.trace("delete table at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to delete table: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to delete table: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete table: not found: {}", e.getMessage());
            throw new TableNotFoundException("Failed to delete table: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete table: {}", e.getMessage());
            throw new DataServiceException("Failed to delete table: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete table: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to delete table: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public ViewDto createView(UUID databaseId, CreateViewDto data) throws DataServiceConnectionException,
            DataServiceException, ColumnNotFoundException {
        final ResponseEntity<ViewDto> response;
        final String path = "/api/v1/database/" + databaseId + "/view";
        log.trace("create view at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(data), ViewDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to create view: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new DataServiceException("Failed to create view: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to create view: not found: {}", e.getMessage());
            throw new ColumnNotFoundException("Failed to create view: not found: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create view: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to create view: wrong http code: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to create view: empty body: {}", response.getStatusCode());
            throw new DataServiceException("Failed to create view: empty body: " + response.getStatusCode());
        }
        return response.getBody();
    }

    @Override
    public void deleteView(UUID databaseId, UUID viewId) throws DataServiceConnectionException, DataServiceException,
            ViewNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/v1/database/" + databaseId + "/view/" + viewId;
        log.trace("delete view at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to delete view: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to delete view: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete view: not found: {}", e.getMessage());
            throw new ViewNotFoundException("Failed to delete view: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete view: {}", e.getMessage());
            throw new DataServiceException("Failed to delete view: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete view: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to delete view: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public QueryDto findQuery(UUID databaseId, UUID queryId) throws DataServiceConnectionException, DataServiceException,
            QueryNotFoundException {
        final ResponseEntity<QueryDto> response;
        final String path = "/api/v1/database/" + databaseId + "/subset/" + queryId;
        log.trace("find subset at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, QueryDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to find query: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to find query", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find query: not found: {}", e.getMessage());
            throw new QueryNotFoundException("Failed to find query: not found", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to find query: unauthorized: {}", e.getMessage());
            throw new DataServiceException("Failed to find query: unauthorized", e);
        } catch (HttpClientErrorException.NotAcceptable e) {
            log.error("Failed to find query: format not acccepted: {}", e.getMessage());
            throw new DataServiceException("Failed to find query: format not accepted", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find query: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to find query: wrong http code: " + response.getStatusCode());
        }
        return response.getBody();
    }

    @Override
    public List<TableDto> getTableSchemas(UUID databaseId) throws DataServiceConnectionException, DataServiceException,
            TableNotFoundException {
        final ResponseEntity<TableDto[]> response;
        final String path = "/api/v1/database/" + databaseId + "/table";
        log.trace("get table schemas at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, TableDto[].class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to get table schemas: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to get table schemas: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to get table schemas: not found: {}", e.getMessage());
            throw new TableNotFoundException("Failed to get table schemas: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to get table schemas: {}", e.getMessage());
            throw new DataServiceException("Failed to get table schemas: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to get table schemas: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to get table schemas: wrong http code: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to get table schemas: empty body: {}", response.getStatusCode());
            throw new DataServiceException("Failed to get table schemas: empty body: " + response.getStatusCode());
        }
        final List<TableDto> tables = Arrays.asList(response.getBody());
        log.debug("found {} table(s) in data service", tables.size());
        return tables;
    }

    @Override
    public List<ViewDto> getViewSchemas(UUID databaseId) throws DataServiceConnectionException, DataServiceException,
            ViewNotFoundException {
        final ResponseEntity<ViewDto[]> response;
        final String path = "/api/v1/database/" + databaseId + "/view";
        log.trace("get view schemas at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, ViewDto[].class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to get view schemas: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to get view schemas: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to get view schemas: not found: {}", e.getMessage());
            throw new ViewNotFoundException("Failed to get view schemas: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to get view schemas: {}", e.getMessage());
            throw new DataServiceException("Failed to get view schemas: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to get view schemas: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to get view schemas: wrong http code: " + response.getStatusCode());
        }
        if (response.getBody() == null) {
            log.error("Failed to get view schemas: empty body: {}", response.getStatusCode());
            throw new DataServiceException("Failed to get view schemas: empty body: " + response.getStatusCode());
        }
        final List<ViewDto> views = Arrays.asList(response.getBody());
        log.debug("found {} view(s) in data service", views.size());
        return views;
    }

    @Override
    public TableStatisticDto getTableStatistics(UUID databaseId, UUID tableId) throws DataServiceConnectionException,
            DataServiceException, TableNotFoundException {
        final ResponseEntity<TableStatisticDto> response;
        final String path = "/api/v1/database/" + databaseId + "/table/" + tableId + "/statistic";
        log.trace("get table statistics at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, TableStatisticDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to analyse table statistic: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to analyse table statistic: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to analyse table statistic: not found: {}", e.getMessage());
            throw new TableNotFoundException("Failed to analyse table statistic: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to analyse table statistic: {}", e.getMessage());
            throw new DataServiceException("Failed to analyse table statistic: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to analyse table statistic: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to analyse table statistic: wrong http code: " + response.getStatusCode());
        }
        return response.getBody();
    }

}
