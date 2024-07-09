package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.*;
import at.tuwien.config.MetadataConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.View;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierStatusType;
import at.tuwien.entities.identifier.IdentifierTitle;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.repository.IdentifierRepository;
import at.tuwien.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final ViewService viewService;
    private final MetadataConfig metadataConfig;
    private final MetadataMapper metadataMapper;
    private final TemplateEngine templateEngine;
    private final DataServiceGateway dataServiceGateway;
    private final IdentifierRepository identifierRepository;
    private final SearchServiceGateway searchServiceGateway;


    public IdentifierServiceImpl(ViewService viewService, TemplateEngine templateEngine, MetadataMapper metadataMapper,
                                 MetadataConfig metadataConfig, DataServiceGateway dataServiceGateway,
                                 IdentifierRepository identifierRepository, SearchServiceGateway searchServiceGateway) {
        this.viewService = viewService;
        this.metadataConfig = metadataConfig;
        this.metadataMapper = metadataMapper;
        this.templateEngine = templateEngine;
        this.dataServiceGateway = dataServiceGateway;
        this.identifierRepository = identifierRepository;
        this.searchServiceGateway = searchServiceGateway;
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
                    .filter(i -> i.getType().equals(metadataMapper.identifierTypeDtoToIdentifierType(type)));
        }
        if (databaseId != null) {
            log.trace("filter by database id: {}", databaseId);
            stream = stream.filter(i -> Objects.nonNull(i.getDatabase().getId()))
                    .filter(i -> databaseId.equals(i.getDatabase().getId()));
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
    public Identifier publish(Long identifierId) throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException {
        Identifier identifier = find(identifierId);
        /* publish identifier */
        identifier.setStatus(IdentifierStatusType.PUBLISHED);
        identifier = identifierRepository.save(identifier);
        /* update in search service */
        searchServiceGateway.update(identifier.getDatabase());
        log.info("Published identifier with id {}", identifier.getId());
        return identifier;
    }

    @Override
    @Transactional
    public Identifier save(Database database, User user, IdentifierSaveDto data) throws SearchServiceException,
            DataServiceException, QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException {
        final Identifier identifier = find(data.getId());
        identifier.setDatabase(database);
        identifier.setCreatedBy(user.getId());
        identifier.setCreator(user);
        identifier.setStatus(IdentifierStatusType.DRAFT);
        /* set from data */
        identifier.setTableId(data.getTableId());
        identifier.setQueryId(data.getQueryId());
        identifier.setViewId(data.getViewId());
        identifier.setDoi(data.getDoi());
        identifier.setLanguage(metadataMapper.languageTypeDtoToLanguageType(data.getLanguage()));
        identifier.setLicenses(new LinkedList<>(data.getLicenses()
                .stream()
                .map(metadataMapper::licenseDtoToLicense)
                .toList()));
        identifier.setPublicationDay(data.getPublicationDay());
        identifier.setPublicationMonth(data.getPublicationMonth());
        identifier.setPublicationYear(data.getPublicationYear());
        identifier.setType(metadataMapper.identifierTypeDtoToIdentifierType(data.getType()));
        /* create in metadata database */
        if (data.getCreators() != null) {
            identifier.setCreators(new LinkedList<>(data.getCreators()
                    .stream()
                    .map(metadataMapper::creatorCreateDtoToCreator)
                    .peek(c -> c.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} creator(s)", identifier.getCreators().size());
        }
        if (data.getRelatedIdentifiers() != null) {
            identifier.setRelatedIdentifiers(new LinkedList<>(data.getRelatedIdentifiers()
                    .stream()
                    .map(metadataMapper::relatedIdentifierCreateDtoToRelatedIdentifier)
                    .peek(r -> r.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} related identifier(s)", identifier.getRelatedIdentifiers().size());
        }
        if (data.getTitles() != null) {
            identifier.setTitles(new LinkedList<>(data.getTitles()
                    .stream()
                    .map(metadataMapper::identifierCreateTitleDtoToIdentifierTitle)
                    .peek(t -> t.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} title(s)", identifier.getTitles().size());
        }
        if (data.getDescriptions() != null) {
            identifier.setDescriptions(new LinkedList<>(data.getDescriptions()
                    .stream()
                    .map(metadataMapper::identifierCreateDescriptionDtoToIdentifierDescription)
                    .peek(d -> d.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} description(s)", identifier.getDescriptions().size());
        }
        if (data.getFunders() != null) {
            identifier.setFunders(new LinkedList<>(data.getFunders()
                    .stream()
                    .map(metadataMapper::identifierFunderSaveDtoToIdentifierFunder)
                    .peek(d -> d.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} funder(s)", identifier.getFunders().size());
        }
        return save(identifier);
    }

    @Override
    @Transactional
    public Identifier create(Database database, User user, IdentifierCreateDto data) throws SearchServiceException,
            DataServiceException, QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException {
        final Identifier identifier = metadataMapper.identifierCreateDtoToIdentifier(data);
        identifier.setDatabase(database);
        identifier.setCreatedBy(user.getId());
        identifier.setCreator(user);
        identifier.setStatus(IdentifierStatusType.DRAFT);
        /* create in metadata database */
        if (data.getCreators() != null) {
            identifier.setCreators(new LinkedList<>(data.getCreators()
                    .stream()
                    .map(metadataMapper::creatorCreateDtoToCreator)
                    .peek(c -> c.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} creator(s)", identifier.getCreators().size());
        }
        if (data.getRelatedIdentifiers() != null) {
            identifier.setRelatedIdentifiers(new LinkedList<>(data.getRelatedIdentifiers()
                    .stream()
                    .map(metadataMapper::relatedIdentifierCreateDtoToRelatedIdentifier)
                    .peek(r -> r.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} related identifier(s)", identifier.getRelatedIdentifiers().size());
        }
        if (data.getTitles() != null) {
            identifier.setTitles(null);
            identifier.setTitles(new LinkedList<>(data.getTitles()
                    .stream()
                    .map(metadataMapper::identifierCreateTitleDtoToIdentifierTitle)
                    .peek(t -> t.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} title(s)", identifier.getTitles().size());
        }
        if (data.getDescriptions() != null) {
            identifier.setDescriptions(new LinkedList<>(data.getDescriptions()
                    .stream()
                    .map(metadataMapper::identifierCreateDescriptionDtoToIdentifierDescription)
                    .peek(d -> d.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} description(s)", identifier.getDescriptions().size());
        }
        if (data.getFunders() != null) {
            identifier.setFunders(new LinkedList<>(data.getFunders()
                    .stream()
                    .map(metadataMapper::identifierFunderSaveDtoToIdentifierFunder)
                    .peek(d -> d.setIdentifier(identifier))
                    .toList()));
            log.debug("set {} funder(s)", identifier.getFunders().size());
        }
        return save(identifier);
    }

    @Transactional
    public Identifier save(Identifier identifier) throws DataServiceException, DataServiceConnectionException,
            IdentifierNotFoundException, ViewNotFoundException, DatabaseNotFoundException, QueryNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        /* save identifier */
        switch (identifier.getType()) {
            case SUBSET -> {
                log.debug("identifier type: subset with id {} and database with id {}", identifier.getQueryId(), identifier.getDatabase().getId());
                final QueryDto query = dataServiceGateway.findQuery(identifier.getDatabase().getId(), identifier.getQueryId());
                identifier.setQuery(query.getQuery());
                identifier.setQueryId(query.getId());
                identifier.setQueryNormalized(query.getQueryNormalized());
                identifier.setQueryHash(query.getQueryHash());
                identifier.setExecution(query.getExecution());
                identifier.setResultNumber(query.getResultNumber());
                identifier.setResultHash(query.getResultHash());
            }
            case VIEW -> {
                log.debug("identifier type: view with id {} and database with id {}", identifier.getViewId(), identifier.getDatabase().getId());
                final View view = viewService.findById(identifier.getDatabase(), identifier.getViewId());
                identifier.setViewId(view.getId());
                identifier.setQuery(view.getQuery());
                identifier.setQueryNormalized(view.getQuery());
                identifier.setQueryHash(view.getQueryHash());
            }
            case DATABASE -> log.debug("identifier type: database with id {}", identifier.getDatabase());
            case TABLE -> log.debug("identifier type: table with id {}", identifier.getTableId());
        }
        /* save identifier in metadata database */
        final Identifier out = identifierRepository.save(identifier);
        /* update in search database */
        identifier.getDatabase()
                .getIdentifiers()
                .add(out);
        searchServiceGateway.update(identifier.getDatabase());
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportMetadata(Identifier identifier) {
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
    public String exportBibliography(Identifier identifier, BibliographyTypeDto style) throws MalformedException {
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
            log.error("Failed export bibliography: template error: {}", e.getMessage());
            throw new MalformedException("Failed export bibliography: template error: " + e.getMessage(), e);
        }
        log.trace("mapped bibliography {}", body);
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportResource(Identifier identifier) throws DataServiceException,
            DataServiceConnectionException, QueryNotFoundException {
        final ExportResourceDto exportResource = dataServiceGateway.exportQuery(identifier.getDatabase().getId(), identifier.getQueryId());
        return exportResource.getResource();
    }

    @Override
    @Transactional
    public void delete(Identifier identifier) throws DataServiceException, DataServiceConnectionException,
            IdentifierNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* delete in metadata database */
        identifierRepository.deleteById(identifier.getId());
        /* delete in search database */
        identifier.getDatabase()
                .getIdentifiers()
                .remove(identifier);
        searchServiceGateway.update(identifier.getDatabase());
        log.info("Deleted identifier with id {}", identifier.getId());
    }

    public IdentifierTitle preferTitle(List<IdentifierTitle> titles) {
        final Optional<IdentifierTitle> optional = titles.stream()
                .filter(t -> Objects.nonNull(t.getLanguage()))
                .filter(t -> t.getLanguage().equals(LanguageType.EN))
                .findFirst();
        return optional.orElseGet(() -> titles.get(0));
    }

}
