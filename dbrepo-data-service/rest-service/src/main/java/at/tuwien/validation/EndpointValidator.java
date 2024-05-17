package at.tuwien.validation;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.PaginationException;
import at.tuwien.exception.QueryNotSupportedException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Component
public class EndpointValidator {

    private final QueryConfig queryConfig;

    @Autowired
    public EndpointValidator(QueryConfig queryConfig) {
        this.queryConfig = queryConfig;
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
        log.trace("sufficient write access {}", access);
    }


}
