package at.tuwien.gateway.impl;

import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.DraftDto;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.gateway.DocumentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class InvenioDocumentGatewayImpl implements DocumentGateway {

    private RestTemplate restTemplate;

    @Autowired
    public InvenioDocumentGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public DraftDto createDraft(CreateDraftDto data, String token) throws DraftRecordCreateException {
        log.trace("sending {}", data);
        final ResponseEntity<DraftDto> response;
        try {
            response = restTemplate.exchange("/api/records", HttpMethod.POST,
                    new HttpEntity<>(data, headers(token)), DraftDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to create draft record");
            throw new DraftRecordCreateException("Failed to create draft record", e);
        }
        return response.getBody();
    }

    @Override
    public DraftDto reserveDraftDoi(String id, String token) throws DraftRecordCreateException {
        final ResponseEntity<DraftDto> response;
        try {
            response = restTemplate.exchange("/api/records/" + id + "/draft/pids/doi", HttpMethod.POST,
                    new HttpEntity<>(null, headers(token)), DraftDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to reserve draft doi");
            throw new DraftRecordCreateException("Failed to reserve draft doi", e);
        }
        return response.getBody();
    }

    @Override
    public DraftDto findDraft(String id, String token) throws DraftRecordCreateException {
        final ResponseEntity<DraftDto> response;
        try {
            response = restTemplate.exchange("/api/records" + id +"/draft", HttpMethod.GET,
                    new HttpEntity<>(null, headers(token)), DraftDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to find draft record");
            throw new DraftRecordCreateException("Failed to create find record", e);
        }
        return response.getBody();
    }

    /**
     * Prepares the headers for all requests to authorize with the bearer token.
     *
     * @param token The token.
     * @return The headers.
     */
    private HttpHeaders headers(String token) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        return headers;
    }
}
