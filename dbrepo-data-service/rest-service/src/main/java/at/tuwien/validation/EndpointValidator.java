package at.tuwien.validation;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.endpoints.RestEndpoint;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Component
public class EndpointValidator extends RestEndpoint {

    private final QueryConfig queryConfig;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public EndpointValidator(QueryConfig queryConfig, MetadataServiceGateway metadataServiceGateway) {
        this.queryConfig = queryConfig;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    public void validateDataParams(Long page, Long size) throws PaginationException {
        log.trace("validate data params, page={}, size={}", page, size);
        if ((page == null && size != null) || (page != null && size == null)) {
            log.error("Failed to validate page and/or size number, either both are present or none");
            throw new PaginationException("Failed to validate page and/or size number");
        }
        if (page != null && page < 0) {
            log.error("Failed to validate page number, is lower than zero");
            throw new PaginationException("Failed to validate page number");
        }
        if (size != null && size <= 0) {
            log.error("Failed to validate size number, is lower or equal than zero");
            throw new PaginationException("Failed to validate size number");
        }
    }

    public void validateOnlyPrivateSchemaAccess(DatabaseDto database, Principal principal)
            throws NotAllowedException, RemoteUnavailableException, MetadataServiceException {
        validateOnlyPrivateSchemaAccess(database, principal, false);
    }

    public void validateOnlyPrivateSchemaAccess(DatabaseDto database, Principal principal,
                                                boolean writeAccessOnly) throws NotAllowedException,
            RemoteUnavailableException, MetadataServiceException {
        if (database.getIsSchemaPublic()) {
            log.trace("database schema with id {} is public: no access needed", database.getId());
            return;
        }
        validateOnlyAccess(database, principal, writeAccessOnly);
    }

    public void validateOnlyPrivateSchemaHasRole(DatabaseDto database, Principal principal, String role)
            throws NotAllowedException {
        if (database.getIsSchemaPublic()) {
            log.trace("database with id {} has public schema: no access needed", database.getId());
            return;
        }
        log.trace("database with id {} has private schema", database.getId());
        if (principal == null) {
            log.error("Access not allowed: no authorization provided");
            throw new NotAllowedException("Access not allowed: no authorization provided");
        }
        log.trace("principal: {}", principal.getName());
        if (!hasRole(principal, role)) {
            log.error("Access not allowed: role {} missing", role);
            throw new NotAllowedException("Access not allowed: role " + role + " missing");
        }
        log.trace("principal has role '{}': access granted", role);
    }

    public void validateOnlyAccess(DatabaseDto database, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, RemoteUnavailableException, MetadataServiceException {
        if (principal == null) {
            throw new NotAllowedException("No principal provided");
        }
        if (isSystem(principal)) {
            return;
        }
        final DatabaseAccessDto access = metadataServiceGateway.getAccess(database.getId(), getId(principal));
        log.trace("found access: {}", access);
        if (writeAccessOnly && !(access.getType().equals(AccessTypeDto.WRITE_OWN) || access.getType().equals(AccessTypeDto.WRITE_ALL))) {
            log.error("Access not allowed: no write access");
            throw new NotAllowedException("Access not allowed: no write access");
        }
    }

    public void validateForbiddenStatements(String query) throws QueryNotSupportedException {
        final List<String> words = new LinkedList<>();
        Arrays.stream(queryConfig.getForbiddenKeywords())
                .forEach(keyword -> {
                    final Pattern pattern = Pattern.compile("(" + keyword + ")");
                    final Matcher matcher = pattern.matcher(query);
                    final boolean found = matcher.find();
                    if (found) {
                        words.add(keyword);
                        log.debug("query contains keyword '{}' matching '{}'", keyword, matcher.group(1));
                    }
                });
        if (words.isEmpty()) {
            return;
        }
        log.error("Query contains forbidden keyword(s): {}", words);
        throw new QueryNotSupportedException("Query contains forbidden keyword(s): " + Arrays.toString(words.toArray()));
    }

    public void validateOnlyWriteOwnOrWriteAllAccess(AccessTypeDto access, UUID owner, UUID user) throws NotAllowedException {
        if (access.equals(AccessTypeDto.READ)) {
            log.error("Failed to create table data: no write access");
            throw new NotAllowedException("Failed to create table data: no write access");
        }
        if (access.equals(AccessTypeDto.WRITE_OWN) && !owner.equals(user)) {
            log.error("Failed to create table data: insufficient table write access");
            throw new NotAllowedException("Failed to create table data: insufficient table write access");
        }
        log.trace("sufficient write access {} for user {} and owner {}", access, user, owner);
    }


}
