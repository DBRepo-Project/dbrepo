package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.PaginationException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.endpoints.RestEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Slf4j
@Component
public class EndpointValidator extends RestEndpoint {

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

    public void validateOnlyAccess(Database database, Principal principal) throws NotAllowedException {
        if (principal == null) {
            throw new NotAllowedException("No principal provided");
        }
        if (isSystem(principal)) {
            log.trace("user {} is internal: no access needed", getUsername(principal));
            return;
        }
        if (database.getAccesses()
                .stream()
                .noneMatch(a -> a.getUsername().equals(getUsername(principal)))) {
            log.error("No access found for user {} to database: {}", getUsername(principal), database.getInternalName());
            throw new NotAllowedException("No access found");
        }
    }

    public void validateOnlyWriteAccess(Database database, Table table, Principal principal)
            throws NotAllowedException {
        validateOnlyAccess(database, principal);
        if (database.getAccesses()
                .stream()
                .anyMatch(a -> a.getType().equals(AccessType.WRITE_OWN) &&
                        table.getOwnedBy().equals(getUsername(principal)) &&
                        a.getUsername().equals(getUsername(principal)))) {
            return;
        }
        if (database.getAccesses()
                .stream()
                .noneMatch(a -> a.getType().equals(AccessType.WRITE_ALL) &&
                        a.getUsername().equals(getUsername(principal)))) {
            log.error("No write access found for user {} to database: {}", getUsername(principal), database.getInternalName());
            throw new NotAllowedException("No write access found");
        }
    }


}
