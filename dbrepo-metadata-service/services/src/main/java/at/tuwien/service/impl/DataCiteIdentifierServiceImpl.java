package at.tuwien.service.impl;

import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.DataCiteData;
import at.tuwien.api.datacite.doi.DataCiteCreateDoi;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.api.datacite.doi.DataCiteDoiEvent;
import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.CreateIdentifierDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.config.DataCiteConfig;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierStatusType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.repository.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Primary
@Profile("doi")
@Service
public class DataCiteIdentifierServiceImpl implements IdentifierService {

    private final RestTemplate restTemplate;
    private final DataCiteConfig dataCiteConfig;
    private final MetadataMapper metadataMapper;
    private final EndpointConfig endpointConfig;
    private final IdentifierService identifierService;
    private final IdentifierRepository identifierRepository;

    private final ParameterizedTypeReference<DataCiteBody<DataCiteDoi>> dataCiteBodyParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    public DataCiteIdentifierServiceImpl(@Qualifier("dataCiteRestTemplate") RestTemplate restTemplate,
                                         DataCiteConfig dataCiteConfig, MetadataMapper metadataMapper,
                                         EndpointConfig endpointConfig, IdentifierServiceImpl identifierService,
                                         IdentifierRepository identifierRepository) {
        this.restTemplate = restTemplate;
        this.dataCiteConfig = dataCiteConfig;
        this.metadataMapper = metadataMapper;
        this.endpointConfig = endpointConfig;
        this.identifierService = identifierService;
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(IdentifierTypeDto type, Long databaseId, Long queryId, Long viewId, Long tableId) {
        return identifierService.findAll(type, databaseId, queryId, viewId, tableId);
    }

    @Override
    @Transactional
    public Identifier publish(Identifier identifier) throws MalformedException, DataServiceConnectionException,
            ExternalServiceException {
        identifier.setStatus(IdentifierStatusType.PUBLISHED);
        identifier.setDoi(remoteSave(identifier, DataCiteDoiEvent.PUBLISH));
        return identifierRepository.save(identifier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId) {
        return identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
    }

    @Override
    public List<Identifier> findAllDatabaseIdentifiers() {
        return identifierService.findAllDatabaseIdentifiers();
    }

    @Override
    public List<Identifier> findAllSubsetIdentifiers() {
        return identifierService.findAllSubsetIdentifiers();
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Identifier save(Database database, User user, IdentifierSaveDto data) throws DataServiceException,
            DataServiceConnectionException, MalformedException, DatabaseNotFoundException, IdentifierNotFoundException,
            ViewNotFoundException, QueryNotFoundException, SearchServiceException, SearchServiceConnectionException,
            ExternalServiceException {
        data.setDoi(remoteSave(identifierService.save(database, user, data), DataCiteDoiEvent.REGISTER));
        return identifierService.save(database, user, data);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Identifier create(Database database, User user, CreateIdentifierDto data) throws DataServiceException,
            DataServiceConnectionException, IdentifierNotFoundException, MalformedException, ViewNotFoundException,
            DatabaseNotFoundException, QueryNotFoundException, SearchServiceException,
            SearchServiceConnectionException, ExternalServiceException {
        data.setDoi(remoteSave(identifierService.create(database, user, data), DataCiteDoiEvent.REGISTER));
        return identifierService.create(database, user, data);
    }

    /**
     * Saves the PID remotely in DataCite Fabrica
     *
     * @param identifier The identifier information
     * @param event      The PID status event, e.g. publish
     * @return The DOI for this PID.
     * @throws MalformedException
     * @throws DataServiceConnectionException
     * @throws ExternalServiceException
     */
    public String remoteSave(Identifier identifier, DataCiteDoiEvent event) throws MalformedException,
            DataServiceConnectionException, ExternalServiceException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(dataCiteConfig.getUsername(), dataCiteConfig.getPassword());
        final HttpEntity<DataCiteBody<DataCiteCreateDoi>> request = new HttpEntity<>(
                DataCiteBody.<DataCiteCreateDoi>builder()
                        .data(DataCiteData.<DataCiteCreateDoi>builder()
                                .type("dois")
                                .attributes(metadataMapper.identifierToDataCiteCreateDoi(identifier,
                                        endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId(),
                                        dataCiteConfig.getPrefix(), event))
                                .build())
                        .build(),
                headers
        );
        final String url = dataCiteConfig.getUrl() + "/dois";
        log.trace("request doi from url {}", url);
        try {
            final ResponseEntity<DataCiteBody<DataCiteDoi>> response = restTemplate.exchange(url, HttpMethod.POST,
                    request, dataCiteBodyParameterizedTypeReference);
            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null) {
                log.error("Failed to mint doi: {}", response);
                throw new ExternalServiceException("Failed to mint doi: " + response.getBody());
            }
            return response.getBody()
                    .getData()
                    .getAttributes()
                    .getDoi();
        } catch (HttpClientErrorException e) {
            log.error("Failed to mint doi: malformed metadata: {}", e.getMessage());
            throw new MalformedException("Failed to mint doi: malformed metadata: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("Failed to mint doi: {}", e.getMessage());
            throw new DataServiceConnectionException("Failed to mint doi: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Identifier> findAll() {
        return identifierService.findAll();
    }

    @Override
    public List<Identifier> findAll(Long databaseId) {
        return identifierService.findAll(databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(Long identifierId) throws IdentifierNotFoundException {
        return identifierService.find(identifierId);
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier findByDoi(String doi) throws IdentifierNotFoundException {
        return identifierService.findByDoi(doi);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportMetadata(Identifier identifier) {
        return identifierService.exportMetadata(identifier);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportBibliography(Identifier identifier, BibliographyTypeDto style) throws MalformedException {
        return identifierService.exportBibliography(identifier, style);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportResource(Identifier identifier) throws DataServiceException,
            DataServiceConnectionException, IdentifierNotFoundException, QueryNotFoundException {
        return identifierService.exportResource(identifier);
    }

    @Override
    @Transactional
    public void delete(Identifier identifier) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, IdentifierNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        identifierService.delete(identifier);
    }

}
