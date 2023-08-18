package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.api.identifier.*;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.View;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierTitle;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.service.*;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final UserService userService;
    private final ViewService viewService;
    private final EndpointConfig endpointConfig;
    private final TemplateEngine templateEngine;
    private final DatabaseService databaseService;
    private final IdentifierMapper identifierMapper;
    private final QueryService queryService;
    private final StoreService storeService;
    private final IdentifierRepository identifierRepository;
    private final IdentifierIdxRepository identifierIdxRepository;

    public IdentifierServiceImpl(UserService userService, ViewService viewService, EndpointConfig endpointConfig,
                                 TemplateEngine templateEngine, DatabaseService databaseService,
                                 IdentifierMapper identifierMapper, QueryService queryService,
                                 StoreService storeService, IdentifierRepository identifierRepository,
                                 IdentifierIdxRepository identifierIdxRepository) {
        this.userService = userService;
        this.viewService = viewService;
        this.endpointConfig = endpointConfig;
        this.templateEngine = templateEngine;
        this.databaseService = databaseService;
        this.identifierMapper = identifierMapper;
        this.queryService = queryService;
        this.storeService = storeService;
        this.identifierRepository = identifierRepository;
        this.identifierIdxRepository = identifierIdxRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll() {
        return identifierRepository.findAll();
    }

    @Override
    public List<Identifier> findAll(Long databaseId) {
        return identifierRepository.findByDatabaseId(databaseId);
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
    public List<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId) {
        return identifierRepository.findByDatabaseIdAndQueryId(databaseId, queryId);
    }

    @Override
    public List<Identifier> findAllDatabaseIdentifiers() {
        return identifierRepository.findAllDatabaseIdentifiers();
    }

    @Override
    public List<Identifier> findAllSubsetIdentifiers() {
        return identifierRepository.findAllSubsetIdentifiers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(IdentifierTypeDto type, Long databaseId, Long queryId, Long viewId) {
        final List<Identifier> identifiers = this.identifierRepository.findAll();
        Stream<Identifier> stream = identifiers.stream();
        if (type != null) {
            log.trace("filter by type: {}", type);
            stream = stream.filter(i -> Objects.nonNull(i.getType()))
                    .filter(i -> i.getType().equals(identifierMapper.identifierTypeDtoToIdentifierType(type)));
        }
        if (databaseId != null) {
            log.trace("filter by database id: {}", databaseId);
            stream = stream.filter(i -> Objects.nonNull(i.getDatabaseId()))
                    .filter(i -> i.getDatabaseId().equals(databaseId));
        }
        if (queryId != null) {
            log.trace("filter by query id: {}", queryId);
            stream = stream.filter(i -> Objects.nonNull(i.getQueryId()))
                    .filter(i -> i.getQueryId().equals(queryId));
        }
        if (viewId != null) {
            log.trace("filter by view id: {}", viewId);
            stream = stream.filter(i -> Objects.nonNull(i.getViewId()))
                    .filter(i -> i.getViewId().equals(viewId));
        }
        return stream.toList();
    }

    @Override
    @Transactional
    public Identifier create(IdentifierSaveDto data, Principal principal)
            throws QueryNotFoundException, RemoteUnavailableException, IdentifierAlreadyExistsException,
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException {
        /* check */
        if (data.getType().equals(IdentifierTypeDto.DATABASE) && identifierRepository.existsByDatabaseIdAndType(data.getDatabaseId(), IdentifierType.DATABASE)) {
            log.error("Identifier already issued for database with id {}", data.getDatabaseId());
            throw new IdentifierAlreadyExistsException("Database identifier already exists");
        } else if (data.getType().equals(IdentifierTypeDto.SUBSET) && identifierRepository.existsByDatabaseIdAndQueryIdAndType(data.getDatabaseId(), data.getQueryId(), IdentifierType.SUBSET)) {
            log.error("Identifier already issued for database with id {} and query with id {}", data.getDatabaseId(), data.getQueryId());
            throw new IdentifierAlreadyExistsException("Subset identifier already exists");
        }
        /* create identifier */
        final Identifier identifier = identifierMapper.identifierCreateDtoToIdentifier(data);
        final User creator = userService.findByUsername(principal.getName());
        identifier.setCreator(creator);
        identifier.setDatabaseId(data.getDatabaseId());
        final Database database = databaseService.find(data.getDatabaseId());
        identifier.setDatabase(database);
        if (data.getType().equals(IdentifierTypeDto.SUBSET)) {
            log.debug("identifier type: subset");
            final Query query = storeService.findOne(data.getDatabaseId(), data.getQueryId(), principal);
            identifier.setQuery(query.getQuery());
            identifier.setQueryId(query.getId());
            identifier.setQueryNormalized(query.getQueryNormalized());
            identifier.setQueryHash(query.getQueryHash());
            identifier.setExecution(query.getExecuted());
            identifier.setResultNumber(query.getResultNumber());
            identifier.setResultHash(query.getResultHash());
        } else if (data.getType().equals(IdentifierTypeDto.VIEW)) {
            log.debug("identifier type: view");
            final View view = viewService.findById(data.getViewId());
            identifier.setViewId(view.getId());
            identifier.setQuery(view.getQuery());
            identifier.setQueryNormalized(view.getQuery());
            identifier.setQueryHash(view.getQueryHash());
        }
        /* create in metadata database */
        final Identifier entity = saveIdentifier(identifier, data.getCreators(), data.getRelatedIdentifiers(),
                data.getTitles(), data.getDescriptions(), data.getFunders());
        log.info("Created identifier with id {} in metadata database", entity.getId());
        /* create in open search database */
        identifierIdxRepository.save(identifierMapper.identifierToIdentifierDto(entity));
        log.info("Created identifier with id {} in open search database", entity.getId());
        return entity;
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
        context.setVariable("language", identifier.getLanguage());
        context.setVariable("creators", identifier.getCreators());
        context.setVariable("titles", identifier.getTitles());
        context.setVariable("publisher", identifier.getPublisher());
        context.setVariable("publicationYear", identifier.getPublicationYear());
        context.setVariable("created", identifier.getCreated());
        context.setVariable("relatedIdentifiers", identifier.getRelatedIdentifiers());
        context.setVariable("funders", identifier.getFunders());
        context.setVariable("descriptions", identifier.getDescriptions());
        context.setVariable("licenses", identifier.getLicenses());
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
    public InputStreamResource exportResource(Long identifierId, Principal principal) throws IdentifierNotFoundException,
            QueryNotFoundException, IdentifierRequestException, UserNotFoundException,
            QueryStoreException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, FileStorageException {
        /* check */
        final Identifier identifier = find(identifierId);
        if (identifier.getType().equals(IdentifierType.DATABASE)) {
            log.error("Failed to find identifier with id {} as it refers to a database and not a query", identifierId);
            throw new IdentifierRequestException("Failed to find identifier");
        }
        /* subset */
        ExportResource exportResource = queryService.findOne(identifier.getDatabase().getId(), identifier.getQueryId(), null);
        final InputStreamResource resource = exportResource.getResource();
        log.trace("found resource {}", resource);
        return resource;
    }

    @Override
    @Transactional
    public Identifier update(Long identifierId, IdentifierSaveDto data, Principal principal)
            throws UserNotFoundException, DatabaseNotFoundException, QueryNotFoundException, RemoteUnavailableException,
            IdentifierNotFoundException, QueryStoreException, DatabaseConnectionException, ImageNotSupportedException {
        /* find doi */
        final Identifier oldIdentifier = find(identifierId);
        /* create identifier */
        final Identifier identifier = identifierMapper.identifierUpdateDtoToIdentifier(data);
        identifier.setId(identifierId);
        identifier.setDoi(oldIdentifier.getDoi());
        final User creator = userService.findByUsername(principal.getName());
        identifier.setCreator(creator);
        final Database database = databaseService.find(data.getDatabaseId());
        identifier.setDatabase(database);
        if (data.getType().equals(IdentifierTypeDto.SUBSET)) {
            log.debug("identifier describes a subset");
            final IdentifierSaveDto payload = identifierMapper.identifierUpdateDtoToIdentifierCreateDto(data);
            final Query query = storeService.findOne(data.getDatabaseId(), payload.getQueryId(), principal);
            identifier.setQuery(query.getQuery());
            identifier.setQueryId(query.getId());
            identifier.setQueryNormalized(query.getQueryNormalized());
            identifier.setQueryHash(query.getQueryHash());
            identifier.setExecution(query.getExecuted());
            identifier.setResultNumber(query.getResultNumber());
            identifier.setResultHash(query.getResultHash());
        }
        /* update in metadata database */
        final Identifier entity = saveIdentifier(identifier, data.getCreators(), data.getRelatedIdentifiers(),
                data.getTitles(), data.getDescriptions(), data.getFunders());
        log.info("Updated identifier with id {} in metadata database", identifierId);
        /* update in open search database */
        identifierIdxRepository.save(identifierMapper.identifierToIdentifierDto(entity));
        log.info("Updated identifier with id {} in open search database", identifierId);
        return entity;
    }

    @Override
    @Transactional
    public void delete(Long identifierId) throws IdentifierNotFoundException {
        /* delete in metadata database */
        if (!identifierRepository.existsById(identifierId)) {
            log.error("Failed to find identifier with id {} in metadata database", identifierId);
            throw new IdentifierNotFoundException("Failed to find identifier with id " + identifierId + " in metadata database");
        }
        identifierRepository.deleteById(identifierId);
        log.info("Deleted identifier with id {} in metadata database", identifierId);
        /* delete in elastic search */
        if (!identifierIdxRepository.existsById(identifierId)) {
            log.error("Failed to find identifier with id {} in open search database", identifierId);
            throw new IdentifierNotFoundException("Failed to find identifier with id " + identifierId + " in open search database");
        }
        identifierIdxRepository.deleteById(identifierId);
        log.info("Deleted identifier with id {} in open search database", identifierId);
    }

    public IdentifierTitle preferTitle(List<IdentifierTitle> titles) {
        final Optional<IdentifierTitle> optional = titles.stream()
                .filter(t -> Objects.nonNull(t.getLanguage()))
                .filter(t -> t.getLanguage().equals(LanguageType.EN))
                .findFirst();
        return optional.orElseGet(() -> titles.get(0));
    }

    public Identifier saveIdentifier(Identifier identifier, List<CreatorSaveDto> creators,
                                     List<RelatedIdentifierSaveDto> relatedIdentifiers,
                                     List<IdentifierSaveTitleDto> titles,
                                     List<IdentifierSaveDescriptionDto> descriptions,
                                     List<IdentifierFunderSaveDto> funders) {
        /* create in metadata database */
        if (creators != null) {
            identifier.setCreators(creators.stream()
                    .map(identifierMapper::creatorCreateDtoToCreator)
                    .peek(c -> c.setIdentifier(identifier))
                    .toList());
            log.debug("set {} creator(s)", identifier.getCreators().size());
        }
        if (relatedIdentifiers != null) {
            identifier.setRelatedIdentifiers(relatedIdentifiers.stream()
                    .map(identifierMapper::relatedIdentifierCreateDtoToRelatedIdentifier)
                    .peek(r -> r.setIdentifier(identifier))
                    .toList());
            log.debug("set {} related identifier(s)", identifier.getRelatedIdentifiers().size());
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
            identifier.setDescriptions(descriptions.stream()
                    .map(identifierMapper::identifierCreateDescriptionDtoToIdentifierDescription)
                    .peek(d -> d.setIdentifier(identifier))
                    .toList());
            log.debug("set {} description(s)", identifier.getDescriptions().size());
        }
        if (funders != null) {
            identifier.setFunders(funders.stream()
                    .map(identifierMapper::identifierFunderSaveDtoToIdentifierFunder)
                    .peek(d -> d.setIdentifier(identifier))
                    .toList());
            log.debug("set {} funder(s)", identifier.getFunders().size());
        }
        return identifierRepository.save(identifier);
    }

}
