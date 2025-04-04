package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierType;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
public class MetadataMapperUnitTest extends BaseTest {

    private final DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
            .withZone(ZoneId.of("UTC"));

    @Autowired
    private MetadataMapper metadataMapper;

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
    public void identifierTypeDtoToIdentifierType_succeeds() {

        /* test */
        assertEquals(IdentifierType.VIEW, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.VIEW));
        assertEquals(IdentifierType.TABLE, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.TABLE));
        assertEquals(IdentifierType.SUBSET, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.SUBSET));
        assertEquals(IdentifierType.DATABASE, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.DATABASE));
    }

    @Test
    public void identifierCreateDtoToIdentifier_withDoi_succeeds() {

        /* test */
        final Identifier response = metadataMapper.createIdentifierDtoToIdentifier(IDENTIFIER_1_CREATE_WITH_DOI_DTO);
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
        final Identifier response = metadataMapper.createIdentifierDtoToIdentifier(IDENTIFIER_2_CREATE_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getViewId());
        assertNull(response.getTableId());
        assertEquals(QUERY_1_ID, response.getQueryId());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_2_TYPE, response.getType());
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
    public void userToUserBriefDto_succeeds() {

        /* test */
        final UserBriefDto response = metadataMapper.userToUserBriefDto(USER_1);
        assertEquals(USER_1_NAME, response.getName());
        assertEquals(USER_1_NAME + " — @" + USER_1_USERNAME, response.getQualifiedName());
    }

    @Test
    public void userToUserDto_succeeds() {

        /* test */
        assertEquals(USER_1_DTO, metadataMapper.userToUserDto(USER_1));
        assertEquals(USER_2_DTO, metadataMapper.userToUserDto(USER_2));
        assertEquals(USER_3_DTO, metadataMapper.userToUserDto(USER_3));
        assertEquals(USER_4_DTO, metadataMapper.userToUserDto(USER_4));
        assertEquals(USER_5_DTO, metadataMapper.userToUserDto(USER_5));
    }

    @Test
    public void identifierToIdentifierDto_succeeds() {

        /* test */
        assertEquals(IDENTIFIER_1_DTO, metadataMapper.identifierToIdentifierDto(IDENTIFIER_1));
        assertEquals(IDENTIFIER_2_DTO, metadataMapper.identifierToIdentifierDto(IDENTIFIER_2));
        assertEquals(IDENTIFIER_3_DTO, metadataMapper.identifierToIdentifierDto(IDENTIFIER_3));
        assertEquals(IDENTIFIER_4_DTO, metadataMapper.identifierToIdentifierDto(IDENTIFIER_4));
        assertEquals(IDENTIFIER_5_DTO, metadataMapper.identifierToIdentifierDto(IDENTIFIER_5));
        assertEquals(IDENTIFIER_6_DTO, metadataMapper.identifierToIdentifierDto(IDENTIFIER_6));
        assertEquals(IDENTIFIER_7_DTO, metadataMapper.identifierToIdentifierDto(IDENTIFIER_7));
    }

    @Test
    public void viewToViewDto_succeeds() {

        /* test */
        assertEquals(VIEW_1_DTO, metadataMapper.viewToViewDto(VIEW_1));
        assertEquals(VIEW_2_DTO, metadataMapper.viewToViewDto(VIEW_2));
        assertEquals(VIEW_3_DTO, metadataMapper.viewToViewDto(VIEW_3));
        assertEquals(VIEW_4_DTO, metadataMapper.viewToViewDto(VIEW_4));
        assertEquals(VIEW_5_DTO, metadataMapper.viewToViewDto(VIEW_5));
    }

    @Test
    public void tableToTableBriefDto_succeeds() {

        /* test */
        assertEquals(TABLE_1_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_1));
        assertEquals(TABLE_2_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_2));
        assertEquals(TABLE_3_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_3));
        assertEquals(TABLE_4_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_4));
        assertEquals(TABLE_5_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_5));
        assertEquals(TABLE_6_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_6));
        assertEquals(TABLE_7_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_7));
        assertEquals(TABLE_8_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_8));
        assertEquals(TABLE_9_BRIEF_DTO, metadataMapper.tableToTableBriefDto(TABLE_9));
    }

    @Test
    public void containerToContainerBriefDto_succeeds() {

        /* test */
        assertEquals(CONTAINER_1_BRIEF_DTO, metadataMapper.containerToContainerBriefDto(CONTAINER_1));
    }

    @Test
    public void bannerMessageToBannerMessageDto_succeeds() {

        /* test */
        assertEquals(BANNER_MESSAGE_1_DTO, metadataMapper.bannerMessageToBannerMessageDto(BANNER_MESSAGE_1));
    }

    @Test
    public void containerImageToImageBriefDto_succeeds() {

        /* test */
        assertEquals(IMAGE_1_BRIEF_DTO, metadataMapper.containerImageToImageBriefDto(IMAGE_1));
    }

    @Test
    public void containerImageToImageDto_succeeds() {

        /* test */
        assertEquals(IMAGE_1_DTO, metadataMapper.containerImageToImageDto(IMAGE_1));
    }

    @Test
    public void ontologyToOntologyBriefDto_succeeds() {

        /* test */
        assertEquals(ONTOLOGY_1_BRIEF_DTO, metadataMapper.ontologyToOntologyBriefDto(ONTOLOGY_1));
    }

    @Test
    public void ontologyToOntologyDto_succeeds() {

        /* test */
        assertEquals(ONTOLOGY_1_DTO, metadataMapper.ontologyToOntologyDto(ONTOLOGY_1));
    }

}
