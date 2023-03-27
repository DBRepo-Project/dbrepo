package at.tuwien.validation;

import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.PaginationException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.SortException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Component
public class EndpointValidator {

    private final QueryConfig queryConfig;

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

    public void validateDataParams(Long page, Long size, SortType sortDirection, String sortColumn)
            throws PaginationException, SortException {
        log.trace("validate data params, page={}, size={}, sortDirection={}, sortColumn={}", page, size,
                sortDirection, sortColumn);
        validateDataParams(page, size);
        if ((sortDirection == null && sortColumn != null) || (sortDirection != null && sortColumn == null)) {
            log.error("Failed to validate sort direction and/or sort column, either both are present or none");
            throw new SortException("Failed to validate sort direction and/or sort column");
        }
    }

    /**
     * Do not allow aggregate functions and comments
     * https://mariadb.com/kb/en/aggregate-functions/
     */
    public void validateForbiddenStatements(ExecuteStatementDto data) throws QueryMalformedException {
        final List<String> words = new LinkedList<>();
        Arrays.stream(queryConfig.getNotSupportedKeywords())
                .forEach(keyword -> {
                    final Pattern pattern = Pattern.compile(keyword);
                    final Matcher matcher = pattern.matcher(data.getStatement());
                    final boolean found = matcher.find();
                    if (found) {
                        words.add(keyword);
                    }
                });
        if (words.size() == 0) {
            return;
        }
        log.error("Query contains forbidden keyword(s): {}", words);
        throw new QueryMalformedException("Query contains forbidden keyword(s): " + Arrays.toString(words.toArray()));
    }

}
