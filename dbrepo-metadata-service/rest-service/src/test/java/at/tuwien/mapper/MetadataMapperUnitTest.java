package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.identifier.*;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
public class MetadataMapperUnitTest extends AbstractUnitTest {

    private final DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
            .withZone(ZoneId.of("UTC"));

    @Autowired
    private MetadataMapper metadataMapper;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void mapMariaDbInstant_succeeds() {
        final String timestamp = "2023-01-08 08:49:29";
        final Instant compare = Instant.ofEpochSecond(1673167769);

        /* test */
        final Instant response = LocalDateTime.parse(timestamp, mariaDbFormatter)
                .atZone(ZoneId.of("UTC"))
                .toInstant();
        assertEquals(compare, response);
    }

    @Test
    public void containerEquals_fails() {

        /* test */
        assertNotEquals(CONTAINER_1, CONTAINER_2);
    }

    @Test
    public void containerEquals_identity_succeeds() {

        /* test */
        assertEquals(CONTAINER_1, CONTAINER_1);
    }

    @Test
    public void containerEquals_similar_succeeds() {
        final Container tmp = Container.builder()
                .id(CONTAINER_1_ID)
                .build();

        /* test */
        assertEquals(CONTAINER_1, tmp);
    }

    @Test
    public void identifierTypeDtoToIdentifierType_succeeds() {

        /* test */
        assertEquals(IdentifierType.VIEW, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.VIEW));
        assertEquals(IdentifierType.TABLE, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.TABLE));
        assertEquals(IdentifierType.SUBSET, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.SUBSET));
        assertEquals(IdentifierType.DATABASE, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.DATABASE));
    }

    @Test
    public void identifierCreateDtoToIdentifier_succeeds() {

        /* test */
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_1_CREATE_DTO);
        assertNotNull(response.getTitles());
        final List<IdentifierTitle> titles = response.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitle title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE, title0.getTitleType());
        final IdentifierTitle title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE, title1.getTitleType());
        assertNotNull(response.getDescriptions());
        assertEquals(1, response.getDescriptions().size());
        final List<IdentifierDescription> descriptions = response.getDescriptions();
        final IdentifierDescription description0 = descriptions.get(0);
        assertNull(description0.getId());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG, description0.getLanguage());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_TYPE, description0.getDescriptionType());
        assertNotNull(response.getCreators());
        assertEquals(1, response.getCreators().size());
        final Creator creator0 = response.getCreators().get(0);
        assertNotNull(creator0);
        assertNull(creator0.getId());
        assertEquals(IDENTIFIER_1_CREATOR_1_FIRSTNAME, creator0.getFirstname());
        assertEquals(IDENTIFIER_1_CREATOR_1_LASTNAME, creator0.getLastname());
        assertEquals(IDENTIFIER_1_CREATOR_1_NAME, creator0.getCreatorName());
        assertEquals(IDENTIFIER_1_CREATOR_1_ORCID, creator0.getNameIdentifier());
        assertEquals(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE, creator0.getNameIdentifierScheme());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION, creator0.getAffiliation());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER, creator0.getAffiliationIdentifier());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME, creator0.getAffiliationIdentifierScheme());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI, creator0.getAffiliationIdentifierSchemeUri());
        assertNotNull(response.getFunders());
        assertEquals(1, response.getFunders().size());
        assertNull(response.getRelatedIdentifiers()); /* mapstruct strategy for empty values is to set null */
    }

    @Test
    public void identifierCreateDtoToIdentifier_withDoi_succeeds() {

        /* test */
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_1_CREATE_WITH_DOI_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getViewId());
        assertNull(response.getQueryId());
        assertNull(response.getTableId());
        assertEquals(IDENTIFIER_1_DOI, response.getDoi());
        assertEquals(IDENTIFIER_1_TYPE, response.getType());
    }

    @Test
    public void identifierCreateDtoToIdentifier_subset_succeeds() {

        /* test */
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_2_CREATE_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getViewId());
        assertNull(response.getTableId());
        assertEquals(IDENTIFIER_2_QUERY_ID, response.getQueryId());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_2_TYPE, response.getType());
    }

    @Test
    public void identifierCreateDtoToIdentifier_view_succeeds() {

        /* test */
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_3_CREATE_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getQueryId());
        assertNull(response.getTableId());
        assertEquals(IDENTIFIER_3_VIEW_ID, response.getViewId());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_3_TYPE, response.getType());
    }

    @Test
    public void customDatabaseToDatabaseDto_succeeds() {

        /* test */
        final DatabaseDto response = metadataMapper.customDatabaseToDatabaseDto(DATABASE_1, USER_1);
        assertEquals(DATABASE_1_ID, response.getId());
        assertNotNull(response.getContact());
        assertEquals(USER_1_ID, response.getContact().getId());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        assertEquals(DATABASE_1_SCHEMA_PUBLIC, response.getIsSchemaPublic());
        /* identifiers formatted */
        assertEquals(4, response.getIdentifiers().size());
        final IdentifierBriefDto identifier1 = response.getIdentifiers().get(0);
        assertEquals(DATABASE_1_ID, identifier1.getDatabaseId());
        final IdentifierBriefDto identifier2 = response.getIdentifiers().get(1);
        assertEquals(DATABASE_1_ID, identifier2.getDatabaseId());
        final IdentifierBriefDto identifier3 = response.getIdentifiers().get(2);
        assertEquals(DATABASE_1_ID, identifier3.getDatabaseId());
        final IdentifierBriefDto identifier4 = response.getIdentifiers().get(3);
        assertEquals(DATABASE_1_ID, identifier4.getDatabaseId());
        /* Table 1 formatted */
        final TableBriefDto table0 = response.getTables().get(0);
        assertEquals(TABLE_1_ID, table0.getId());
        assertEquals(TABLE_1_NAME, table0.getName());
        assertEquals(TABLE_1_INTERNAL_NAME, table0.getInternalName());
        assertEquals(TABLE_1_DESCRIPTION, table0.getDescription());
        assertEquals(DATABASE_1_ID, table0.getDatabaseId());
        assertEquals(TABLE_1_SCHEMA_PUBLIC, table0.getIsSchemaPublic());
        /* Table 2 formatted */
        final TableBriefDto table1 = response.getTables().get(1);
        assertEquals(TABLE_2_ID, table1.getId());
        assertEquals(TABLE_2_NAME, table1.getName());
        assertEquals(TABLE_2_INTERNALNAME, table1.getInternalName());
        assertEquals(TABLE_2_DESCRIPTION, table1.getDescription());
        assertEquals(DATABASE_1_ID, table1.getDatabaseId());
        assertEquals(TABLE_2_SCHEMA_PUBLIC, table1.getIsSchemaPublic());
        /* Table 3 formatted */
        final TableBriefDto table2 = response.getTables().get(2);
        assertEquals(TABLE_3_ID, table2.getId());
        assertEquals(TABLE_3_NAME, table2.getName());
        assertEquals(TABLE_3_INTERNALNAME, table2.getInternalName());
        assertEquals(TABLE_3_DESCRIPTION, table2.getDescription());
        assertEquals(DATABASE_1_ID, table2.getDatabaseId());
        assertEquals(TABLE_3_SCHEMA_PUBLIC, table2.getIsSchemaPublic());
        /* Table 4 formatted */
        final TableBriefDto table3 = response.getTables().get(3);
        assertEquals(TABLE_4_ID, table3.getId());
        assertEquals(TABLE_4_NAME, table3.getName());
        assertEquals(TABLE_4_INTERNALNAME, table3.getInternalName());
        assertEquals(TABLE_4_DESCRIPTION, table3.getDescription());
        assertEquals(DATABASE_1_ID, table3.getDatabaseId());
        assertEquals(TABLE_4_SCHEMA_PUBLIC, table3.getIsSchemaPublic());
    }

    public static Stream<Arguments> nameToInternalName_parameters() {
        return Stream.of(
                Arguments.arguments("dash_minus", "OE/NO-027", "oe_no_027"),
                Arguments.arguments("percent", "OE%NO-027", "oe_no_027"),
                Arguments.arguments("umlaut", "OE/NÖ-027", "oe_no__027"),
                Arguments.arguments("dot", "OE.NO-027", "oe_no_027")
        );
    }

    @ParameterizedTest
    @MethodSource("nameToInternalName_parameters")
    public void nameToInternalName_succeeds(String name, String request, String compare) {

        /* test */
        final String response = metadataMapper.nameToInternalName(request);
        assertEquals(compare, response);
    }

    @Test
    public void userEquals_fails() {

        /* test */
        assertNotEquals(USER_1_DTO, USER_2_DTO);
    }

    @Test
    public void userEquals_identity_succeeds() {

        /* test */
        assertEquals(USER_1_DTO, USER_1_DTO);
    }

    @Test
    public void userEquals_similar_succeeds() {
        final UserDto tmp = UserDto.builder()
                .id(USER_1_ID)
                .build();

        /* test */
        assertEquals(USER_1_DTO, tmp);
    }

    @Test
    public void userToUserBriefDto_succeeds() {

        /* test */
        final UserBriefDto response = metadataMapper.userToUserBriefDto(USER_1);
        assertEquals(USER_1_NAME, response.getName());
        assertEquals(USER_1_NAME + " — @" + USER_1_USERNAME, response.getQualifiedName());
    }

    @Test
    public void userToUserDto_succeeds() {

        /* test */
        final UserDto response = metadataMapper.userToUserDto(USER_1);
        assertEquals(USER_1_NAME, response.getName());
        assertEquals(USER_1_NAME + " — @" + USER_1_USERNAME, response.getQualifiedName());
    }

    @Test
    public void viewToViewDto_succeeds() {

        /* test */
        final ViewDto response = metadataMapper.viewToViewDto(VIEW_1);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_DATABASE_ID, response.getVdbid());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertNotNull(response.getDatabase());
        assertEquals(VIEW_1_DATABASE_ID, response.getDatabase().getId());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        assertEquals(VIEW_1_QUERY_HASH, response.getQueryHash());
        assertNotNull(response.getIdentifiers());
        assertEquals(1, response.getIdentifiers().size());
        final IdentifierDto identifier0 = response.getIdentifiers().get(0);
        assertEquals(IDENTIFIER_3_ID, identifier0.getId());
        assertEquals(VIEW_1_DATABASE_ID, identifier0.getDatabaseId());
        assertEquals(VIEW_1_ID, identifier0.getViewId());
        assertEquals(VIEW_1_QUERY, identifier0.getQuery());
        assertEquals(VIEW_1_QUERY_HASH, identifier0.getQueryHash());
    }

}
