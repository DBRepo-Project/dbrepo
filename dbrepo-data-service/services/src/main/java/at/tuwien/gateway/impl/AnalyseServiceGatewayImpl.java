package at.tuwien.gateway.impl;

import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.AnalyseServiceGateway;
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

@Log4j2
@Service
public class AnalyseServiceGatewayImpl implements AnalyseServiceGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public AnalyseServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public TableStatisticDto analyseTable(Long databaseId, Long tableId) throws RemoteUnavailableException,
            NotAllowedException, TableNotFoundException {
        final ResponseEntity<TableStatisticDto> response;
        final String url = "/api/analyse/database/" + databaseId + "/table/" + tableId + "/statistics";
        log.trace("mapped url: {}", url);
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), TableStatisticDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to analyse table with id {}: {}", tableId, e.getMessage());
            throw new RemoteUnavailableException("Failed to analyse table", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to analyse table with id {}: not found: {}", tableId, e.getMessage());
            throw new TableNotFoundException("Failed to analyse table: not found", e);
        }
        if (response.getBody() == null) {
            log.error("Failed to analyse table: body is null");
            throw new NotAllowedException("Failed to analyse table: body is null");
        }
        return response.getBody();
    }

}
