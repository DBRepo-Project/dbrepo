package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierType;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        final Identifier response = metadataMapper.identifierSaveDtoToIdentifier(
                metadataMapper.createIdentifierDtoToIdentifierSaveDto(IDENTIFIER_1_CREATE_WITH_DOI_DTO));
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
        final Identifier response = metadataMapper.identifierSaveDtoToIdentifier(
                metadataMapper.createIdentifierDtoToIdentifierSaveDto(IDENTIFIER_2_CREATE_DTO));
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

    /**
     * @implNote Test does not cover the {ViewDto#owner} property.
     */
    @Test
    public void identifierToIdentifierDto_succeeds() {

        /* test */
        final IdentifierDto id1 = metadataMapper.identifierToIdentifierDto(IDENTIFIER_1);
        id1.setOwner(USER_1_BRIEF_DTO);
        assertEquals(IDENTIFIER_1_DTO, id1);
        final IdentifierDto id2 = metadataMapper.identifierToIdentifierDto(IDENTIFIER_2);
        id2.setOwner(USER_1_BRIEF_DTO);
        assertEquals(IDENTIFIER_2_DTO, id2);
        final IdentifierDto id3 = metadataMapper.identifierToIdentifierDto(IDENTIFIER_3);
        id3.setOwner(USER_1_BRIEF_DTO);
        assertEquals(IDENTIFIER_3_DTO, id3);
        final IdentifierDto id4 = metadataMapper.identifierToIdentifierDto(IDENTIFIER_4);
        id4.setOwner(USER_1_BRIEF_DTO);
        assertEquals(IDENTIFIER_4_DTO, id4);
        final IdentifierDto id5 = metadataMapper.identifierToIdentifierDto(IDENTIFIER_5);
        id5.setOwner(USER_2_BRIEF_DTO);
        assertEquals(IDENTIFIER_5_DTO, id5);
        final IdentifierDto id6 = metadataMapper.identifierToIdentifierDto(IDENTIFIER_6);
        id6.setOwner(USER_3_BRIEF_DTO);
        assertEquals(IDENTIFIER_6_DTO, id6);
        final IdentifierDto id7 = metadataMapper.identifierToIdentifierDto(IDENTIFIER_7);
        id7.setOwner(USER_4_BRIEF_DTO);
        assertEquals(IDENTIFIER_7_DTO, id7);
    }

    /**
     * @implNote Test does not cover the {#owner} property.
     */
    @Test
    public void viewToViewDto_succeeds() {

        /* test */
        final ViewDto view1 = VIEW_1_DTO.toBuilder().owner(USER_1_MINIMAL_DTO).build();
        view1.getIdentifiers().forEach(id -> id.setOwner(USER_1_MINIMAL_DTO));
        assertEquals(view1, metadataMapper.viewToViewDto(VIEW_1));
        assertEquals(VIEW_2_DTO.toBuilder().owner(USER_1_MINIMAL_DTO).build(), metadataMapper.viewToViewDto(VIEW_2));
        assertEquals(VIEW_3_DTO.toBuilder().owner(USER_1_MINIMAL_DTO).build(), metadataMapper.viewToViewDto(VIEW_3));
        assertEquals(VIEW_4_DTO.toBuilder().owner(USER_1_MINIMAL_DTO).build(), metadataMapper.viewToViewDto(VIEW_4));
        assertEquals(VIEW_5_DTO.toBuilder().owner(USER_1_MINIMAL_DTO).build(), metadataMapper.viewToViewDto(VIEW_5));
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

    @Test
    public void relatedIdentifierToDoiRelatedIdentifier_succeeds() {

        /* test */
        assertEquals(IDENTIFIER_1_DATACITE_RELATED_IDENTIFIER_1_DTO, metadataMapper.relatedIdentifierToDoiRelatedIdentifier(IDENTIFIER_1_RELATED_IDENTIFIER_1));
    }

}
