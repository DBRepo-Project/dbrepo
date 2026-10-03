package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationAccessStatus;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccessNotFoundException;
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
import java.util.UUID;

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
    public List<ReplicationAccessDto> findAll() {
        return databaseService.findAll()
                .stream()
                .filter(this::isTargetReplica)
                .map(this::dto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplicationAccessDto> findPending() {
        return findAll()
                .stream()
                .filter(access -> access.getStatus() == ReplicationAccessStatus.PENDING)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcile(UUID databaseId) throws NotAllowedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        final Database database = databaseService.findById(databaseId);
        final boolean targetReplica = isTargetReplica(database);
        if (!targetReplica && (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty())) {
            throw new NotAllowedException("Access reconciliation requires a replicated database");
        }
        final var accesses = database.getAccesses().stream()
                .filter(access -> !isReplicationUser(access.getUsername()))
                .toList();
        for (var access : accesses) {
            final String username = access.getUsername();
            if (targetReplica) {
                restrictAccess(database, username);
            } else {
                try {
                    accessService.update(database, username, AccessTypeDto.valueOf(access.getType().name()));
                } catch (AccessNotFoundException e) {
                    throw new DataServiceException("Failed to reconcile primary access for " + username, e);
                }
            }
        }
        log.info("Reconciled SQL access for {} users of replicated database {}", accesses.size(), database.getId());
    }

    private void restrictAccess(Database database, String username) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        try {
            accessService.update(database, username, AccessTypeDto.READ);
        } catch (AccessNotFoundException e) {
            throw new DataServiceException("Failed to restrict replica access for " + username, e);
        }
    }

    private Database apply(Database database, ReplicationOwnerDto originOwner, UserDto localOwner)
            throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        final boolean hasAccess = database.getAccesses()
                .stream()
                .anyMatch(access -> access.getUsername().equals(localOwner.getUsername()));
        if (!hasAccess) {
            accessService.create(database, localOwner.getUsername(), AccessTypeDto.READ);
        } else {
            restrictAccess(database, localOwner.getUsername());
        }
        final String previousUsername = database.getReplicationLocalUsername();
        if (previousUsername != null && !previousUsername.equals(localOwner.getUsername())) {
            try {
                accessService.delete(database, previousUsername);
            } catch (AccessNotFoundException e) {
                log.debug("Previous local owner {} no longer has access to replicated database {}",
                        previousUsername, database.getId());
            }
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
        return ReplicationSites.isReplica(database.getCreationLocation(), baseUrl);
    }

    private boolean isReplicationUser(String username) {
        return username != null && username.equals(replicationUsername);
    }

}
