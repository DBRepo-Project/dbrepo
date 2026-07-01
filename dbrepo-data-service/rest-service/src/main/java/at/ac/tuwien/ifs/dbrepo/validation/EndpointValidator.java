package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ConditionalDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.JoinDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.OrderDto;
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
import org.springframework.util.StringUtils;

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

    public void validateDataSortParams(String sortColumn, String sortDirection) throws PaginationException {
        log.trace("validate data sort params, sortColumn={}, sortDirection={}", sortColumn, sortDirection);
        if (sortColumn != null && !StringUtils.hasText(sortColumn)) {
            log.error("Failed to validate sort column, must not be blank");
            throw new PaginationException("Failed to validate sort column");
        }
        if (!StringUtils.hasText(sortColumn) && sortDirection != null) {
            log.error("Failed to validate sort column and/or sort direction, either both are present or none");
            throw new PaginationException("Failed to validate sort column and/or sort direction");
        }
        if (StringUtils.hasText(sortColumn) && sortDirection == null) {
            log.error("Failed to validate sort column and/or sort direction, either both are present or none");
            throw new PaginationException("Failed to validate sort column and/or sort direction");
        }
        if (sortDirection != null && !sortDirection.equals("asc") && !sortDirection.equals("desc")) {
            log.error("Failed to validate sort direction, must be asc or desc");
            throw new PaginationException("Failed to validate sort direction");
        }
    }

    public void validateSubsetParams(SubsetDto subset) throws QueryMalformedException {
        if (subset == null) {
            log.error("Failed to validate subset: missing subset");
            throw new QueryMalformedException("Failed to validate subset: missing subset");
        }
        if (subset.getOrders() != null) {
            for (OrderDto order : subset.getOrders()) {
                if (order == null || order.getColumnId() == null) {
                    log.error("Failed to validate subset: missing order column");
                    throw new QueryMalformedException("Failed to validate subset: missing order column");
                }
            }
        }
        if (subset.getJoins() != null) {
            for (JoinDto join : subset.getJoins()) {
                if (join == null || join.getType() == null || join.getDatasourceId() == null || join.getConditionals() == null) {
                    log.error("Failed to validate subset: incomplete join");
                    throw new QueryMalformedException("Failed to validate subset: incomplete join");
                }
                for (ConditionalDto conditional : join.getConditionals()) {
                    if (conditional == null || conditional.getColumnId() == null || conditional.getForeignColumnId() == null) {
                        log.error("Failed to validate subset: incomplete join conditional");
                        throw new QueryMalformedException("Failed to validate subset: incomplete join conditional");
                    }
                }
            }
        }
        if (subset.getFilters() != null) {
            final List<FilterDto> filters = subset.getFilters();
            boolean expectWhere = true;
            for (FilterDto filter : filters) {
                if (filter == null || filter.getType() == null) {
                    log.error("Failed to validate subset: incomplete filter");
                    throw new QueryMalformedException("Failed to validate subset: incomplete filter");
                }
                final boolean connector = filter.getType().equals(FilterTypeDto.AND) || filter.getType().equals(FilterTypeDto.OR);
                if ((expectWhere && !filter.getType().equals(FilterTypeDto.WHERE)) || (!expectWhere && !connector)) {
                    log.error("Failed to validate subset: invalid specification, must be where-[(and|or)-where] but is: {}", filter);
                    throw new QueryMalformedException("Failed to validate subset: invalid specification, must be where-[(and|or)-where]");
                }
                if (filter.getType().equals(FilterTypeDto.WHERE) && (filter.getColumnId() == null || filter.getOperatorId() == null)) {
                    log.error("Failed to validate subset: incomplete filter");
                    throw new QueryMalformedException("Failed to validate subset: incomplete filter");
                }
                expectWhere = connector;
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
