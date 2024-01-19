package at.tuwien.service.impl;

import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.DataCiteData;
import at.tuwien.api.datacite.doi.DataCiteCreateDoi;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.config.DataCiteConfig;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.mapper.DataCiteMapper;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Primary
@Profile("doi")
@Service
public class DataCiteIdentifierServiceImpl implements IdentifierService {

    private final DataCiteConfig dataCiteConfig;
    private final DataCiteMapper dataCiteMapper;
    private final EndpointConfig endpointConfig;
    private final IdentifierService identifierService;
    private final RestTemplateBuilder restTemplateBuilder;
    private final IdentifierRepository identifierRepository;

    public DataCiteIdentifierServiceImpl(DataCiteConfig dataCiteConfig, DataCiteMapper dataCiteMapper,
                                         EndpointConfig endpointConfig, IdentifierRepository identifierRepository,
                                         RestTemplateBuilder restTemplateBuilder, IdentifierServiceImpl identifierService) {
        this.dataCiteConfig = dataCiteConfig;
        this.dataCiteMapper = dataCiteMapper;
        this.endpointConfig = endpointConfig;
        this.identifierService = identifierService;
        this.restTemplateBuilder = restTemplateBuilder.basicAuthentication(dataCiteConfig.getUsername(),
                        dataCiteConfig.getPassword())
                .uriTemplateHandler(new DefaultUriBuilderFactory(dataCiteConfig.getUrl()));
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(IdentifierTypeDto type, Long databaseId, Long queryId, Long viewId, Long tableId) {
        return identifierService.findAll(type, databaseId, queryId, viewId, tableId);
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
    public Identifier create(IdentifierSaveDto data, Principal principal) throws QueryNotFoundException,
            IdentifierRequestException, RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException,
            ViewNotFoundException, QueryStoreException, ImageNotSupportedException {
        final Identifier identifier = identifierService.create(data, principal);
        /* https://stackoverflow.com/questions/55090541/spring-data-jpa-lombok-unsupportedoperationexception-during-saving */
        if (identifier.getCreators() != null) {
            identifier.setCreators(new LinkedList<>(identifier.getCreators()));
        }
        if (identifier.getTitles() != null) {
            identifier.setTitles(new LinkedList<>(identifier.getTitles()));
        }
        if (identifier.getDescriptions() != null) {
            identifier.setDescriptions(new LinkedList<>(identifier.getDescriptions()));
        }
        if (identifier.getFunders() != null) {
            identifier.setFunders(new LinkedList<>(identifier.getFunders()));
        }
        if (identifier.getLicenses() != null) {
            identifier.setLicenses(new LinkedList<>(identifier.getLicenses()));
        }
        if (identifier.getRelatedIdentifiers() != null) {
            identifier.setRelatedIdentifiers(new LinkedList<>(identifier.getRelatedIdentifiers()));
        }
        /* end fix */
        final RestTemplate restTemplate = restTemplateBuilder.build();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(dataCiteConfig.getUsername(), dataCiteConfig.getPassword());
        HttpEntity<DataCiteBody<DataCiteCreateDoi>> request = new HttpEntity<>(
                DataCiteBody.<DataCiteCreateDoi>builder()
                        .data(DataCiteData.<DataCiteCreateDoi>builder()
                                .type("dois")
                                .attributes(dataCiteMapper.identifierToDataCiteCreateDoi(identifier,
                                        endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId(),
                                        dataCiteConfig.getPrefix()))
                                .build())
                        .build(),
                headers
        );
        final String url = dataCiteConfig.getUrl() + "/dois";
        log.debug("request doi from url {}", url);
        try {
            ResponseEntity<DataCiteBody<DataCiteDoi>> response = restTemplate.exchange(url, HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    }
            );

            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null) {
                log.error("Could not successfully create DOI. Response: {}", response);
                throw new IdentifierRequestException("Could not successfully create DOI.");
            }

            identifier.setDoi(response.getBody().getData().getAttributes().getDoi());
            this.identifierRepository.save(identifier);
        } catch (HttpClientErrorException e) {
            log.error("Invalid DOI metadata.", e);
            throw new IdentifierRequestException("Invalid DOI metadata.", e);
        } catch (RestClientException e) {
            log.error("Could not fulfil request to DataCite server.", e);
            throw new InternalError("Could not fulfil request to DataCite server.", e);
        }

        return identifier;
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
    public InputStreamResource exportMetadata(Long id) throws IdentifierNotFoundException {
        return identifierService.exportMetadata(id);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportBibliography(Long id, BibliographyTypeDto style)
            throws IdentifierNotFoundException, IdentifierRequestException {
        return identifierService.exportBibliography(id, style);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportResource(Long identifierId, Principal principal)
            throws IdentifierNotFoundException, QueryNotFoundException, FileStorageException,
            IdentifierRequestException, QueryStoreException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, DataDbSidecarException {
        return identifierService.exportResource(identifierId, principal);
    }

    @Override
    @Transactional
    public void delete(Long identifierId) throws IdentifierNotFoundException, DatabaseNotFoundException {
        identifierService.delete(identifierId);
    }

}
