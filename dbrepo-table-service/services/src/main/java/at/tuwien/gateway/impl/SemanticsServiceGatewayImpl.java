package at.tuwien.gateway.impl;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.exception.SemanticEntityPersistException;
import at.tuwien.gateway.SemanticsServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SemanticsServiceGatewayImpl implements SemanticsServiceGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public SemanticsServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ConceptDto saveConcept(ConceptSaveDto data, String authorization) throws SemanticEntityPersistException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        final ResponseEntity<ConceptDto> response = restTemplate.exchange("/api/semantic/concept", HttpMethod.POST,
                new HttpEntity<>(data, headers), ConceptDto.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to save concept with uri {}", data.getUri());
            throw new SemanticEntityPersistException("Failed to save concept with uri " + data.getUri());
        }
        return response.getBody();
    }

    @Override
    public UnitDto saveUnit(UnitSaveDto data, String authorization) throws SemanticEntityPersistException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        final ResponseEntity<UnitDto> response = restTemplate.exchange("/api/semantic/unit", HttpMethod.POST,
                new HttpEntity<>(data, headers), UnitDto.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to save unit with uri {}", data.getUri());
            throw new SemanticEntityPersistException("Failed to save unit with uri " + data.getUri());
        }
        return response.getBody();
    }
}
