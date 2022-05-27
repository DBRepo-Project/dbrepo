package at.tuwien.mapper;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.License;
import at.tuwien.entities.identifier.Identifier;
import org.apache.commons.io.IOUtils;
import org.mapstruct.Mapper;
import org.springframework.core.io.InputStreamResource;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentMapper.class);

    LanguageTypeDto languageToLanguageDto(LanguageType data);

    LicenseDto licenseToLicenseDto(License data);

    Date instantToDate(Instant data);

    default InputStreamResource identifierToInputStreamResource(Database database, Identifier data) {
        final StringBuilder builder = new StringBuilder("<resource ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xmlns=\"http://datacite.org/schema/kernel-4\" ")
                .append("xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 ")
                .append("https://schema.datacite.org/meta/kernel-4.4/metadata.xsd\">");
        builder.append("<identifier identifierType=\"DOI\"></identifier>");
        if (data.getCreators().size() == 0) {
            builder.append("<creators>");
            data.getCreators()
                    .forEach(creator -> builder.append("<creator><creatorName>")
                            .append(creator.getLastname())
                            .append(", ")
                            .append(creator.getFirstname())
                            .append("</creatorName><givenName>")
                            .append(creator.getFirstname())
                            .append("</givenName><familyName>")
                            .append(creator.getLastname())
                            .append("</familyName></creator>"));
            builder.append("</creators>");
        }
        builder.append("<titles><title xml:lang=\"en\">")
                .append(data.getTitle())
                .append("</title></titles>")
                .append("<publisher xml:lang=\"en\">")
                .append(database.getPublisher())
                .append("</publisher><publicationYear>")
                .append(database.getPublicationYear())
                .append("</publicationYear>");
        if (database.getSubjects().size() > 0) {
            builder.append("<subjects>");
            database.getSubjects()
                    .forEach(subject -> builder.append("<subject xml:lang=\"en\">")
                            .append(subject)
                            .append("</subject>"));
            builder.append("</subjects>");
        }
        builder.append("<dates><date dateType=\"Issued\">")
                .append(instantToDate(data.getCreated()))
                .append("</date><date dateType=\"Available\">")
                .append(instantToDate(data.getCreated()))
                .append("</date></dates>");
        if (database.getLanguage() != null) {
            builder.append("<language>")
                    .append(languageToLanguageDto(database.getLanguage()).name())
                    .append("</language>");
        }
        builder.append("<resourceType resourceTypeGeneral=\"Dataset\">Dataset</resourceType>");
        if (database.getLicense() != null) {
            builder.append("<rightsList><rights xml:lang=\"en-US\" schemeURI=\"https://spdx.org/licenses/\" ")
                    .append("rightsIdentifierScheme=\"SPDX\" rightsIdentifier=\"")
                    .append(database.getLicense().getIdentifier())
                    .append("\" rightsURI=\"")
                    .append(database.getLicense().getUri())
                    .append("\"/></rightsList>");
        }
        builder.append("<version>1.0</version><descriptions>")
                .append("<description xml:lang=\"en\" descriptionType=\"Abstract\">")
                .append(data.getDescription())
                .append("</description></descriptions>")
                .append("</resource>");
        log.trace("mapped identifier to xml {}", builder);
        return new InputStreamResource(IOUtils.toInputStream(builder.toString(), Charset.defaultCharset()));
    }

}
