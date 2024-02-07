package at.tuwien.mapper;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.entities.database.View;
import at.tuwien.exception.QueryMalformedException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring", uses = {ContainerMapper.class, UserMapper.class, TableMapper.class})
public interface ViewMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewMapper.class);

    @Named("internalNameMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(data).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    @Mappings({
            @Mapping(target = "database.container", ignore = true),
            @Mapping(target = "database.views", ignore = true),
            @Mapping(target = "database.tables", ignore = true),
            @Mapping(target = "database.identifiers", ignore = true),
    })
    ViewDto viewToViewDto(View data);

    ViewBriefDto viewToViewBriefDto(View data);

    default PreparedStatement viewToSelectAll(Connection connection, View view, Long page, Long size) throws QueryMalformedException {
        log.debug("mapping view query, view.query={}", view.getQuery());
        final StringBuilder statement = new StringBuilder("SELECT ");
        final int[] idx = new int[]{0};
        view.getColumns()
                .forEach(c -> statement.append(idx[0]++ > 0 ? "," : "")
                        .append("`")
                        .append(c.getInternalName())
                        .append("`"));
        statement.append(" FROM `")
                .append(view.getInternalName())
                .append("`");
        /* pagination */
        if (size != null) {
            log.trace("pagination size/limit of {}", size);
            statement.append(" LIMIT ")
                    .append(size);
            if (page != null) {
                log.trace("pagination page/offset of {}", page);
                statement.append(" OFFSET ")
                        .append(page * size);
            }
        }
        statement.append(";");
        try {
            log.trace("mapped view query {} to prepared statement", statement);
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement: " + e.getMessage(), e);
        }
    }

    default PreparedStatement viewToRawDeleteViewQuery(Connection connection, View view)
            throws QueryMalformedException {
        log.debug("mapping delete view query, view.name={}", view.getName());
        final StringBuilder statement = new StringBuilder("DROP VIEW `")
                .append(nameToInternalName(view.getName()))
                .append("`;");
        try {
            log.trace("mapped delete view {} to prepared statement", view.getName());
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement: " + e.getMessage(), e);
        }
    }

    default PreparedStatement viewCreateDtoToRawCreateViewQuery(Connection connection, ViewCreateDto data)
            throws QueryMalformedException {
        log.debug("mapping create view, data={}", data);
        final StringBuilder statement = new StringBuilder("CREATE VIEW `")
                .append(nameToInternalName(data.getName()))
                .append("` AS (")
                .append(data.getQuery())
                .append(")");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped create view {} to prepared statement {}", data.getName(), pstmt);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement: " + e.getMessage(), e);
        }
    }

}
