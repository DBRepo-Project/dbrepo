package at.tuwien;

import at.tuwien.api.document.metadata.*;
import at.tuwien.api.document.record.*;
import at.tuwien.api.user.UserDetailsDto;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static Long USER_1_ID = 1L;
    public final static String USER_1_USERNAME = "junit";

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .build();

    public final static AccessTypeDto DOCUMENT_1_RECORD_TYPE = AccessTypeDto.PUBLIC;
    public final static FileTypeDto DOCUMENT_1_FILE_TYPE = FileTypeDto.PUBLIC;

    public final static AccessTypeDto DOCUMENT_2_RECORD_TYPE = AccessTypeDto.PUBLIC;
    public final static FileTypeDto DOCUMENT_2_FILE_TYPE = FileTypeDto.RESTRICTED;

    public final static AccessOptionsDto DOCUMENT_1_ACCESS_OPTIONS = AccessOptionsDto.builder()
            .record(DOCUMENT_1_RECORD_TYPE)
            .files(DOCUMENT_1_FILE_TYPE)
            .build();

    public final static AccessOptionsDto DOCUMENT_2_ACCESS_OPTIONS = AccessOptionsDto.builder()
            .record(DOCUMENT_2_RECORD_TYPE)
            .files(DOCUMENT_2_FILE_TYPE)
            .build();

    public final static Boolean DOCUMENT_1_FILES_ENABLED = false;

    public final static Boolean DOCUMENT_2_FILES_ENABLED = true;

    public final static FilesOptionsDto DOCUMENT_1_FILES_OPTIONS = FilesOptionsDto.builder()
            .enabled(DOCUMENT_1_FILES_ENABLED)
            .build();

    public final static FilesOptionsDto DOCUMENT_2_FILES_OPTIONS = FilesOptionsDto.builder()
            .enabled(DOCUMENT_2_FILES_ENABLED)
            .build();

    public final static String DOCUMENT_1_TITLE = "Public Test-Record";
    public final static String DOCUMENT_1_RESOURCE_TYPE_TYPE = "other";
    public final static Date DOCUMENT_1_PUBLICATION_DATE = Date.from(Instant.now());

    public final static String DOCUMENT_2_TITLE = "Restricted Test-Record";
    public final static String DOCUMENT_2_RESOURCE_TYPE_TYPE = "other";
    public final static Date DOCUMENT_2_PUBLICATION_DATE = Date.from(Instant.now());

    public final static ResourceTypeDto DOCUMENT_1_RESOURCE_TYPE = ResourceTypeDto.builder()
            .id(DOCUMENT_1_RESOURCE_TYPE_TYPE)
            .build();

    public final static ResourceTypeDto DOCUMENT_2_RESOURCE_TYPE = ResourceTypeDto.builder()
            .id(DOCUMENT_2_RESOURCE_TYPE_TYPE)
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

    public final static MetadataDto DOCUMENT_2_METADATA = MetadataDto.builder()
            .title(DOCUMENT_2_TITLE)
            .resourceType(DOCUMENT_2_RESOURCE_TYPE)
            .publicationDate(DOCUMENT_2_PUBLICATION_DATE)
            .creators(List.of(CREATOR_1))
            .build();

    public final static CreateDraftDto DOCUMENT_1_CREATE_DRAFT = CreateDraftDto.builder()
            .access(DOCUMENT_1_ACCESS_OPTIONS)
            .files(DOCUMENT_1_FILES_OPTIONS)
            .metadata(DOCUMENT_1_METADATA)
            .build();

    public final static CreateDraftDto DOCUMENT_2_CREATE_DRAFT = CreateDraftDto.builder()
            .access(DOCUMENT_2_ACCESS_OPTIONS)
            .files(DOCUMENT_2_FILES_OPTIONS)
            .metadata(DOCUMENT_2_METADATA)
            .build();

    public final static String FILE_1_NAME = "mock.png";

}
