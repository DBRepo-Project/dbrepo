package at.tuwien.gateway.impl;

import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.exception.DoiNotFoundException;
import at.tuwien.gateway.CrossrefGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class CrossrefGatewayImpl implements CrossrefGateway {

    private final RestTemplate restTemplate;

    public CrossrefGatewayImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public CrossrefDto findById(String id) throws DoiNotFoundException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        final String url = "http://data.crossref.org/fundingdata/funder/" + id;
        final ResponseEntity<CrossrefDto> response;
        try {
            log.trace("find crossref doi from url {}", url);
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), CrossrefDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to retrieve CrossRef metadata from URL {}: {}", url, e.getMessage());
            throw new DoiNotFoundException("Failed to retrieve CrossRef metadata from URL " + url + ": " + e.getMessage());
        }
        return response.getBody();
    }
}