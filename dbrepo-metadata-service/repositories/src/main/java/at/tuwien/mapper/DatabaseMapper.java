package at.tuwien.mapper;

import at.tuwien.api.database.*;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryMalformedException;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.UUID;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring", uses = {ContainerMapper.class, UserMapper.class, ImageMapper.class, UserMapper.class/*, IdentifierMapper.class*/}, imports = {RandomStringUtils.class})
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
        log.debug("mapping name {} to internal name {}", data, name);
        return name;
    }

    /* keep */
    @Named("languageMapping")
    LanguageType languageTypeDtoToLanguageType(LanguageTypeDto data);

    /* keep */
    @Named("engineMapping")
    default String containerImageToEngine(ContainerImage data) {
        return data.getName() + ":" + data.getVersion();
    }

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "engine", source = "container.image", qualifiedByName = "engineMapping"),
            @Mapping(target = "image", source = "container.image"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
            @Mapping(target = "container", ignore = true),
    })
    DatabaseBriefDto databaseToDatabaseBriefDto(Database data);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "image", source = "container.image"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
            @Mapping(target = "identifier.database", ignore = true)
    })
    DatabaseDto databaseToDatabaseDto(Database data);

    @Mappings({
            @Mapping(target = "identifier.database", ignore = true)
    })
    Database databaseDtoToDatabase(DatabaseDto data);

    @Mappings({
            @Mapping(target = "internalName", expression = "java(nameToInternalName(data.getName()) + \"_\" + RandomStringUtils.randomAlphabetic(4).toLowerCase())"),
    })
    Database databaseCreateDtoToDatabase(DatabaseCreateDto data);

    default PreparedStatement userToRawCreateUserQuery(Connection connection, User data) throws QueryMalformedException {
        if (data.getMariadbPassword() == null) {
            log.error("Failed to map create user query: attribute 'mariadb_password' is empty");
            throw new QueryMalformedException("Failed to map create user query: attribute 'mariadb_password' is empty");
        }
        final StringBuilder statement = new StringBuilder("CREATE USER IF NOT EXISTS `")
                .append(data.getUsername())
                .append("`@`%` IDENTIFIED BY PASSWORD '")
                .append(data.getMariadbPassword())
                .append("';");
        log.trace("statement={}", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement userToRawUpdateUserQuery(Connection connection, User data) throws QueryMalformedException {
        if (data.getMariadbPassword() == null) {
            log.error("Failed to map create user query: attribute 'mariadb_password' is empty");
            throw new QueryMalformedException("Failed to map create user query: attribute 'mariadb_password' is empty");
        }
        final StringBuilder statement = new StringBuilder("SET PASSWORD FOR `")
                .append(data.getUsername())
                .append("`@`%` = '")
                .append(data.getMariadbPassword())
                .append("';");
        log.trace("statement={}", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement userToRawDropUserQuery(Connection connection, String username) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("DROP USER IF EXISTS `")
                .append(username)
                .append("`@`%`;");
        log.debug("raw drop user statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement databaseToRawCreateDatabaseQuery(Connection connection, Database database) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE DATABASE `")
                .append(database.getInternalName())
                .append("`;");
        log.trace("statement={}", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawGrantCreatorAccessQuery(Connection connection, String databaseName, String username,
                                                         String privileges) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT ")
                .append(privileges)
                .append(" ON ")
                .append(databaseName)
                .append(".* TO `")
                .append(username)
                .append("`@`%`;");
        log.trace("statement={}", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawRevokeUserAccessQuery(Connection connection, String username) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("REVOKE ALL PRIVILEGES ON *.* FROM `")
                .append(username)
                .append("`@`%`;");
        log.debug("raw revoke all privileges statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawGrantUserAccessQuery(Connection connection, String username, AccessTypeDto type)
            throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT ");
        switch (type) {
            case READ:
                statement.append("SELECT");
                break;
            case WRITE_ALL:
            case WRITE_OWN: // todo restrict the access right
                statement.append("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");
                break;
        }
        statement.append(" ON *.* TO `")
                .append(username)
                .append("`@`%`;");
        log.debug("raw grant {} privileges statement [{}]", type, statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawGrantUserProcedure(Connection connection, String username)
            throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT EXECUTE ON PROCEDURE `store_query` TO `")
                .append(username)
                .append("`@`%`;");
        log.debug("raw grant execute user procedure privileges statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawGrantDefaultReadonlyAccessQuery(Connection connection, String username)
            throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT SELECT ON *.* TO `")
                .append(username)
                .append("`@`%`;");
        log.trace("statement={}", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawFlushPrivileges(Connection connection) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("FLUSH PRIVILEGES;");
        log.trace("statement={}", statement);
        try {
            return connection.prepareStatement(statement.toString());
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
            log.debug("mapped create database query {}", statement);
            return pstmt;
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default Principal userDetailsDtoToPrincipal(UserDetailsDto data) {
        final Principal principal = new BasicUserPrincipal(data.getUsername());
        log.debug("mapped user details {} to principal {}", data, principal);
        return principal;
    }

    default DatabaseAccess defaultCreatorAccess(Database database, UUID userId) {
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(database.getId())
                .huserid(userId)
                .type(AccessType.WRITE_ALL)
                .build();
        log.debug("give default owner access to database with id {} to user with id {}", database.getId(), userId);
        return access;
    }

    AccessType accessTypeDtoToAccessType(AccessTypeDto data);

    AccessTypeDto accessTypeToAccessTypeDto(AccessType data);

    DatabaseAccessDto databaseAccessToDatabaseAccessDto(DatabaseAccess data);

    default DatabaseAccess databaseGiveAccessDtoToDatabaseAccess(Database database, UUID id,
                                                                 DatabaseGiveAccessDto data) {
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(database.getId())
                .huserid(id)
                .type(accessTypeDtoToAccessType(data.getType()))
                .build();
        log.debug("mapped database access {} to database access {}", data, access);
        return access;
    }

    default DatabaseAccess databaseModifyAccessDtoToDatabaseAccess(Database database, User user, DatabaseModifyAccessDto data) {
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(database.getId())
                .huserid(user.getId())
                .type(accessTypeDtoToAccessType(data.getType()))
                .build();
        log.debug("mapped database access {} to database access {}", data, access);
        return access;
    }

}
