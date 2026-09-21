package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.metadata.ReplicationIdentityMappingRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationIdentityMapping;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationIdentityService;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationIdentityServiceImpl implements ReplicationIdentityService {

    private final ReplicationIdentityMappingRepository repository;
    private final UserService userService;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String localIssuer;

    public ReplicationIdentityServiceImpl(ReplicationIdentityMappingRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> resolve(ReplicationOwnerDto owner) {
        if (!complete(owner)) {
            return Optional.empty();
        }
        if (normalize(owner.getIssuer()).equals(normalize(localIssuer))) {
            try {
                return Optional.of(userService.findById(UUID.fromString(owner.getSubject())));
            } catch (IllegalArgumentException | UserNotFoundException | NotAllowedException e) {
                log.debug("Origin owner subject {} is not a local user", owner.getSubject());
            }
        }
        return repository.findByOriginSiteAndOriginIssuerAndOriginSubject(
                        normalize(owner.getSiteUrl()), normalize(owner.getIssuer()), owner.getSubject())
                .flatMap(mapping -> {
                    try {
                        return Optional.of(userService.findByUsername(mapping.getLocalUsername()));
                    } catch (UserNotFoundException | NotAllowedException e) {
                        log.warn("Mapped replication user {} no longer exists", mapping.getLocalUsername());
                        return Optional.empty();
                    }
                });
    }

    @Override
    @Transactional
    public void map(ReplicationOwnerDto owner, String localUsername) throws NotAllowedException {
        if (!complete(owner)) {
            throw new NotAllowedException("Origin owner identity is incomplete");
        }
        final String site = normalize(owner.getSiteUrl());
        final String issuer = normalize(owner.getIssuer());
        final Optional<ReplicationIdentityMapping> existing = repository
                .findByOriginSiteAndOriginIssuerAndOriginSubject(site, issuer, owner.getSubject());
        if (existing.isPresent()) {
            if (!existing.get().getLocalUsername().equals(localUsername)) {
                throw new NotAllowedException("Origin identity is already mapped to another local user");
            }
            return;
        }
        repository.save(ReplicationIdentityMapping.builder()
                .originSite(site)
                .originIssuer(issuer)
                .originSubject(owner.getSubject())
                .localUsername(localUsername)
                .build());
    }

    private boolean complete(ReplicationOwnerDto owner) {
        return owner != null && StringUtils.hasText(owner.getSiteUrl()) && StringUtils.hasText(owner.getIssuer())
                && StringUtils.hasText(owner.getSubject()) && StringUtils.hasText(owner.getUsername());
    }

    private String normalize(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

}
