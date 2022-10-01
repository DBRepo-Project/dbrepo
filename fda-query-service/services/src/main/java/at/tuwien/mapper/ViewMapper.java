package at.tuwien.mapper;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.entities.database.View;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.QueryStoreException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.querystore.Query;
import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Column;
import java.sql.*;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring")
public interface ViewMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewMapper.class);

    @Named("internalMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.length() == 0) {
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
            @Mapping(target = "database.container", ignore = true)
    })
    ViewDto viewToViewDto(View data);

    ViewBriefDto viewToViewBriefDto(View data);

    default PreparedStatement viewCreateDtoToRawCreateViewQuery(Connection connection, ViewCreateDto data)
            throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE VIEW `v_")
                .append(nameToInternalName(data.getName()))
                .append("` AS (")
                .append(data.getQuery())
                .append(")");
        log.trace("mapped raw create view query [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.debug("Failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement viewCreateDtoToRawInsertViewQuery(Connection connection, Long databaseId, Long userId, ViewCreateDto data) throws QueryStoreException {
        final String statement = "INSERT INTO `qs_views` (`vdbid`, `created_by`, `name`, `is_public`, `is_initial_view`, `query`, `created`) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING `id`";
        try {
            final PreparedStatement ps = connection.prepareStatement(statement);
            ps.setLong(1, databaseId);
            ps.setLong(2, userId);
            ps.setString(3, "v_" + nameToInternalName(data.getName()));
            ps.setBoolean(4, data.getIsPublic());
            ps.setBoolean(5, false);
            ps.setString(6, data.getQuery());
            ps.setTimestamp(7, Timestamp.from(Instant.now()));
            return ps;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}: {}", statement, e.getMessage());
            throw new QueryStoreException("Failed to prepare statement", e);
        }
    }

}
