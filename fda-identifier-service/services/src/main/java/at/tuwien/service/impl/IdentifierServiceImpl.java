package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.VisibilityTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.identifier.VisibilityType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.mapper.DocumentMapper;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IdentifierServiceImpl implements IdentifierService {

    private final UserService userService;
    private final DocumentMapper documentMapper;
    private final DatabaseService databaseService;
    private final IdentifierMapper identifierMapper;
    private final QueryServiceGateway queryServiceGateway;
    private final IdentifierRepository identifierRepository;

    public IdentifierServiceImpl(UserService userService, DocumentMapper documentMapper,
                                 DatabaseService databaseService, IdentifierMapper identifierMapper,
                                 QueryServiceGateway queryServiceGateway, IdentifierRepository identifierRepository) {
        this.userService = userService;
        this.documentMapper = documentMapper;
        this.databaseService = databaseService;
        this.identifierMapper = identifierMapper;
        this.queryServiceGateway = queryServiceGateway;
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(Long containerId, Long databaseId) {
        return identifierRepository.findByDbid(databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(Long containerId, Long databaseId, Long queryId) throws IdentifierNotFoundException {
        final Optional<Identifier> identifier = identifierRepository.findByDbidAndQid(databaseId, queryId);
        if (identifier.isEmpty()) {
            log.error("Failed to find identifier with query id {}", queryId);
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        return identifier.get();
    }

    @Override
    @Transactional
    public Identifier create(Long containerId, Long databaseId, IdentifierCreateDto data, Principal principal,
                             String authorization)
            throws QueryNotFoundException, RemoteUnavailableException, IdentifierAlreadyExistsException,
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException {
        /* check */
        final Database database = databaseService.find(containerId, databaseId);
        if (database.getIsPublic() && !data.getVisibility().equals(VisibilityTypeDto.EVERYONE)) {
            log.error("Identifier cannot restrict the result set");
            throw new IdentifierPublishingNotAllowedException("Identifier cannot restrict the result set");
        }
        /* find */
        final Optional<Identifier> optional = identifierRepository.findByDbidAndQid(databaseId, data.getQid());
        if (optional.isPresent()) {
            log.error("Identifier already issued for database {} and query id {}", databaseId, data.getQid());
            log.debug("identifier already exists similar to request {}", data);
            throw new IdentifierAlreadyExistsException("Identifier exists");
        }
        final QueryDto query = queryServiceGateway.find(containerId, databaseId, data, authorization);
        log.debug("found query in query service {}", query);
        final Identifier tmp = identifierMapper.identifierCreateDtoToIdentifier(data);
        tmp.setCid(containerId);
        tmp.setDbid(databaseId);
        tmp.setVisibility(identifierMapper.visibilityTypeDtoToVisibilityType(data.getVisibility()));
        final User creator = userService.findByUsername(principal.getName());
        tmp.setCreator(creator);
        tmp.setCreators(List.of());
        tmp.setQuery(query.getQuery());
        tmp.setQueryNormalized(query.getQueryNormalized());
        tmp.setQueryHash(query.getQueryHash());
        tmp.setExecution(query.getExecution());
        tmp.setResultNumber(query.getResultNumber());
        tmp.setResultHash(query.getResultHash());
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
            entity.setRelatedIdentifiers(data.getRelatedIdentifiers()
                    .stream()
                    .map(r -> {
                        final RelatedIdentifier id = identifierMapper.relatedIdentifierCreateDtoToRelatedIdentifier(r);
                        id.setIdentifier(entity);
                        id.setCreator(creator);
                        return id;
                    })
                    .collect(Collectors.toList()));
        }
        final Identifier identifier = identifierRepository.save(entity);
        log.info("Created identifier with id {}", identifier.getId());
        log.debug("created identifier {}", identifier);
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
    public ExportResource exportMetadata(Long containerId, Long databaseId, Long identifierId)
            throws IdentifierNotFoundException, DatabaseNotFoundException {
        /* check */
        final Identifier identifier = find(identifierId);
        final Database database = databaseService.find(containerId, databaseId);
        /* map */
        final InputStreamResource resource = documentMapper.identifierToInputStreamResource(database, identifier);
        return ExportResource.builder()
                .filename("metadata.xml")
                .resource(resource)
                .build();
    }

    @Override
    @Transactional
    public Identifier update(Long containerId, Long databaseId, Long identifierId, IdentifierDto data)
            throws IdentifierNotFoundException {
        /* check */
        find(identifierId);
        /* update */
        final Identifier entityUpdated = identifierRepository.save(identifierMapper.identifierDtoToIdentifier(data));
        log.info("Updated identifier with id {}", identifierId);
        log.debug("updated identifier {}", entityUpdated);
        return entityUpdated;
    }

    @Override
    @Transactional
    public Identifier publish(Long containerId, Long databaseId, Long identifierId, VisibilityTypeDto visibility)
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
        log.debug("published identifier {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public void delete(Long containerId, Long databaseId, Long identifierId) throws IdentifierNotFoundException {
        /* check */
        final Identifier identifier = find(identifierId);
        /* delete */
        identifierRepository.delete(identifier);
        log.info("Deleted identifier with id {}", identifierId);
        log.debug("deleted identifier {}", identifier);
    }

}
