package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.*;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.elastic.IdentifieridxRepository;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.repository.jpa.RelatedIdentifierRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final UserService userService;
    private final EndpointConfig endpointConfig;
    private final TemplateEngine templateEngine;
    private final DatabaseService databaseService;
    private final IdentifierMapper identifierMapper;
    private final QueryServiceGateway queryServiceGateway;
    private final IdentifierRepository identifierRepository;
    private final IdentifieridxRepository identifieridxRepository;
    private final RelatedIdentifierRepository relatedIdentifierRepository;

    public IdentifierServiceImpl(UserService userService, EndpointConfig endpointConfig, TemplateEngine templateEngine,
                                 DatabaseService databaseService, IdentifierMapper identifierMapper,
                                 QueryServiceGateway queryServiceGateway, IdentifierRepository identifierRepository,
                                 IdentifieridxRepository identifieridxRepository,
                                 RelatedIdentifierRepository relatedIdentifierRepository) {
        this.userService = userService;
        this.endpointConfig = endpointConfig;
        this.templateEngine = templateEngine;
        this.databaseService = databaseService;
        this.identifierMapper = identifierMapper;
        this.queryServiceGateway = queryServiceGateway;
        this.identifierRepository = identifierRepository;
        this.identifieridxRepository = identifieridxRepository;
        this.relatedIdentifierRepository = relatedIdentifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(Long databaseId, Long queryId) {
        if (databaseId != null && queryId != null) {
            return identifierRepository.findByDatabaseIdAndQueryId(databaseId, queryId);
        } else if (databaseId == null && queryId != null) {
            return identifierRepository.findByQueryId(queryId);
        } else if (databaseId != null && queryId == null) {
            return identifierRepository.findByDatabaseId(databaseId);
        }
        return identifierRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(Long containerId, Long databaseId, Long queryId) throws IdentifierNotFoundException {
        final List<Identifier> identifier = identifierRepository.findByDatabaseIdAndQueryId(databaseId, queryId);
        if (identifier.isEmpty()) {
            log.error("Failed to find identifier with query id {}", queryId);
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        return identifier.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll() {
        return identifierRepository.findAll();
    }

    @Override
    @Transactional
    public Identifier create(IdentifierCreateDto data, Principal principal, String authorization)
            throws QueryNotFoundException, RemoteUnavailableException, IdentifierAlreadyExistsException,
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException {
        /* check */
        final Database database = databaseService.find(data.getCid(), data.getDbid());
        if (database.getIsPublic() && !data.getVisibility().equals(VisibilityTypeDto.EVERYONE)) {
            log.error("Identifier cannot restrict the result set");
            throw new IdentifierPublishingNotAllowedException("Identifier cannot restrict the result set");
        }
        /* find */
        final List<Identifier> optional = identifierRepository.findByDatabaseIdAndQueryId(data.getDbid(), data.getQid());
        if (!optional.isEmpty()) {
            log.error("Identifier already issued for database {} and query id {}", data.getDbid(), data.getQid());
            log.debug("identifier already exists similar to request {}", data);
            throw new IdentifierAlreadyExistsException("Identifier exists");
        }
        /* identifier */
        final Identifier tmp = identifierMapper.identifierCreateDtoToIdentifier(data);
        tmp.setContainerId(data.getCid());
        tmp.setDatabaseId(data.getDbid());
        final User creator = userService.findByUsername(principal.getName());
        tmp.setCreator(creator);
        tmp.setCreators(List.of());
        if (data.getType().equals(IdentifierTypeDto.SUBSET)) {
            log.debug("identifier describes a subset");
            final QueryDto query = queryServiceGateway.find(data.getCid(), data.getDbid(), data, authorization);
            tmp.setVisibility(identifierMapper.visibilityTypeDtoToVisibilityType(data.getVisibility()));
            tmp.setQuery(query.getQuery());
            tmp.setQueryId(query.getId());
            tmp.setQueryNormalized(query.getQueryNormalized());
            tmp.setQueryHash(query.getQueryHash());
            tmp.setExecution(query.getExecution());
            tmp.setResultNumber(query.getResultNumber());
            tmp.setResultHash(query.getResultHash());
        } else if (data.getType().equals(IdentifierTypeDto.DATABASE)) {
            log.debug("identifier describes a database");
            tmp.setVisibility(identifierMapper.databaseToVisibilityType(database));
        } else {
            log.error("Failed to map identifier type: {}", data.getType());
            throw new IdentifierPublishingNotAllowedException("Failed to map identifier type");
        }
        /* create in metadata database */
        final Identifier entity = identifierRepository.save(tmp);
        entity.setCreators(data.getCreators()
                .stream()
                .map(c -> {
                    final Creator creatorDto = identifierMapper.creatorCreateDtoToCreator(c);
                    creatorDto.setPid(entity.getId());
                    creatorDto.setCreator(creator);
                    return creatorDto;
                })
                .collect(Collectors.toList()));
        if (data.getRelatedIdentifiers() != null) {
            data.getRelatedIdentifiers()
                    .forEach(r -> {
                        final RelatedIdentifier id = identifierMapper.relatedIdentifierCreateDtoToRelatedIdentifier(r);
                        id.setIid(entity.getId());
                        id.setCreator(creator);
                        relatedIdentifierRepository.save(id);
                    });
        }
        final Identifier identifier = identifierRepository.save(entity);
        log.info("Created identifier with id {}", identifier.getId());
        log.trace("created identifier {}", identifier);
        final Identifier elIdentifier = identifieridxRepository.save(identifier);
        log.info("Created identifier with id {} in elastic search", elIdentifier.getId());
        return identifier;
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(Long identifierId) throws IdentifierNotFoundException {
        final Optional<Identifier> optional = identifierRepository.findById(identifierId);
        if (optional.isEmpty()) {
            log.error("Identifier with id {} not existing", identifierId);
            throw new IdentifierNotFoundException("Unable to find identifier");
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportMetadata(Long id) throws IdentifierNotFoundException {
        /* check */
        final Identifier identifier = find(id);
        /* context */
        final Context context = new Context();
        context.setVariable("doi", endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId());
        context.setVariable("creators", identifier.getCreators());
        context.setVariable("title", identifier.getTitle());
        context.setVariable("publisher", identifier.getPublisher());
        context.setVariable("publicationYear", identifier.getPublicationYear());
        context.setVariable("created", identifier.getCreated());
        context.setVariable("relatedIdentifiers", identifier.getRelated());
        context.setVariable("description", identifier.getDescription());
        /* map */
        final String body = templateEngine.process("doi.xml", context)
                .replaceAll("\\s+", " ");
        final InputStreamResource resource = new InputStreamResource(IOUtils.toInputStream(body, Charset.defaultCharset()));
        log.debug("mapped file stream {}", resource.getDescription());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportBibliography(Long id, BibliographyTypeDto style)
            throws IdentifierNotFoundException, IdentifierRequestException {
        /* check */
        final Identifier identifier = find(id);
        /* context */
        final Context context = new Context();
        context.setVariable("doi", endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId());
        context.setVariable("creator", identifier.getCreator());
        context.setVariable("creators", identifier.getCreators());
        context.setVariable("title", identifier.getTitle());
        context.setVariable("publisher", identifier.getPublisher());
        context.setVariable("publicationMonth", identifier.getPublicationMonth());
        context.setVariable("publicationYear", identifier.getPublicationYear());
        /* map */
        final String template = "cite_" + style.name().toLowerCase() + ".txt";
        final String body;
        try {
            body = templateEngine.process(template, context);
        } catch (TemplateInputException e) {
            log.error("Failed to load template: {}", e.getMessage());
            throw new IdentifierRequestException("Failed to load template", e);
        }
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportResource(Long identifierId) throws IdentifierNotFoundException,
            QueryNotFoundException, RemoteUnavailableException, IdentifierRequestException {
        /* check */
        final Identifier identifier = find(identifierId);
        if (identifier.getType().equals(IdentifierType.DATABASE)) {
            log.error("Failed to find identifier with id {} as it refers to a database and not a query", identifierId);
            log.debug("failed to find identifier {}", identifier);
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        /* export */
        if (identifier.getType().equals(IdentifierType.SUBSET)) {
            /* subset */
            final byte[] file = queryServiceGateway.export(identifier.getContainerId(),
                    identifier.getDatabaseId(), identifier.getQueryId());
            final InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(file));
            log.trace("found resource {}", resource);
            return resource;
        } else if (identifier.getType().equals(IdentifierType.DATABASE)) {
            /* database, we cannot export this to csv */
            log.warn("Failed to export database to csv, fallback to default http redirect");
            throw new IdentifierRequestException("Failed to export database to csv");
        }
        log.warn("Failed to export database, fallback to default http redirect");
        throw new IdentifierRequestException("Failed to export database");
    }

    @Override
    @Transactional
    public Identifier update(Long identifierId, IdentifierDto data)
            throws IdentifierNotFoundException {
        /* check */
        find(identifierId);
        /* map */
        final Identifier entity = identifierMapper.identifierDtoToIdentifier(data);
        entity.getCreators()
                .forEach(creator -> creator.setPid(identifierId));
        /* update */
        final Identifier entityUpdated = identifierRepository.save(entity);
        log.info("Updated identifier with id {}", identifierId);
        log.trace("updated identifier {}", entityUpdated);
        return entityUpdated;
    }

    @Override
    @Transactional
    public Identifier publish(Long identifierId, VisibilityTypeDto visibility)
            throws IdentifierNotFoundException, IdentifierAlreadyPublishedException {
        final Identifier identifier = find(identifierId);
        if (identifier.getVisibility().equals(VisibilityType.EVERYONE)) {
            /* once published, the identifier cannot be reverted, it is persistent! */
            log.error("Identifier is already published");
            throw new IdentifierAlreadyPublishedException("Identifier is already published");
        }
        identifier.setVisibility(identifierMapper.visibilityTypeDtoToVisibilityType(visibility));
        final Identifier entity = identifierRepository.save(identifier);
        log.info("Published identifier with id {}", identifierId);
        log.trace("published identifier {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public void delete(Long identifierId) throws IdentifierNotFoundException {
        /* check */
        final Identifier identifier = find(identifierId);
        /* delete */
        identifierRepository.delete(identifier);
        log.info("Deleted identifier with id {}", identifierId);
        log.trace("deleted identifier {}", identifier);
        identifieridxRepository.deleteById(identifierId);
        log.info("Deleted identifier with id {} in elastic search", identifierId);
    }

}
