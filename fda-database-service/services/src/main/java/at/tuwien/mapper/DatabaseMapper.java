package at.tuwien.mapper;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.querystore.Query;
import org.apache.http.auth.BasicUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring", uses = {ContainerMapper.class})
public interface DatabaseMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseMapper.class);

    @Mappings({
            @Mapping(target = "name", source = "internalName")
    })
    CreateVirtualHostDto databaseToCreateVirtualHostDto(Database data);

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

    LanguageType languageTypeDtoToLanguageType(LanguageTypeDto data);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "engine", expression = "java(data.getContainer().getImage().getRepository()+\":\"+data.getContainer().getImage().getTag())"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
    })
    DatabaseBriefDto databaseToDatabaseBriefDto(Database data);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "image", source = "container.image"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm")
    })
    DatabaseDto databaseToDatabaseDto(Database data);

    default PreparedStatement databaseToRawCreateDatabaseQuery(Connection connection, Database database) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE DATABASE `")
                .append(database.getInternalName())
                .append("`;");
        log.trace("raw create statement [{}]", statement);try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement imageToRawGrantReadonlyAccessQuery(Connection connection) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT SELECT ON *.* TO `mariadb`@`%`;");
        log.trace("raw grant readonly statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement databaseToRawDeleteDatabaseQuery(Connection connection, Database database) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("DROP DATABASE `")
                .append(database.getInternalName())
                .append("`;");
        log.trace("raw grant readonly statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default Principal userDetailsDtoToPrincipal(UserDetailsDto data) {
        return new BasicUserPrincipal(data.getUsername());
    }

}
