package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.*;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.repository.mdb.RelatedIdentifierRepository;
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
import java.util.LinkedList;
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
    private final IdentifierIdxRepository identifierIdxRepository;
    private final RelatedIdentifierRepository relatedIdentifierRepository;

    public IdentifierServiceImpl(UserService userService, EndpointConfig endpointConfig, TemplateEngine templateEngine,
                                 DatabaseService databaseService, IdentifierMapper identifierMapper,
                                 QueryServiceGateway queryServiceGateway, IdentifierRepository identifierRepository,
                                 IdentifierIdxRepository identifierIdxRepository,
                                 RelatedIdentifierRepository relatedIdentifierRepository) {
        this.userService = userService;
        this.endpointConfig = endpointConfig;
        this.templateEngine = templateEngine;
        this.databaseService = databaseService;
        this.identifierMapper = identifierMapper;
        this.queryServiceGateway = queryServiceGateway;
        this.identifierRepository = identifierRepository;
        this.identifierIdxRepository = identifierIdxRepository;
        this.relatedIdentifierRepository = relatedIdentifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(Long databaseId, Long queryId) throws IdentifierNotFoundException {
        if (databaseId != null && queryId != null) {
            return List.of(find(databaseId, queryId));
        } else if (databaseId == null && queryId != null) {
            return identifierRepository.findByQueryId(queryId);
        } else if (databaseId != null && queryId == null) {
            return identifierRepository.findByDatabaseId(databaseId);
        }
        return identifierRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(Long databaseId, Long queryId) throws IdentifierNotFoundException {
        final Optional<Identifier> identifier = identifierRepository.findByDatabaseIdAndQueryId(databaseId, queryId);
        if (identifier.isEmpty()) {
            log.error("Failed to find identifier with query id {}", queryId);
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        return identifier.get();
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
        final Database database = databaseService.find(data.getDbid());
        if (data.getType().equals(IdentifierTypeDto.DATABASE) && identifierRepository.existsByDatabaseIdAndType(data.getDbid(), IdentifierType.DATABASE)) {
            log.error("Identifier already issued for database with id {}", data.getDbid());
            throw new IdentifierAlreadyExistsException("Database identifier already exists");
        } else if (data.getType().equals(IdentifierTypeDto.SUBSET) && identifierRepository.existsByDatabaseIdAndQueryIdAndType(data.getDbid(), data.getQid(), IdentifierType.SUBSET)) {
            log.error("Identifier already issued for database with id {} and query with id {}", data.getDbid(), data.getQid());
            throw new IdentifierAlreadyExistsException("Subset identifier already exists");
        }
        /* identifier */
        final Identifier tmp = identifierMapper.identifierCreateDtoToIdentifier(data);
        final User creator = userService.findByUsername(principal.getName());
        tmp.setCreator(creator);
        tmp.setCreators(List.of());
        if (data.getType().equals(IdentifierTypeDto.SUBSET)) {
            log.debug("identifier describes a subset");
            final QueryDto query = queryServiceGateway.find(data.getCid(), data.getDbid(), data, authorization);
            tmp.setQuery(query.getQuery());
            tmp.setQueryId(query.getId());
            tmp.setQueryNormalized(query.getQueryNormalized());
            tmp.setQueryHash(query.getQueryHash());
            tmp.setExecution(query.getExecution());
            tmp.setResultNumber(query.getResultNumber());
            tmp.setResultHash(query.getResultHash());
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
            entity.setRelated(new LinkedList<>());
            data.getRelatedIdentifiers()
                    .forEach(r -> {
                        final RelatedIdentifier id = identifierMapper.relatedIdentifierCreateDtoToRelatedIdentifier(r);
                        id.setIid(entity.getId());
                        id.setCreator(creator);
                        final RelatedIdentifier relatedIdentifier = relatedIdentifierRepository.save(id);
                        log.debug("identifier add related with id {}", relatedIdentifier.getId());
                        entity.getRelated().add(relatedIdentifier);
                    });
        }
        final Identifier identifier = identifierRepository.save(entity);
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
        if (identifier.getDoi() != null) {
            context.setVariable("identifierType", "doi");
            context.setVariable("identifier", identifier.getDoi());
        } else {
            context.setVariable("identifierType", "url");
            context.setVariable("identifier", endpointConfig.getWebsiteUrl() + "/pid/" + identifier.getId());
        }
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
            throw new IdentifierRequestException("Failed to find identifier");
        }
        /* subset */
        final byte[] file = queryServiceGateway.export(identifier.getContainerId(),
                identifier.getDatabaseId(), identifier.getQueryId());
        final InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(file));
        log.trace("found resource {}", resource);
        return resource;
    }

    @Override
    @Transactional
    public Identifier update(Long identifierId, IdentifierUpdateDto data) throws IdentifierNotFoundException {
        /* map */
        final Identifier old = find(identifierId);
        final Identifier entity = identifierMapper.identifierUpdateDtoToIdentifier(data);
        entity.setId(identifierId);
        entity.setCreator(old.getCreator());
        entity.getCreators().forEach(c -> {
            c.setPid(identifierId);
            c.setCreator(old.getCreator());
        });
        /* update */
        final Identifier identifier = identifierRepository.save(entity);
        log.info("Updated identifier with id {}", identifierId);
        log.trace("updated identifier {}", identifier);
        /* elastic search */
        identifierIdxRepository.save(identifierMapper.identifierToIdentifierDto(identifier));
        log.info("Updated identifier with id {} in elastic search", identifierId);
        return identifier;
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

}
