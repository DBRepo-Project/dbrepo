package at.tuwien.gateway.impl;

import at.tuwien.api.document.file.FileStartDto;
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
        final String url = "/api/records";
        final ResponseEntity<DraftDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(data, headers(token)), DraftDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to create draft record");
            throw new DraftRecordCreateException("Failed to create draft record", e);
        }
        return response.getBody();
    }

    @Override
    public DraftDto reserveDraftDoi(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records/" + id + "/draft/pids/doi";
        final ResponseEntity<DraftDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(null, headers(token)), DraftDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to reserve draft doi");
            throw new DraftRecordCreateException("Failed to reserve draft doi", e);
        }
        return response.getBody();
    }

    @Override
    public DraftDto findDraft(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records/" + id + "/draft";
        final ResponseEntity<DraftDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(null, headers(token)), DraftDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to find draft record");
            throw new DraftRecordCreateException("Failed to create find record", e);
        }
        return response.getBody();
    }

    @Override
    public FileStartDto startUpload(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records/" + id + "/draft/files";
        final ResponseEntity<FileStartDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(null, headers(token)), FileStartDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to start draft files");
            throw new DraftRecordCreateException("Failed to start draft files", e);
        }
        return response.getBody();
    }

    @Override
    public void delete(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records" + id + "/draft";
        try {
            restTemplate.exchange(url, HttpMethod.DELETE,
                    new HttpEntity<>(null, headers(token)), Void.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to delete draft record");
            throw new DraftRecordCreateException("Failed to delete find record", e);
        }
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
        headers.add("Content-Type", "application/json");
        return headers;
    }
}
