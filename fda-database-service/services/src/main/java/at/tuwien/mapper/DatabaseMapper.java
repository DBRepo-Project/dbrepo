package at.tuwien.mapper;

import at.tuwien.api.database.*;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryMalformedException;
import org.apache.http.auth.BasicUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    default User containerToPrivilegedUser(Container container) {
        final String username = container.getImage()
                .getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);
        final String password = container.getImage()
                .getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);
        return User.builder()
                .username(username)
                .databasePassword(password)
                .build();
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

    default PreparedStatement userToRawCreateUserQuery(Connection connection, User user) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE USER `")
                .append(user.getUsername())
                .append("`@`%` IDENTIFIED BY PASSWORD '")
                .append(user.getDatabasePassword())
                .append("';");
        log.trace("raw create user statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement databaseToRawCreateDatabaseQuery(Connection connection, Database database) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("CREATE DATABASE `")
                .append(database.getInternalName())
                .append("`;");
        log.trace("raw create database statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default DatabaseGiveAccessDto databaseModifyAccessToDatabaseGiveAccessDto(String username, DatabaseModifyAccessDto data) {
        return DatabaseGiveAccessDto.builder()
                .username(username)
                .type(data.getType())
                .build();
    }

    default PreparedStatement rawGrantCreatorAccessQuery(Connection connection, User user) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT ALL PRIVILEGES ON *.* TO `")
                .append(user.getUsername())
                .append("`@`%`;");
        log.trace("raw grant all privileges statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}, reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawRevokeUserAccessQuery(Connection connection, User user) throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("REVOKE ALL PRIVILEGES ON *.* TO `")
                .append(user.getUsername())
                .append("`@`%`;");
        log.trace("raw revoke all privileges statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawGrantUserAccessQuery(Connection connection, DatabaseGiveAccessDto data)
            throws QueryMalformedException {
        final StringBuilder statement = new StringBuilder("GRANT ");
        switch (data.getType()) {
            case READ:
                statement.append("SELECT");
            case WRITE_ALL:
            case WRITE_OWN: // todo restrict the access right
                statement.append("CREATE, CREATE VIEW, SELECT, INSERT, UPDATE, DELETE");
        }
        statement.append(" ON *.* TO `")
                .append(data.getUsername())
                .append("`@`%`;");
        log.trace("raw grant readonly privileges statement [{}]", statement);
        try {
            return connection.prepareStatement(statement.toString());
        } catch (SQLException e) {
            log.error("Failed to prepare statement");
            log.debug("failed to prepare statement {} reason: {}", statement, e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement", e);
        }
    }

    default PreparedStatement rawGrantDefaultReadonlyAccessQuery(Connection connection) throws QueryMalformedException {
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

    default DatabaseAccess defaultCreatorAccess(Database database, User user) {
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(database.getId())
                .huserid(user.getId())
                .type(AccessType.WRITE_ALL)
                .build();
        log.debug("give default creator access to database with id {} to user with username {}", database.getId(), user.getUsername());
        return access;
    }

    AccessType accessTypeDtoToAccessType(AccessTypeDto data);

    AccessTypeDto accessTypeToAccessTypeDto(AccessType data);

    DatabaseAccessDto databaseAccessToDatabaseAccessDto(DatabaseAccess data);

    default DatabaseAccess databaseGiveAccessDtoToDatabaseAccess(Database database, User user,
                                                                 DatabaseGiveAccessDto data) {
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(database.getId())
                .huserid(user.getId())
                .type(accessTypeDtoToAccessType(data.getType()))
                .build();
        log.trace("mapped database access {} to database access {}", data, access);
        return access;
    }

    default DatabaseAccess databaseModifyAccessDtoToDatabaseAccess(Database database, User user,
                                                                   DatabaseModifyAccessDto data) {
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(database.getId())
                .huserid(user.getId())
                .type(accessTypeDtoToAccessType(data.getType()))
                .build();
        log.trace("mapped database access {} to database access {}", data, access);
        return access;
    }

}
