package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.DataCiteConfig;
import at.ac.tuwien.ifs.dbrepo.config.EndpointConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.DataCiteBody;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.DataCiteData;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteCreateDoi;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoi;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoiEvent;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.exception.ExternalServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataCiteGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Profile("doi")
public class DataCiteGatewayImpl implements DataCiteGateway {

    private final RestTemplate restTemplate;
    private final DataCiteConfig dataCiteConfig;
    private final EndpointConfig endpointConfig;
    private final MetadataMapper metadataMapper;

    private static final String DOIS = "dois";
    private static final String LOG_MINT_FAILED = "Failed to mint doi: ";
    private static final String LOG_TYPE = "type";
    private static final String LOG_DOI = "doi";
    private static final String LOG_ATTRIBUTES = "attributes";

    private final ParameterizedTypeReference<DataCiteBody<DataCiteDoi>> dataCiteBodyParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    @Autowired
    public DataCiteGatewayImpl(@Qualifier("dataCiteRestTemplate") RestTemplate restTemplate,
                               DataCiteConfig dataCiteConfig, EndpointConfig endpointConfig,
                               MetadataMapper metadataMapper) {
        this.restTemplate = restTemplate;
        this.dataCiteConfig = dataCiteConfig;
        this.endpointConfig = endpointConfig;
        this.metadataMapper = metadataMapper;
    }

    private HttpHeaders defaultHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/vnd.api+json"));
        return headers;
    }

    @Override
    public String create() throws MalformedException, ExternalServiceException {
        final HttpHeaders headers = defaultHeaders();
        headers.setBasicAuth(dataCiteConfig.getUsername(), dataCiteConfig.getPassword());
        final DataCiteCreateDoi attributes = DataCiteCreateDoi.builder()
                .prefix(dataCiteConfig.getPrefix())
                .build();
        final HttpEntity<DataCiteBody<DataCiteCreateDoi>> request = new HttpEntity<>(
                DataCiteBody.<DataCiteCreateDoi>builder()
                        .data(DataCiteData.<DataCiteCreateDoi>builder()
                                .type(DOIS)
                                .attributes(attributes)
                                .build())
                        .build(),
                headers
        );
        final String url = dataCiteConfig.getUrl() + "/" + DOIS;
        log.atDebug()
                .setMessage("create new doi to url: " + url)
                .addKeyValue(LOG_TYPE, DOIS)
                .addKeyValue(LOG_ATTRIBUTES, attributes)
                .log();
        try {
            final ResponseEntity<DataCiteBody<DataCiteDoi>> response = restTemplate.exchange(url, HttpMethod.POST,
                    request, dataCiteBodyParameterizedTypeReference);
            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null) {
                log.atError()
                        .setMessage(LOG_MINT_FAILED + response)
                        .addKeyValue(LOG_ATTRIBUTES, attributes)
                        .log();
                log.trace("failed payload: {}", request);
                throw new ExternalServiceException(LOG_MINT_FAILED + response.getBody());
            }
            final String doi = response.getBody()
                    .getData()
                    .getAttributes()
                    .getDoi();
            log.atInfo()
                    .setMessage("Successfully created new doi: " + doi)
                    .addKeyValue(LOG_DOI, doi)
                    .log();
            return doi;
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.atError()
                    .setMessage(LOG_MINT_FAILED + e.getMessage())
                    .addKeyValue(LOG_ATTRIBUTES, attributes)
                    .setCause(e)
                    .log();
            log.trace("failed payload: {}", request);
            throw new MalformedException(LOG_MINT_FAILED + e.getMessage(), e);
        }
    }

    @Override
    public String save(Identifier identifier, DataCiteDoiEvent event) throws MalformedException, ExternalServiceException {
        final HttpHeaders headers = defaultHeaders();
        headers.setBasicAuth(dataCiteConfig.getUsername(), dataCiteConfig.getPassword());
        final DataCiteCreateDoi attributes = metadataMapper.identifierToDataCiteCreateDoi(identifier);
        attributes.setEvent(null);
        attributes.setUrl(endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId());
        attributes.setPrefix(dataCiteConfig.getPrefix());
        final HttpEntity<DataCiteBody<DataCiteCreateDoi>> request = new HttpEntity<>(
                DataCiteBody.<DataCiteCreateDoi>builder()
                        .data(DataCiteData.<DataCiteCreateDoi>builder()
                                .id(identifier.getDoi())
                                .attributes(attributes)
                                .build())
                        .build(),
                headers
        );
        final String url = dataCiteConfig.getUrl() + "/" + DOIS + "/" + identifier.getDoi();
        log.atDebug()
                .setMessage("register doi to url: " + url)
                .addKeyValue(LOG_ATTRIBUTES, attributes)
                .log();
        try {
            final ResponseEntity<DataCiteBody<DataCiteDoi>> response = restTemplate.exchange(url, HttpMethod.PUT,
                    request, dataCiteBodyParameterizedTypeReference);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.atError()
                        .setMessage(LOG_MINT_FAILED + response)
                        .addKeyValue(LOG_ATTRIBUTES, attributes)
                        .log();
                log.trace("failed payload: {}", request);
                throw new ExternalServiceException(LOG_MINT_FAILED + response.getBody());
            }
            final String doi = response.getBody()
                    .getData()
                    .getAttributes()
                    .getDoi();
            log.atInfo()
                    .setMessage("Successfully requested doi: " + doi)
                    .addKeyValue(LOG_DOI, doi)
                    .log();
            return doi;
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            log.atError()
                    .setMessage(LOG_MINT_FAILED + e.getMessage())
                    .addKeyValue(LOG_ATTRIBUTES, attributes)
                    .setCause(e)
                    .log();
            log.trace("failed payload: {}", request);
            throw new MalformedException(LOG_MINT_FAILED + e.getMessage(), e);
        }
    }

}
