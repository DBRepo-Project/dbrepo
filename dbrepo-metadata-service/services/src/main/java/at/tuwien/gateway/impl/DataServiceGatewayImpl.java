package at.tuwien.gateway.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.UpdateDatabaseAccessDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class DataServiceGatewayImpl implements DataServiceGateway {

    private final RestTemplate restTemplate;

    public DataServiceGatewayImpl(@Qualifier("dataServiceRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void createAccess(Long databaseId, UUID userId, AccessTypeDto access)
            throws ServiceConnectionException, ServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/access/" + userId;
        log.debug("create access in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(UpdateDatabaseAccessDto.builder().type(access).build()), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to create access: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to create access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to create access: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to create access: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create access: {}", e.getMessage());
            throw new ServiceException("Failed to create access: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create access: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to create access: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void updateAccess(Long databaseId, UUID userId, AccessTypeDto access)
            throws ServiceConnectionException, ServiceException, AccessNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/access/" + userId;
        log.debug("update access in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(UpdateDatabaseAccessDto.builder().type(access).build()), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to update access: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to update access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update access: not found: {}", e.getMessage());
            throw new AccessNotFoundException("Failed to update access: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update access: {}", e.getMessage());
            throw new ServiceException("Failed to update access: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update access: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to update access: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void deleteAccess(Long databaseId, UUID userId) throws ServiceConnectionException, ServiceException,
            AccessNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/access/" + userId;
        log.debug("delete access in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to delete access: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to delete access: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete access: not found: {}", e.getMessage());
            throw new AccessNotFoundException("Failed to delete access: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete access: {}", e.getMessage());
            throw new ServiceException("Failed to delete access: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete access: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to delete access: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public DatabaseDto createDatabase(CreateDatabaseDto data) throws ServiceConnectionException, ServiceException {
        final ResponseEntity<DatabaseDto> response;
        final String url = "/api/database";
        log.debug("create database in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data), DatabaseDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to create database: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to create database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create database: {}", e.getMessage());
            throw new ServiceException("Failed to create database: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create database: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to create database: wrong http code: " + response.getStatusCode());
        }
        return response.getBody();
    }

    @Override
    public void updateDatabase(Long databaseId, UpdateUserPasswordDto data) throws ServiceConnectionException,
            ServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId;
        log.debug("update database in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to update user password in database: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to update user password in database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update user password in database: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to update user password in database: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update user password in database: {}", e.getMessage());
            throw new ServiceException("Failed to update user password in database: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update user password in database: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to update user password in database: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void createTable(Long databaseId, TableCreateDto data) throws ServiceConnectionException, ServiceException,
            DatabaseNotFoundException, TableExistsException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/table";
        log.debug("create table in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to create table: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to create table: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to create table: not found: {}", e.getMessage());
            throw new DatabaseNotFoundException("Failed to create table: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Conflict e) {
            log.error("Failed to create table: already exists: {}", e.getMessage());
            throw new TableExistsException("Failed to create table: already exists", e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create table: {}", e.getMessage());
            throw new ServiceException("Failed to create table: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create table: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to create table: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void deleteTable(Long databaseId, Long tableId) throws ServiceConnectionException, ServiceException,
            TableNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/table/" + tableId;
        log.debug("delete table in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to delete table: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to delete table: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete table: not found: {}", e.getMessage());
            throw new TableNotFoundException("Failed to delete table: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete table: {}", e.getMessage());
            throw new ServiceException("Failed to delete table: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete table: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to delete table: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void createView(Long databaseId, ViewCreateDto data) throws ServiceConnectionException, ServiceException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/view";
        log.debug("create view in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to create view: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to create view: {}", e.getMessage());
            throw new ServiceException("Failed to create view: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create view: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to create view: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public void deleteView(Long databaseId, Long viewId) throws ServiceConnectionException, ServiceException,
            ViewNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "/api/database/" + databaseId + "/view/" + viewId;
        log.debug("delete view in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to delete view: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to delete view: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete view: not found: {}", e.getMessage());
            throw new ViewNotFoundException("Failed to delete view: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete view: {}", e.getMessage());
            throw new ServiceException("Failed to delete view: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete view: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to delete view: wrong http code: " + response.getStatusCode());
        }
    }

    @Override
    public QueryDto findQuery(Long databaseId, Long queryId) throws ServiceConnectionException, ServiceException,
            QueryNotFoundException {
        final ResponseEntity<QueryDto> response;
        final String url = "/api/database/" + databaseId + "/subset/" + queryId;
        log.debug("get query in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), QueryDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to find query: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to delete table", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find query: not found: {}", e.getMessage());
            throw new QueryNotFoundException("Failed to find query: not found", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to find query: unauthorized: {}", e.getMessage());
            throw new ServiceException("Failed to find query: unauthorized", e);
        } catch (HttpClientErrorException.NotAcceptable e) {
            log.error("Failed to find query: format not acccepted: {}", e.getMessage());
            throw new ServiceException("Failed to find query: format not accepted", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to find query: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to find query: wrong http code: " + response.getStatusCode());
        }
        return response.getBody();
    }

    @Override
    public ExportResourceDto exportQuery(Long databaseId, Long queryId) throws ServiceConnectionException,
            ServiceException, QueryNotFoundException {
        final ResponseEntity<ExportResourceDto> response;
        final String url = "/api/database/" + databaseId + "/subset/" + queryId;
        log.debug("export query in data service");
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), ExportResourceDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to export query: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to delete table: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to export query: not found: {}", e.getMessage());
            throw new QueryNotFoundException("Failed to export query: not found: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to export query: {}", e.getMessage());
            throw new ServiceException("Failed to export query: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to export query: wrong http code: {}", response.getStatusCode());
            throw new ServiceException("Failed to export query: wrong http code: " + response.getStatusCode());
        }
        return response.getBody();
    }

}
