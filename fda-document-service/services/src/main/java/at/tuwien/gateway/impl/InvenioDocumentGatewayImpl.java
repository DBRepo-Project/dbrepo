package at.tuwien.gateway.impl;

import at.tuwien.api.document.file.FileAnnounceDto;
import at.tuwien.api.document.file.FileDto;
import at.tuwien.api.document.file.FileKeyDto;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.gateway.DocumentGateway;
import at.tuwien.mapper.DocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class InvenioDocumentGatewayImpl implements DocumentGateway {

    private final static String OCTET_STREAM = "application/octet-stream";

    private final DocumentMapper documentMapper;
    private final RestTemplate documentRestTemplate;

    @Autowired
    public InvenioDocumentGatewayImpl(DocumentMapper documentMapper, RestTemplate documentRestTemplate) {
        this.documentMapper = documentMapper;
        this.documentRestTemplate = documentRestTemplate;
    }

    @Override
    public RecordDto createDraft(CreateDraftDto data, String token) throws DraftRecordCreateException {
        log.trace("sending {}", data);
        final String url = "/api/records";
        final ResponseEntity<RecordDto> response;
        try {
            response = documentRestTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(data, headers(token)), RecordDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to create draft record");
            throw new DraftRecordCreateException("Failed to create draft record", e);
        }
        return response.getBody();
    }

    @Override
    public RecordDto reserveDraftDoi(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records/" + id + "/draft/pids/doi";
        final ResponseEntity<RecordDto> response;
        try {
            response = documentRestTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(null, headers(token)), RecordDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to reserve draft doi");
            throw new DraftRecordCreateException("Failed to reserve draft doi", e);
        }
        return response.getBody();
    }

    @Override
    public RecordDto findDraft(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records/" + id + "/draft";
        final ResponseEntity<RecordDto> response;
        try {
            response = documentRestTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(null, headers(token)), RecordDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to find draft record");
            throw new DraftRecordCreateException("Failed to create find record", e);
        }
        return response.getBody();
    }

    @Override
    public RecordDto publishDraft(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records/" + id + "/draft/actions/publish";
        final ResponseEntity<RecordDto> response;
        try {
            response = documentRestTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(null, headers(token)), RecordDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to publish draft record");
            throw new DraftRecordCreateException("Failed to publish find record", e);
        }
        return response.getBody();
    }

    @Override
    public FileDto uploadFile(String id, MultipartFile file, String token)
            throws FileUploadException {
        /* announce */
        final String url1 = "/api/records/" + id + "/draft/files";
        final List<FileKeyDto> files = List.of(documentMapper.stringToFileKeyDto(file.getName()));
        final ResponseEntity<FileAnnounceDto> response1;
        try {
            response1 = documentRestTemplate.exchange(url1, HttpMethod.POST,
                    new HttpEntity<>(files, headers(token)), FileAnnounceDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to announce draft file");
            throw new FileUploadException("Failed to announce draft file", e);
        }
        if (response1.getStatusCode() != HttpStatus.CREATED) {
            log.error("Failed to announce file upload");
            throw new FileUploadException("Failed to announce file upload");
        }
        /* upload */
        final String url2 = "/api/records/" + id + "/draft/files/" + file.getName() + "/content";
        final ResponseEntity<Void> response2;
        try {
            response2 = documentRestTemplate.exchange(url2, HttpMethod.PUT,
                    new HttpEntity<>(file.getBytes(), headers(token, OCTET_STREAM)), Void.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to upload draft file");
            throw new FileUploadException("Failed to upload draft file", e);
        } catch (IOException e) {
            log.error("Failed to get draft file bytes");
            throw new FileUploadException("Failed to get draft file bytes", e);
        }
        if (response2.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to upload file");
            throw new FileUploadException("Failed to upload file");
        }
        /* commit */
        final String url3 = "/api/records/" + id + "/draft/files/" + file.getName() + "/commit";
        final ResponseEntity<FileDto> response3;
        try {
            response3 = documentRestTemplate.exchange(url3, HttpMethod.POST,
                    new HttpEntity<>(null, headers(token)), FileDto.class);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to commit draft file");
            throw new FileUploadException("Failed to commit draft file", e);
        }
        if (response3.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to commit file");
            throw new FileUploadException("Failed to commit file");
        }
        return response3.getBody();
    }

    @Override
    public void delete(String id, String token) throws DraftRecordCreateException {
        final String url = "/api/records" + id + "/draft";
        try {
            documentRestTemplate.exchange(url, HttpMethod.DELETE,
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
        return headers(token, "application/json");
    }

    /**
     * Prepares the headers for all requests to authorize with the bearer token and content type.
     *
     * @param token       The token.
     * @param contentType The content type.
     * @return The headers.
     */
    private HttpHeaders headers(String token, String contentType) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.add("Content-Type", contentType);
        return headers;
    }
}
