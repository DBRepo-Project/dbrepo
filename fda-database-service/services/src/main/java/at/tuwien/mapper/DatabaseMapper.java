package at.tuwien.mapper;

import at.tuwien.api.database.*;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.exception.QueryMalformedException;
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

    /* keep */
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
        final String name = slug.toLowerCase(Locale.ENGLISH);
        log.trace("mapping name {} to internal name {}", data, name);
        return name;
    }

    /* keep */
    @Named("languageMapping")
    LanguageType languageTypeDtoToLanguageType(LanguageTypeDto data);

    /* keep */
    @Named("engineMapping")
    default String containerImageToEngine(ContainerImage data) {
        return data.getRepository() + ":" + data.getTag();
    }

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "engine", source = "container.image", qualifiedByName = "engineMapping"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
    })
    DatabaseBriefDto databaseToDatabaseBriefDto(Database data);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "image", source = "container.image"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm")
    })
    DatabaseDto databaseToDatabaseDto(Database data);

    @Mappings({
            @Mapping(target = "internalName", source = "name", qualifiedByName = "internalMapping"),
    })
    Database databaseCreateDtoToDatabase(DatabaseCreateDto data);

    default PreparedStatement databaseToRawCreateDatabaseQuery(Connection connection, Database database) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE DATABASE `")
                .append(database.getInternalName())
                .append("`;");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped create database query {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement imageToRawGrantReadonlyAccessQuery(Connection connection) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT SELECT ON *.* TO `mariadb`@`%`;");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped create database query {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement databaseToRawDeleteDatabaseQuery(Connection connection, Database database) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("DROP DATABASE `")
                .append(database.getInternalName())
                .append("`;");
        try {
            final PreparedStatement pstmt = connection.prepareStatement(statement.toString());
            log.trace("mapped create database query {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default Principal userDetailsDtoToPrincipal(UserDetailsDto data) {
        final Principal principal = new BasicUserPrincipal(data.getUsername());
        log.trace("mapped user details {} to principal {}", data, principal);
        return principal;
    }

}
