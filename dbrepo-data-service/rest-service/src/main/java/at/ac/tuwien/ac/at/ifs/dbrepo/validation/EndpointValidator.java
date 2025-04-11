package at.ac.tuwien.ac.at.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ac.at.ifs.dbrepo.endpoints.RestEndpoint;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ac.at.ifs.dbrepo.service.CacheService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Log4j2
@Component
public class EndpointValidator extends RestEndpoint {

    private final CacheService credentialService;

    @Autowired
    public EndpointValidator(CacheService credentialService) {
        this.credentialService = credentialService;
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

    public void validateSubsetParams(SubsetDto subset) throws QueryMalformedException {
        if (subset.getFilter() != null) {
            final List<FilterDto> filters = subset.getFilter();
            FilterTypeDto previous = null;
            for (int i = 0; i < filters.size(); i++) {
                final FilterDto filter = filters.get(i);
                if ((i == 0 && !filter.getType().equals(FilterTypeDto.WHERE)) ||
                        (i > 0 && !previous.equals(FilterTypeDto.WHERE) && (filter.getType().equals(FilterTypeDto.AND) || filter.getType().equals(FilterTypeDto.OR)))) {
                    log.error("Failed to validate subset: invalid specification, must be where-[(and|or)-where]");
                    throw new QueryMalformedException("Failed to validate subset: invalid specification, must be where-[(and|or)-where]");
                }
                previous = filter.getType();
            }
        }
    }

    public void validateOnlyAccess(DatabaseDto database, Principal principal, boolean writeAccessOnly)
            throws NotAllowedException, RemoteUnavailableException, MetadataServiceException {
        if (principal == null) {
            throw new NotAllowedException("No principal provided");
        }
        if (isSystem(principal)) {
            return;
        }
        final DatabaseAccessDto access = credentialService.getAccess(database.getId(), getId(principal));
        log.trace("found access: {}", access);
        if (writeAccessOnly && !(access.getType().equals(AccessTypeDto.WRITE_OWN) || access.getType().equals(AccessTypeDto.WRITE_ALL))) {
            log.error("Access not allowed: no write access");
            throw new NotAllowedException("Access not allowed: no write access");
        }
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
