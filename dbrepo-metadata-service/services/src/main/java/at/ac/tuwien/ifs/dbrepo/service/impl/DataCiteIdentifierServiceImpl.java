package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.DataCiteConfig;
import at.ac.tuwien.ifs.dbrepo.config.EndpointConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.DataCiteBody;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.DataCiteData;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteCreateDoi;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoi;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoiEvent;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.BibliographyTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.CreateIdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierSaveDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierStatusType;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.repository.IdentifierRepository;
import at.ac.tuwien.ifs.dbrepo.service.IdentifierService;
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
import java.util.UUID;

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

    private static final String LOG_MINT_FAILED = "Failed to mint doi";

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
    public List<Identifier> findAll(IdentifierTypeDto type, UUID databaseId, UUID queryId, UUID viewId, UUID tableId) {
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
    public List<Identifier> findByDatabaseIdAndQueryId(UUID databaseId, UUID queryId) {
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
            DataServiceConnectionException, MalformedException, ViewNotFoundException, DatabaseNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {
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
        final DataCiteCreateDoi attributes = metadataMapper.identifierToDataCiteCreateDoi(identifier,
                endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId(),
                dataCiteConfig.getPrefix(), event);
        final HttpEntity<DataCiteBody<DataCiteCreateDoi>> request = new HttpEntity<>(
                DataCiteBody.<DataCiteCreateDoi>builder()
                        .data(DataCiteData.<DataCiteCreateDoi>builder()
                                .type("dois")
                                .attributes(attributes)
                                .build())
                        .build(),
                headers
        );
        final String url = dataCiteConfig.getUrl() + "/dois";
        log.atDebug()
                .setMessage("register doi from datacite url: " + url)
                .addKeyValue("type", "dois")
                .addKeyValue("event", event)
                .addKeyValue("attributes", attributes)
                .log();
        log.trace("request doi from url {}", url);
        try {
            final ResponseEntity<DataCiteBody<DataCiteDoi>> response = restTemplate.exchange(url, HttpMethod.POST,
                    request, dataCiteBodyParameterizedTypeReference);
            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null) {
                log.error(LOG_MINT_FAILED + ": {}", response);
                throw new ExternalServiceException(LOG_MINT_FAILED + ": " + response.getBody());
            }
            final String doi = response.getBody()
                    .getData()
                    .getAttributes()
                    .getDoi();
            log.atInfo()
                    .setMessage("Saved doi: " + doi)
                    .addKeyValue("doi", doi)
                    .log();
            return doi;
        } catch (HttpClientErrorException e) {
            log.atError()
                    .setMessage(LOG_MINT_FAILED)
                    .setCause(e)
                    .log();
            throw new MalformedException(LOG_MINT_FAILED + ": " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.atError()
                    .setMessage(LOG_MINT_FAILED)
                    .setCause(e)
                    .log();
            throw new DataServiceConnectionException(LOG_MINT_FAILED + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Identifier> findAll() {
        return identifierService.findAll();
    }

    @Override
    public List<Identifier> findAll(UUID databaseId) {
        return identifierService.findAll(databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(UUID identifierId) throws IdentifierNotFoundException {
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
    @Transactional
    public void delete(Identifier identifier) throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        identifierService.delete(identifier);
    }

}
