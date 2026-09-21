package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationAccessStatus;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationAccessService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationIdentityService;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ReplicationAccessServiceImpl implements ReplicationAccessService {

    private final DatabaseService databaseService;
    private final AccessService accessService;
    private final UserService userService;
    private final ReplicationIdentityService identityService;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    @Value("${dbrepo.replication.username}")
    private String replicationUsername;

    public ReplicationAccessServiceImpl(DatabaseService databaseService, AccessService accessService,
                                        UserService userService, ReplicationIdentityService identityService) {
        this.databaseService = databaseService;
        this.accessService = accessService;
        this.userService = userService;
        this.identityService = identityService;
    }

    @Override
    @Transactional
    public Database initialize(Database database, ReplicationOwnerDto owner) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        database = databaseService.modifyReplicationAccess(database, owner, ReplicationAccessStatus.PENDING, null);
        final Optional<UserDto> localOwner = identityService.resolve(owner);
        if (localOwner.isEmpty()) {
            return database;
        }
        if (isReplicationUser(localOwner.get().getUsername())) {
            log.warn("Refusing to assign replicated database {} to the technical replication user", database.getId());
            return database;
        }
        try {
            return apply(database, owner, localOwner.get());
        } catch (DataServiceException | DataServiceConnectionException | DatabaseNotFoundException
                 | SearchServiceException | SearchServiceConnectionException e) {
            log.error("Failed to grant local access for replicated database {}: {}", database.getId(),
                    e.getMessage(), e);
            return database;
        }
    }

    @Override
    @Transactional
    public ReplicationAccessDto map(Database database, String localUsername) throws UserNotFoundException,
            NotAllowedException, DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        if (!isTargetReplica(database)) {
            throw new NotAllowedException("Replication access can only be mapped for a target replica");
        }
        final UserDto localOwner = userService.findByUsername(localUsername);
        if (isReplicationUser(localOwner.getUsername())) {
            throw new NotAllowedException("The technical replication user cannot own a database");
        }
        final ReplicationOwnerDto originOwner = owner(database);
        if (originOwner != null) {
            identityService.map(originOwner, localUsername);
        }
        return dto(apply(database, originOwner, localOwner));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplicationAccessDto> findPending() {
        return databaseService.findAll()
                .stream()
                .filter(this::isTargetReplica)
                .filter(database -> database.getReplicationAccessStatus() == null
                        || database.getReplicationAccessStatus() == ReplicationAccessStatus.PENDING)
                .map(this::dto)
                .toList();
    }

    private Database apply(Database database, ReplicationOwnerDto originOwner, UserDto localOwner)
            throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final boolean hasAccess = database.getAccesses()
                .stream()
                .anyMatch(access -> access.getUsername().equals(localOwner.getUsername()));
        if (!hasAccess) {
            accessService.create(database, localOwner.getUsername(), AccessTypeDto.WRITE_ALL);
        }
        return databaseService.modifyReplicationAccess(database, originOwner, ReplicationAccessStatus.MAPPED,
                localOwner.getUsername());
    }

    private ReplicationAccessDto dto(Database database) {
        return ReplicationAccessDto.builder()
                .databaseId(database.getId())
                .databaseName(database.getName())
                .creationLocation(database.getCreationLocation())
                .originOwner(owner(database))
                .status(database.getReplicationAccessStatus() == null
                        ? ReplicationAccessStatus.PENDING : database.getReplicationAccessStatus())
                .localUsername(database.getReplicationLocalUsername())
                .build();
    }

    private ReplicationOwnerDto owner(Database database) {
        if (database.getOriginOwnerSite() == null || database.getOriginOwnerIssuer() == null
                || database.getOriginOwnerSubject() == null || database.getOriginOwnerUsername() == null) {
            return null;
        }
        return ReplicationOwnerDto.builder()
                .siteUrl(database.getOriginOwnerSite())
                .issuer(database.getOriginOwnerIssuer())
                .subject(database.getOriginOwnerSubject())
                .username(database.getOriginOwnerUsername())
                .build();
    }

    private boolean isTargetReplica(Database database) {
        return database.getCreationLocation() != null
                && !normalize(database.getCreationLocation()).equals(normalize(baseUrl));
    }

    private boolean isReplicationUser(String username) {
        return username != null && username.equals(replicationUsername);
    }

    private String normalize(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

}
