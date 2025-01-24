package at.tuwien.gateway.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.*;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.CreateTableDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.api.database.table.TableUpdateDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import lombok.extern.log4j.Log4j2;
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

@Log4j2
@Service
public class DataServiceGatewayImpl implements DataServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    public DataServiceGatewayImpl(@Qualifier("dataServiceRestTemplate") RestTemplate restTemplate,
                                  GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public void createAccess(Long databaseId, UUID userId, AccessTypeDto access)
            throws DataServiceConnectionException, DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId + "/access/" + userId;
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
    public void updateAccess(Long databaseId, UUID userId, AccessTypeDto access)
            throws DataServiceConnectionException, DataServiceException, AccessNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId + "/access/" + userId;
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
    public void deleteAccess(Long databaseId, UUID userId) throws DataServiceConnectionException, DataServiceException,
            AccessNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId + "/access/" + userId;
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
    public DatabaseDto createDatabase(CreateDatabaseDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<DatabaseDto> response;
        final String path = "/api/database";
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
        return response.getBody();
    }

    @Override
    public void updateDatabase(Long databaseId, UpdateUserPasswordDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId;
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
    public void updateTable(Long databaseId, Long tableId, TableUpdateDto data) throws DataServiceConnectionException,
            DataServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId + "/table/" + tableId;
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
    public void createTable(Long databaseId, CreateTableDto data) throws DataServiceConnectionException, DataServiceException,
            DatabaseNotFoundException, TableExistsException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId + "/table";
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
    public void deleteTable(Long databaseId, Long tableId) throws DataServiceConnectionException, DataServiceException,
            TableNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId + "/table/" + tableId;
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
    public ViewDto createView(Long databaseId, CreateViewDto data) throws DataServiceConnectionException, DataServiceException {
        final ResponseEntity<ViewDto> response;
        final String path = "/api/database/" + databaseId + "/view";
        log.trace("create view at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(data), ViewDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to create view: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new DataServiceException("Failed to create view: " + e.getMessage(), e);
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
    public void deleteView(Long databaseId, Long viewId) throws DataServiceConnectionException, DataServiceException,
            ViewNotFoundException {
        final ResponseEntity<Void> response;
        final String path = "/api/database/" + databaseId + "/view/" + viewId;
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
    public QueryDto findQuery(Long databaseId, Long queryId) throws DataServiceConnectionException, DataServiceException,
            QueryNotFoundException {
        final ResponseEntity<QueryDto> response;
        final String path = "/api/database/" + databaseId + "/subset/" + queryId;
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
    public ExportResourceDto exportQuery(Long databaseId, Long queryId) throws DataServiceConnectionException,
            DataServiceException, QueryNotFoundException {
        final ResponseEntity<ExportResourceDto> response;
        final String path = "/api/database/" + databaseId + "/subset/" + queryId;
        log.trace("export subset at endpoint {} with path {}", gatewayConfig.getDataEndpoint(), path);
        try {
            response = restTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, ExportResourceDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to export query: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to export query: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to export query: not found: {}", e.getMessage());
            throw new QueryNotFoundException("Failed to export query: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to export query: {}", e.getMessage());
            throw new DataServiceException("Failed to export query: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to export query: wrong http code: {}", response.getStatusCode());
            throw new DataServiceException("Failed to export query: wrong http code: " + response.getStatusCode());
        }
        return response.getBody();
    }

    @Override
    public List<TableDto> getTableSchemas(Long databaseId) throws DataServiceConnectionException, DataServiceException,
            TableNotFoundException {
        final ResponseEntity<TableDto[]> response;
        final String path = "/api/database/" + databaseId + "/table";
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
    public List<ViewDto> getViewSchemas(Long databaseId) throws DataServiceConnectionException, DataServiceException,
            ViewNotFoundException {
        final ResponseEntity<ViewDto[]> response;
        final String path = "/api/database/" + databaseId + "/view";
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
    public TableStatisticDto getTableStatistics(Long databaseId, Long tableId) throws DataServiceConnectionException,
            DataServiceException, TableNotFoundException {
        final ResponseEntity<TableStatisticDto> response;
        final String path = "/api/database/" + databaseId + "/table/" + tableId + "/statistic";
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
