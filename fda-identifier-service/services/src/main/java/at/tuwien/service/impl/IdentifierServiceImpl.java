package at.tuwien.service.impl;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.VisibilityTypeDto;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.VisibilityType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final IdentifierMapper identifierMapper;
    private final QueryServiceGateway queryServiceGateway;
    private final IdentifierRepository identifierRepository;

    @Autowired
    public IdentifierServiceImpl(UserService userService, IdentifierMapper identifierMapper,
                                 QueryServiceGateway queryServiceGateway, IdentifierRepository identifierRepository) {
        this.userService = userService;
        this.identifierMapper = identifierMapper;
        this.queryServiceGateway = queryServiceGateway;
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(Long containerId, Long databaseId) {
        return identifierRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(Long containerId, Long databaseId, Long queryId) throws IdentifierNotFoundException {
        final Optional<Identifier> identifier = identifierRepository.findByQid(queryId);
        if (identifier.isEmpty()) {
            log.error("Failed to find identifier with query id {}", queryId);
            throw new IdentifierNotFoundException("Failed to find identifier");
        }
        return identifier.get();
    }

    @Override
    @Transactional
    public Identifier create(Long containerId, Long databaseId, IdentifierDto data, Principal principal)
            throws IdentifierPublishingNotAllowedException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierAlreadyExistsException, UserNotFoundException {
        if (!data.getVisibility().equals(VisibilityTypeDto.SELF)) {
            log.error("Identifier must be self visible for creation");
            log.debug("identifier is not self-visible {}", data);
            throw new IdentifierPublishingNotAllowedException("Identifier not self-visible");
        }
        /* find */
        final Optional<Identifier> optional = identifierRepository.findByQid(data.getQid());
        if (optional.isPresent()) {
            log.error("Identifier already issued for database {} and query id {}", data.getDbid(), data.getQid());
            log.debug("identifier already exists similar to request {}", data);
            throw new IdentifierAlreadyExistsException("Identifier exists");
        }
        final QueryDto query = queryServiceGateway.find(data) /* check if exists */;
        log.debug("found query in query service {}", query);
        final Identifier tmp = identifierMapper.identifierDtoToIdentifier(data);
        tmp.setVisibility(identifierMapper.visibilityTypeDtoToVisibilityType(data.getVisibility()));
        final User creator = userService.findByUsername(principal.getName());
        tmp.setCreator(creator);
        tmp.setCreators(List.of());
        /* create in metadata database */
        final Identifier entity = identifierRepository.save(tmp);
        entity.setCreators(data.getCreators()
                .stream()
                .map(c -> {
                    final Creator creatorDto = identifierMapper.creatorDtoToCreator(c);
                    creatorDto.setPid(entity.getId());
                    creatorDto.setCreator(creator);
                    return creatorDto;
                })
                .collect(Collectors.toList()));
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
    @Transactional
    public Identifier update(Long containerId, Long databaseId, Long identifierId, IdentifierDto data)
            throws IdentifierNotFoundException {
        /* check */
        find(identifierId);
        /* update */
        final Identifier identifier = identifierMapper.identifierDtoToIdentifier(data);
        final Identifier entityUpdated = identifierRepository.save(identifier);
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
    }

}
