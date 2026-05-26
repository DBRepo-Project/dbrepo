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
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Slf4j
@Component
public class EndpointValidator {

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
        if (subset.getFilters() != null) {
            final List<FilterDto> filters = subset.getFilters();
            boolean expectWhere = true;
            for (FilterDto filter : filters) {
                if (filter.getType() == null ||
                        (expectWhere && !filter.getType().equals(FilterTypeDto.WHERE)) ||
                        (!expectWhere && filter.getType().equals(FilterTypeDto.WHERE))) {
                    log.error("Failed to validate subset: invalid specification, must be where-[(and|or)-where] but is: {}", filter);
                    throw new QueryMalformedException("Failed to validate subset: invalid specification, must be where-[(and|or)-where]");
                }
                expectWhere = filter.getType().equals(FilterTypeDto.AND) || filter.getType().equals(FilterTypeDto.OR);
            }
            if (expectWhere && !filters.isEmpty()) {
                log.error("Failed to validate subset: invalid specification, must be where-[(and|or)-where] but ends with a connector");
                throw new QueryMalformedException("Failed to validate subset: invalid specification, must be where-[(and|or)-where]");
            }
        }
    }

    public void validateOnlyAccess(Database database, Principal principal) throws NotAllowedException {
        if (principal == null) {
            throw new NotAllowedException("No principal provided");
        }
        if (AuthUtil.isSystem(principal)) {
            log.trace("user {} is internal: no access needed", AuthUtil.getUsername(principal));
            return;
        }
        if (database.getAccesses()
                .stream()
                .noneMatch(a -> a.getUsername().equals(AuthUtil.getUsername(principal)))) {
            log.error("No access found for user {} to database: {}", AuthUtil.getUsername(principal), database.getInternalName());
            throw new NotAllowedException("No access found");
        }
    }

    public void validateOnlyWriteAccess(Database database, Table table, Principal principal)
            throws NotAllowedException {
        validateOnlyAccess(database, principal);
        if (database.getAccesses()
                .stream()
                .anyMatch(a -> a.getType().equals(AccessType.WRITE_OWN) &&
                        table.getOwnedBy().equals(AuthUtil.getUsername(principal)) &&
                        a.getUsername().equals(AuthUtil.getUsername(principal)))) {
            return;
        }
        if (database.getAccesses()
                .stream()
                .noneMatch(a -> a.getType().equals(AccessType.WRITE_ALL) &&
                        a.getUsername().equals(AuthUtil.getUsername(principal)))) {
            log.error("No write access found for user {} to database: {}", AuthUtil.getUsername(principal), database.getInternalName());
            throw new NotAllowedException("No write access found");
        }
    }


}
