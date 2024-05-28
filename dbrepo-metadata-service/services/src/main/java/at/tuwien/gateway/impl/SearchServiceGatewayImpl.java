package at.tuwien.gateway.impl;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.TableMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service
public class SearchServiceGatewayImpl implements SearchServiceGateway {

    private final TableMapper tableMapper;
    private final RestTemplate restTemplate;
    private final DatabaseMapper databaseMapper;

    @Autowired
    public SearchServiceGatewayImpl(TableMapper tableMapper,
                                    @Qualifier("searchServiceRestTemplate") RestTemplate restTemplate,
                                    DatabaseMapper databaseMapper) {
        this.tableMapper = tableMapper;
        this.restTemplate = restTemplate;
        this.databaseMapper = databaseMapper;
    }

    @Override
    public DatabaseDto update(Database database) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException {
        final ResponseEntity<DatabaseDto> response;
        final DatabaseDto payload = databaseMapper.databaseToDatabaseDto(database);
        payload.getTables()
                .forEach(table -> {
                    table.setIsPublic(database.getIsPublic());
                    table.getColumns()
                            .forEach(column -> {
                                column.setTable(table);
                                column.setTableId(table.getId());
                                column.setDatabaseId(payload.getId());
                                column.setIsPublic(payload.getIsPublic());
                            });
                    table.getConstraints()
                            .getUniques()
                            .forEach(uk -> {
                                uk.setTable(tableMapper.tableDtoToTableBriefDto(table));
                                uk.getTable().setDatabaseId(database.getId());
                                uk.setColumns(new LinkedList<>());
//                                uk.getColumns()
//                                        .forEach(column -> {
//                                            column.setTable(table);
//                                            column.setTableId(table.getId());
//                                            column.setDatabaseId(database.getId());
//                                            column.setIsPublic(database.getIsPublic());
//                                        });
                            });
                });
        payload.getViews()
                .stream()
                .map(ViewDto::getColumns)
                .flatMap(List::stream)
                .forEach(columns -> {
                    columns.setIsPublic(database.getIsPublic());
                    columns.setDatabaseId(database.getId());
                });
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        final String url = "/api/search/database/" + database.getId();
        log.debug("update database in search service");
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), DatabaseDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to update database: {}", e.getMessage());
            throw new SearchServiceConnectionException("Failed to update database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to update database: not found");
            throw new DatabaseNotFoundException("Failed to update database: not found", e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to update database: malformed payload: {}", e.getMessage());
            throw new SearchServiceException("Failed to update database: malformed payload", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update database: response code is not 202");
            throw new SearchServiceException("Failed to update database: response code is not 202");
        }
        return response.getBody();
    }

    @Override
    public void delete(Long databaseId) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException {
        final ResponseEntity<Void> response;
        final String url = "/api/search/database/" + databaseId;
        log.trace("delete to url {}", url);
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.InternalServerError e) {
            log.error("Failed to delete database: {}", e.getMessage());
            throw new SearchServiceConnectionException("Failed to delete database: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete database: not found");
            throw new DatabaseNotFoundException("Failed to delete database: not found", e);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
            log.error("Failed to delete database: body is null");
            throw new SearchServiceException("Failed to delete database: body is null", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to delete database: response code is not 202");
            throw new SearchServiceException("Failed to delete database: response code is not 202");
        }
    }
}
