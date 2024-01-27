package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.identifier.*;
import at.tuwien.config.MetadataConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.View;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierTitle;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.*;
import at.tuwien.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final ViewService viewService;
    private final QueryService queryService;
    private final StoreService storeService;
    private final DatabaseMapper databaseMapper;
    private final MetadataConfig metadataConfig;
    private final MetadataMapper metadataMapper;
    private final TemplateEngine templateEngine;
    private final DatabaseService databaseService;
    private final IdentifierMapper identifierMapper;
    private final IdentifierRepository identifierRepository;
    private final DatabaseIdxRepository databaseIdxRepository;

    public IdentifierServiceImpl(ViewService viewService, TemplateEngine templateEngine,
                                 DatabaseService databaseService, IdentifierMapper identifierMapper,
                                 QueryService queryService, StoreService storeService, DatabaseMapper databaseMapper,
                                 MetadataConfig metadataConfig, MetadataMapper metadataMapper,
                                 IdentifierRepository identifierRepository,
                                 DatabaseIdxRepository databaseIdxRepository) {
        this.viewService = viewService;
        this.queryService = queryService;
        this.storeService = storeService;
        this.databaseMapper = databaseMapper;
        this.metadataConfig = metadataConfig;
        this.metadataMapper = metadataMapper;
        this.templateEngine = templateEngine;
        this.databaseService = databaseService;
        this.identifierMapper = identifierMapper;
        this.identifierRepository = identifierRepository;
        this.databaseIdxRepository = databaseIdxRepository;
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
            log.error("Failed to find identifier with id: {}", identifierId);
            throw new IdentifierNotFoundException("Failed to find identifier with id: " + identifierId);
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier findByDoi(String doi) throws IdentifierNotFoundException {
        final Optional<Identifier> optional = identifierRepository.findByDoi(doi);
        if (optional.isEmpty()) {
            log.error("Failed to find identifier with doi {}: not existing", doi);
            throw new IdentifierNotFoundException("Failed to find identifier with doi " + doi + ": not existing");
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
    public List<Identifier> findAll(IdentifierTypeDto type, Long databaseId, Long queryId, Long viewId, Long tableId) {
        final List<Identifier> identifiers = this.identifierRepository.findAll();
        log.trace("found {} identifiers before applying filter(s)", identifiers.size());
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
        if (tableId != null) {
            log.trace("filter by table id: {}", tableId);
            stream = stream.filter(i -> Objects.nonNull(i.getTableId()))
                    .filter(i -> i.getTableId().equals(tableId));
        }
        return stream.toList();
    }

    @Override
    @Transactional
    public Identifier create(IdentifierSaveDto data, Principal principal) throws QueryNotFoundException,
            IdentifierRequestException, RemoteUnavailableException, UserNotFoundException, DatabaseNotFoundException,
            ViewNotFoundException, QueryStoreException, ImageNotSupportedException {
        /* create identifier */
        final Identifier entity = identifierMapper.identifierCreateDtoToIdentifier(data);
        entity.setCreatedBy(UserUtil.getId(principal));
        entity.setDatabaseId(data.getDatabaseId());
        final Database database = databaseService.find(data.getDatabaseId());
        entity.setDatabase(database);
        switch (data.getType()) {
            case SUBSET -> {
                log.debug("identifier type: subset with id {} and database with id {}", data.getQueryId(), data.getDatabaseId());
                final Query query = storeService.findOne(data.getDatabaseId(), data.getQueryId(), principal);
                entity.setQuery(query.getQuery());
                entity.setQueryId(query.getId());
                entity.setQueryNormalized(query.getQueryNormalized());
                entity.setQueryHash(query.getQueryHash());
                entity.setExecution(query.getExecuted());
                entity.setResultNumber(query.getResultNumber());
                entity.setResultHash(query.getResultHash());
            }
            case VIEW -> {
                log.debug("identifier type: view with id {} and database with id {}", data.getViewId(), data.getDatabaseId());
                final View view = viewService.findById(data.getDatabaseId(), data.getViewId());
                entity.setViewId(view.getId());
                entity.setQuery(view.getQuery());
                entity.setQueryNormalized(view.getQuery());
                entity.setQueryHash(view.getQueryHash());
            }
            case DATABASE -> log.debug("identifier type: database with id {}", data.getDatabaseId());
            case TABLE -> log.debug("identifier type: table with id {}", data.getTableId());
        }
        /* create in metadata database */
        final Identifier identifier = saveIdentifier(database, entity, data.getCreators(), data.getRelatedIdentifiers(),
                data.getTitles(), data.getDescriptions(), data.getFunders());
        /* create in search database */
        final DatabaseDto dto = databaseMapper.databaseToDatabaseDto(database);
        databaseIdxRepository.save(dto);
        log.info("Created identifier with id {} in metadata database & search database", identifier.getId());
        return identifier;
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportMetadata(Long id) throws IdentifierNotFoundException {
        /* check */
        final Identifier identifier = find(id);
        /* context */
        final Context context = new Context();
        context.setVariable("identifier", identifier);
        context.setVariable("identifierType", identifier.getDoi() != null ? "DOI" : "OAI");
        context.setVariable("pid", identifier.getDoi() != null ? ("doi:" + identifier.getDoi()) : identifier.getId());
        context.setVariable("datestamp", metadataMapper.instantToDatestamp(identifier.getCreated()));
        /* map */
        final String body = templateEngine.process("record_oai_datacite.xml", context)
                .replaceAll("\\s+", " ");
        final InputStreamResource resource = new InputStreamResource(IOUtils.toInputStream(body, Charset.defaultCharset()));
        log.debug("mapped file stream {}", resource.getDescription());
        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportBibliography(Long id, BibliographyTypeDto style) throws IdentifierNotFoundException,
            IdentifierRequestException {
        /* check */
        final Identifier identifier = find(id);
        /* context */
        final Context context = new Context();
        context.setVariable("identifier", identifier);
        context.setVariable("identifierType", identifier.getDoi() != null ? "doi" : "url");
        context.setVariable("title", preferTitle(identifier.getTitles()));
        context.setVariable("keyword", identifier.getDoi() != null ? "doi" : "howpublished");
        context.setVariable("urlOrDoi", identifier.getDoi() != null ? identifier.getDoi() : ("\\url{" + metadataConfig.getPidBase() + identifier.getId() + "}"));
        context.setVariable("url", identifier.getDoi() != null ? ("https://doi.org/" + identifier.getDoi()) : (metadataConfig.getPidBase() + identifier.getId()));
        /* map */
        final String template = "cite_" + style.name().toLowerCase() + ".txt";
        final String body;
        try {
            body = templateEngine.process(template, context);
        } catch (TemplateInputException e) {
            log.error("Failed to load template: {}", e.getMessage());
            throw new IdentifierRequestException("Failed to load template: " + e.getMessage(), e);
        }
        log.trace("mapped bibliography {}", body);
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportResource(Long identifierId, Principal principal) throws IdentifierNotFoundException,
            QueryNotFoundException, IdentifierRequestException, QueryStoreException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, FileStorageException, DataDbSidecarException,
            DataProcessingException {
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
    public void delete(Long identifierId) throws IdentifierNotFoundException, DatabaseNotFoundException {
        /* delete in metadata database */
        final Identifier identifier = find(identifierId);
        identifierRepository.deleteById(identifierId);
        /* delete in opensearch database */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(databaseService.find(identifier.getDatabaseId())));
        log.info("Deleted identifier with id {} in metadata database & search database", identifierId);
    }

    public IdentifierTitle preferTitle(List<IdentifierTitle> titles) {
        final Optional<IdentifierTitle> optional = titles.stream()
                .filter(t -> Objects.nonNull(t.getLanguage()))
                .filter(t -> t.getLanguage().equals(LanguageType.EN))
                .findFirst();
        return optional.orElseGet(() -> titles.get(0));
    }

    public Identifier saveIdentifier(Database database, Identifier entity, List<CreatorSaveDto> creators,
                                     List<RelatedIdentifierSaveDto> relatedIdentifiers,
                                     List<IdentifierSaveTitleDto> titles,
                                     List<IdentifierSaveDescriptionDto> descriptions,
                                     List<IdentifierFunderSaveDto> funders) {
        /* create in metadata database */
        if (creators != null) {
            entity.setCreators(creators.stream()
                    .map(identifierMapper::creatorCreateDtoToCreator)
                    .peek(c -> c.setIdentifier(entity))
                    .toList());
            log.debug("set {} creator(s)", entity.getCreators().size());
        }
        if (relatedIdentifiers != null) {
            entity.setRelatedIdentifiers(relatedIdentifiers.stream()
                    .map(identifierMapper::relatedIdentifierCreateDtoToRelatedIdentifier)
                    .peek(r -> r.setIdentifier(entity))
                    .toList());
            log.debug("set {} related identifier(s)", entity.getRelatedIdentifiers().size());
        }
        if (titles != null) {
            entity.setTitles(null);
            entity.setTitles(titles.stream()
                    .map(identifierMapper::identifierCreateTitleDtoToIdentifierTitle)
                    .peek(t -> t.setIdentifier(entity))
                    .toList());
            log.debug("set {} title(s)", entity.getTitles().size());
        }
        if (descriptions != null) {
            entity.setDescriptions(descriptions.stream()
                    .map(identifierMapper::identifierCreateDescriptionDtoToIdentifierDescription)
                    .peek(d -> d.setIdentifier(entity))
                    .toList());
            log.debug("set {} description(s)", entity.getDescriptions().size());
        }
        if (funders != null) {
            entity.setFunders(funders.stream()
                    .map(identifierMapper::identifierFunderSaveDtoToIdentifierFunder)
                    .peek(d -> d.setIdentifier(entity))
                    .toList());
            log.debug("set {} funder(s)", entity.getFunders().size());
        }
        /* create new identifier */
        final Identifier identifier = identifierRepository.save(entity);
        database.setIdentifiers(new ArrayList<>(database.getIdentifiers()));
        database.getIdentifiers().add(identifier);
        return identifier;
    }

}
