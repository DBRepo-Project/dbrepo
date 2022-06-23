package at.tuwien;

import at.tuwien.api.document.metadata.*;
import at.tuwien.api.document.record.*;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String USER_1_USERNAME = "junit";

    public final static AccessTypeDto DOCUMENT_1_RECORD_TYPE = AccessTypeDto.PUBLIC;
    public final static FileTypeDto DOCUMENT_1_FILE_TYPE = FileTypeDto.PUBLIC;

    public final static AccessOptionsDto DOCUMENT_1_ACCESS_OPTIONS = AccessOptionsDto.builder()
            .record(DOCUMENT_1_RECORD_TYPE)
            .files(DOCUMENT_1_FILE_TYPE)
            .build();

    public final static Boolean DOCUMENT_1_FILES_ENABLED = true;

    public final static FilesOptionsDto DOCUMENT_1_FILES_OPTIONS = FilesOptionsDto.builder()
            .enabled(DOCUMENT_1_FILES_ENABLED)
            .build();

    public final static String DOCUMENT_1_TITLE = "Test Draft";
    public final static String DOCUMENT_1_RESOURCE_TYPE_TYPE = "other";
    public final static Date DOCUMENT_1_PUBLICATION_DATE = Date.from(Instant.now());

    public final static ResourceTypeDto DOCUMENT_1_RESOURCE_TYPE = ResourceTypeDto.builder()
            .id(DOCUMENT_1_RESOURCE_TYPE_TYPE)
            .build();

    public final static String IDENTIFIER_1_IDENTIFIER = "0000-0003-4216-302X";
    public final static IdentifierTypeDto IDENTIFIER_1_TYPE = IdentifierTypeDto.ORCID;

    public final static IdentifierDto IDENTIFIER_1 = IdentifierDto.builder()
            .identifier(IDENTIFIER_1_IDENTIFIER)
            .scheme(IDENTIFIER_1_TYPE)
            .build();

    public final static String PERSON_1_GIVEN_NAME = "Martin";
    public final static String PERSON_1_FAMILY_NAME = "Weise";
    public final static String PERSON_1_NAME = "Weise, Martin";
    public final static PersonOrOrgTypeDto PERSON_1_TYPE = PersonOrOrgTypeDto.PERSONAL;

    public final static PersonOrOrganizationDto PERSON_1 = PersonOrOrganizationDto.builder()
            .givenName(PERSON_1_GIVEN_NAME)
            .familyName(PERSON_1_FAMILY_NAME)
            .name(PERSON_1_NAME)
            .type(PERSON_1_TYPE)
            .identifiers(List.of(IDENTIFIER_1))
            .build();

    public final static AffiliationDto AFFILIATION_1 = AffiliationDto.builder()
            .name("TU Wien")
            .build();

    public final static CreatorDto CREATOR_1 = CreatorDto.builder()
            .personOrOrganization(PERSON_1)
            .affiliations(List.of(AFFILIATION_1))
            .build();

    public final static MetadataDto DOCUMENT_1_METADATA = MetadataDto.builder()
            .title(DOCUMENT_1_TITLE)
            .resourceType(DOCUMENT_1_RESOURCE_TYPE)
            .publicationDate(DOCUMENT_1_PUBLICATION_DATE)
            .creators(List.of(CREATOR_1))
            .build();

    public final static CreateDraftDto DOCUMENT_1_CREATE_DRAFT = CreateDraftDto.builder()
            .access(DOCUMENT_1_ACCESS_OPTIONS)
            .files(DOCUMENT_1_FILES_OPTIONS)
            .metadata(DOCUMENT_1_METADATA)
            .build();

}
