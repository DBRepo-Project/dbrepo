package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
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
        assertEquals(IDENTIFIER_1, response);
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
        assertEquals(IDENTIFIER_3, response);
    }

    @Test
    public void customDatabaseToDatabaseDto_succeeds() {

        /* test */
        final DatabaseDto response = metadataMapper.customDatabaseToDatabaseDto(DATABASE_1);
        assertEquals(DATABASE_1_DTO, response);
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
        final UserDto response = metadataMapper.userToUserDto(USER_1);
        assertEquals(USER_1_DTO, response);
    }

    @Test
    public void viewToViewDto_succeeds() {

        /* test */
        final ViewDto response = metadataMapper.viewToViewDto(VIEW_1);
        assertEquals(VIEW_1_DTO, response);
    }

}
