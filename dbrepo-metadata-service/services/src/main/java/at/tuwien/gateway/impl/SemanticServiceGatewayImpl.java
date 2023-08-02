package at.tuwien.gateway.impl;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.exception.SemanticEntityNotFoundException;
import at.tuwien.gateway.SemanticServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SemanticServiceGatewayImpl implements SemanticServiceGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public SemanticServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public EntityDto getEntity(Long ontologyId, String uri, String authorization) throws SemanticEntityNotFoundException {
        final String url = "/api/semantic/ontology/" + ontologyId + "/entity?uri=" + uri;
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        final ResponseEntity<EntityDto[]> response = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(null, headers), EntityDto[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK) || response.getBody() == null) {
            log.error("Failed to get semantic entity for uri {}", uri);
            throw new SemanticEntityNotFoundException("Failed to get concept for uri " + uri);
        }
        if (response.getBody().length != 1) {
            log.error("None or multiple entities found for uri {}", uri);
            throw new SemanticEntityNotFoundException("None or multiple entities found for uri " + uri);
        }
        return response.getBody()[0];
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
