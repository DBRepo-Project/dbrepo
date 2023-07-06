package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.*;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.identifier.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.repository.mdb.IdentifierRepository;
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
    private final IdentifierIdxRepository identifierIdxRepository;

    public IdentifierServiceImpl(UserService userService, EndpointConfig endpointConfig, TemplateEngine templateEngine,
                                 DatabaseService databaseService, IdentifierMapper identifierMapper,
                                 QueryServiceGateway queryServiceGateway, IdentifierRepository identifierRepository,
                                 IdentifierIdxRepository identifierIdxRepository) {
        this.userService = userService;
        this.endpointConfig = endpointConfig;
        this.templateEngine = templateEngine;
        this.databaseService = databaseService;
        this.identifierMapper = identifierMapper;
        this.queryServiceGateway = queryServiceGateway;
        this.identifierRepository = identifierRepository;
        this.identifierIdxRepository = identifierIdxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(Long databaseId, Long queryId) throws IdentifierNotFoundException {
        if (databaseId != null && queryId != null) {
            return findByDatabaseIdAndQueryId(databaseId, queryId);
        } else if (databaseId == null && queryId != null) {
            return identifierRepository.findByQueryId(queryId);
        } else if (databaseId != null && queryId == null) {
            return identifierRepository.findByDatabaseId(databaseId);
        }
        return identifierRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId) {
        return identifierRepository.findByDatabaseIdAndQueryId(databaseId, queryId);
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
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException {
        /* check */
        if (data.getType().equals(IdentifierTypeDto.DATABASE) && identifierRepository.existsByDatabaseIdAndType(data.getDbid(), IdentifierType.DATABASE)) {
            log.error("Identifier already issued for database with id {}", data.getDbid());
            throw new IdentifierAlreadyExistsException("Database identifier already exists");
        } else if (data.getType().equals(IdentifierTypeDto.SUBSET) && identifierRepository.existsByDatabaseIdAndQueryIdAndType(data.getDbid(), data.getQid(), IdentifierType.SUBSET)) {
            log.error("Identifier already issued for database with id {} and query with id {}", data.getDbid(), data.getQid());
            throw new IdentifierAlreadyExistsException("Subset identifier already exists");
        }
        /* create identifier */
        final Identifier identifier = identifierMapper.identifierCreateDtoToIdentifier(data);
        final User creator = userService.findByUsername(principal.getName());
        identifier.setCreator(creator);
        final Database database = databaseService.find(data.getDbid());
        identifier.setDatabase(database);
        if (data.getType().equals(IdentifierTypeDto.SUBSET)) {
            log.debug("identifier describes a subset");
            final QueryDto query = queryServiceGateway.find(data.getDbid(), data, authorization);
            identifier.setQuery(query.getQuery());
            identifier.setQueryId(query.getId());
            identifier.setQueryNormalized(query.getQueryNormalized());
            identifier.setQueryHash(query.getQueryHash());
            identifier.setExecution(query.getExecution());
            identifier.setResultNumber(query.getResultNumber());
            identifier.setResultHash(query.getResultHash());
        }
        saveIdentifier(identifier, data.getCreators(), data.getRelatedIdentifiers(), data.getTitles(), data.getDescriptions());
        log.info("Created identifier with id {}", identifier.getId());
        log.trace("created identifier {}", identifier);
        identifierIdxRepository.save(identifierMapper.identifierToIdentifierDto(identifier));
        log.info("Created identifier with id {} in elastic search", identifier.getId());
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
        if (identifier.getDoi() != null) {
            context.setVariable("identifierType", "DOI");
            context.setVariable("identifier", identifier.getDoi());
        } else {
            context.setVariable("identifierType", "PID");
            context.setVariable("identifier", endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId());
        }
        context.setVariable("creators", identifier.getCreators());
        context.setVariable("titles", identifier.getTitles());
        context.setVariable("publisher", identifier.getPublisher());
        context.setVariable("publicationYear", identifier.getPublicationYear());
        context.setVariable("created", identifier.getCreated());
        context.setVariable("relatedIdentifiers", identifier.getRelated());
        context.setVariable("descriptions", identifier.getDescriptions());
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
        if (identifier.getDoi() != null) {
            context.setVariable("identifierType", "doi");
            context.setVariable("identifier", identifier.getDoi());
        } else {
            context.setVariable("identifierType", "url");
            context.setVariable("identifier", endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId());
        }
        context.setVariable("creator", identifier.getCreator());
        context.setVariable("creators", identifier.getCreators());
        context.setVariable("title", preferTitle(identifier.getTitles()));
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
        log.trace("mapped bibliography {}", body);
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
            throw new IdentifierRequestException("Failed to find identifier");
        }
        /* subset */
        final byte[] file = queryServiceGateway.export(identifier.getDatabase().getId(), identifier.getQueryId());
        final InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(file));
        log.trace("found resource {}", resource);
        return resource;
    }

    @Override
    @Transactional
    public Identifier update(Long identifierId, IdentifierUpdateDto data, Principal principal, String authorization)
            throws UserNotFoundException, DatabaseNotFoundException, QueryNotFoundException, RemoteUnavailableException {
        /* create identifier */
        final Identifier identifier = identifierMapper.identifierUpdateDtoToIdentifier(data);
        identifier.setId(identifierId);
        final User creator = userService.findByUsername(principal.getName());
        identifier.setCreator(creator);
        final Database database = databaseService.find(data.getDbid());
        identifier.setDatabase(database);
        if (data.getType().equals(IdentifierTypeDto.SUBSET)) {
            log.debug("identifier describes a subset");
            final IdentifierCreateDto payload = identifierMapper.identifierUpdateDtoToIdentifierCreateDto(data);
            final QueryDto query = queryServiceGateway.find(data.getDbid(), payload, authorization);
            identifier.setQuery(query.getQuery());
            identifier.setQueryId(query.getId());
            identifier.setQueryNormalized(query.getQueryNormalized());
            identifier.setQueryHash(query.getQueryHash());
            identifier.setExecution(query.getExecution());
            identifier.setResultNumber(query.getResultNumber());
            identifier.setResultHash(query.getResultHash());
        }
        /* update in metadata database */
        final Identifier out = saveIdentifier(identifier, data.getCreators(), data.getRelatedIdentifiers(), data.getTitles(), data.getDescriptions());
        log.info("Updated identifier with id {}", identifierId);
        /* elastic search */
        identifierIdxRepository.save(identifierMapper.identifierToIdentifierDto(out));
        log.info("Updated identifier with id {} in elastic search", identifierId);
        return out;
    }

    @Override
    @Transactional
    public void delete(Long identifierId) throws IdentifierNotFoundException {
        /* delete in metadata database */
        if (!identifierRepository.existsById(identifierId)) {
            throw new IdentifierNotFoundException("Identifier not found in metadata database");
        }
        identifierRepository.deleteById(identifierId);
        log.info("Deleted identifier with id {}", identifierId);
        /* delete in elastic search */
        if (!identifierIdxRepository.existsById(identifierId)) {
            throw new IdentifierNotFoundException("Identifier not found in metadata database");
        }
        identifierIdxRepository.deleteById(identifierId);
        log.info("Deleted identifier with id {} in elastic search", identifierId);
    }

    public IdentifierTitle preferTitle(List<IdentifierTitle> titles) {
        final Optional<IdentifierTitle> optional = titles.stream()
                .filter(t -> t.getLanguage().equals(LanguageType.EN))
                .findFirst();
        return optional.orElseGet(() -> titles.get(0));
    }

    public Identifier saveIdentifier(Identifier identifier,
                                     List<CreatorCreateDto> creators,
                                     List<RelatedIdentifierCreateDto> relatedIdentifiers,
                                     List<IdentifierCreateTitleDto> titles,
                                     List<IdentifierCreateDescriptionDto> descriptions) {
        /* create in metadata database */
        if (creators != null) {
            identifier.setCreators(null);
            identifier.setCreators(creators.stream()
                    .map(identifierMapper::creatorCreateDtoToCreator)
                    .peek(c -> c.setIdentifier(identifier))
                    .toList());
            log.debug("set {} creator(s)", identifier.getCreators().size());
        }
        if (relatedIdentifiers != null) {
            identifier.setRelated(null);
            identifier.setRelated(relatedIdentifiers.stream()
                    .map(identifierMapper::relatedIdentifierCreateDtoToRelatedIdentifier)
                    .peek(r -> r.setIdentifier(identifier))
                    .toList());
            log.debug("set {} related identifier(s)", identifier.getRelated().size());
        }
        if (titles != null) {
            identifier.setTitles(null);
            identifier.setTitles(titles.stream()
                    .map(identifierMapper::identifierCreateTitleDtoToIdentifierTitle)
                    .peek(t -> t.setIdentifier(identifier))
                    .toList());
            log.debug("set {} title(s)", identifier.getTitles().size());
        }
        if (descriptions != null) {
            identifier.setDescriptions(null);
            identifier.setDescriptions(descriptions.stream()
                    .map(identifierMapper::identifierCreateDescriptionDtoToIdentifierDescription)
                    .peek(d -> d.setIdentifier(identifier))
                    .toList());
            log.debug("set {} description(s)", identifier.getDescriptions().size());
        }
        return identifierRepository.save(identifier);
    }

}
