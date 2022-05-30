package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.database.Database;
import org.apache.http.auth.BasicUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.security.Principal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring", uses = {ContainerMapper.class})
public interface DatabaseMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseMapper.class);

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
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "engine", expression = "java(data.getContainer().getImage().getRepository()+\":\"+data.getContainer().getImage().getTag())"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
    })
    DatabaseBriefDto databaseToDatabaseBriefDto(Database data);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "image", source = "container.image"),
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
    })
    DatabaseDto databaseToDatabaseDto(Database data);

    default String databaseToRawCreateDatabaseQuery(Database database) {
        final String statement = "CREATE DATABASE " + database.getInternalName() + ";";
        log.trace("raw create statement [{}]", statement);
        return statement;
    }

    default String imageToRawGrantReadonlyAccessQuery() {
        final String statement = "GRANT SELECT ON *.* TO `mariadb`@`%`;";
        log.trace("raw grant readonly statement [{}]", statement);
        return statement;
    }

    default String databaseToRawDeleteDatabaseQuery(Database database) {
        final String statement = "DROP DATABASE " + database.getInternalName() + ";";
        log.trace("raw grant readonly statement [{}]", statement);
        return statement;
    }

    default Principal userDetailsDtoToPrincipal(UserDetailsDto data) {
        return new BasicUserPrincipal(data.getUsername());
    }

}
