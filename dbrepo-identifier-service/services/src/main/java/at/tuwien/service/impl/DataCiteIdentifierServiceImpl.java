package at.tuwien.service.impl;

import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.DataCiteConfig;
import at.tuwien.config.EndpointConfig;
import at.tuwien.datacite.DataCiteBody;
import at.tuwien.datacite.DataCiteData;
import at.tuwien.datacite.doi.DataCiteCreateDoi;
import at.tuwien.datacite.doi.DataCiteDoi;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.mapper.DataCiteMapper;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.elastic.IdentifierIdxRepository;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.repository.jpa.RelatedIdentifierRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.thymeleaf.TemplateEngine;

import javax.transaction.Transactional;
import java.security.Principal;
import java.util.Optional;

@Slf4j
@Profile("doi")
@Service
public class DataCiteIdentifierServiceImpl extends IdentifierServiceImpl {

    private final DataCiteConfig dataCiteConfig;
    private final EndpointConfig endpointConfig;
    private final DataCiteMapper dataCiteMapper;
    private final RestTemplateBuilder restTemplateBuilder;
    private final IdentifierRepository identifierRepository;

    public DataCiteIdentifierServiceImpl(DataCiteConfig dataCiteConfig, DataCiteMapper dataCiteMapper,
                                         RestTemplateBuilder restTemplateBuilder,
                                         UserService userService, EndpointConfig endpointConfig,
                                         TemplateEngine templateEngine, DatabaseService databaseService,
                                         IdentifierMapper identifierMapper, QueryServiceGateway queryServiceGateway,
                                         IdentifierRepository identifierRepository,
                                         IdentifierIdxRepository identifierIdxRepository,
                                         RelatedIdentifierRepository relatedIdentifierRepository) {
        super(userService, endpointConfig, templateEngine, databaseService, identifierMapper, queryServiceGateway,
                identifierRepository, identifierIdxRepository, relatedIdentifierRepository);
        this.dataCiteConfig = dataCiteConfig;
        this.dataCiteMapper = dataCiteMapper;
        this.restTemplateBuilder =
                restTemplateBuilder.basicAuthentication(dataCiteConfig.getUsername(), dataCiteConfig.getPassword())
                        .uriTemplateHandler(new DefaultUriBuilderFactory(dataCiteConfig.getUrl()));
        this.endpointConfig = endpointConfig;
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional
    public Identifier create(IdentifierCreateDto data, Principal principal, String authorization)
            throws IdentifierPublishingNotAllowedException, QueryNotFoundException, RemoteUnavailableException,
            IdentifierAlreadyExistsException, UserNotFoundException, DatabaseNotFoundException,
            IdentifierRequestException {
        Identifier identifier = super.create(data, principal, authorization);
        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
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

        try {
            ResponseEntity<DataCiteBody<DataCiteDoi>> response = restTemplate.exchange("dois", HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    }
            );

            if(response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null) {
                log.error("Could not successfully create DOI. Response: {}", response);
                throw new IdentifierRequestException("Could not successfully create DOI.");
            }

            identifier.setDoi(response.getBody().getData().getAttributes().getDoi());
            this.identifierRepository.save(identifier);
        } catch(HttpClientErrorException e) {
            throw new IdentifierRequestException("Invalid DOI metadata.", e);
        } catch(RestClientException e) {
            throw new InternalError("Could not fulfil request to DataCite server.", e);
        }

        return identifier;
    }

    @Override
    public Identifier update(Long identifierId, IdentifierDto data)
            throws IdentifierNotFoundException, IdentifierRequestException {
        Identifier identifier = super.update(identifierId, data);
        if(identifier.getDoi() == null) {
            return identifier;
        }

        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
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

        try {
            ResponseEntity<DataCiteBody<DataCiteDoi>> response = restTemplate.exchange("dois/{doi}", HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<>() {
                    },
                    identifier.getDoi()
            );

            if(response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("Could not successfully create DOI. Response: {}", response);
                throw new IdentifierRequestException("Could not successfully create DOI.");
            }

            identifier.setDoi(response.getBody().getData().getAttributes().getDoi());
            this.identifierRepository.save(identifier);
        } catch(HttpClientErrorException e) {
            throw new IdentifierRequestException("Invalid DOI metadata.", e);
        } catch(RestClientException e) {
            throw new InternalError("Could not fulfil request to DataCite server.", e);
        }

        return identifier;
    }

}
