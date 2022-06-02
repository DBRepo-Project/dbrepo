package at.tuwien.gateway.impl;

import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.gateway.QueryServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class QueryServiceGatewayImpl implements QueryServiceGateway {

    private final RestTemplate restTemplate;

    private String token;

    @Autowired
    public QueryServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Integer publish(Long containerId, Long databaseId, Long tableId, TableCsvDto data) {
        final String url = "/api/container/" + containerId + "/database/" + databaseId + "/table/" + tableId + "/data";
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        final ResponseEntity<Integer> response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(data, headers), Integer.class);
        return response.getBody();
    }

    @Override
    public void setToken(String token) {
        this.token = token;
    }

}
