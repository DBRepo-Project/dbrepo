package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.identifier.IdentifierDescriptionDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTitleDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class IdentifierEndpointIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        TABLE_8.setColumns(TABLE_8_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4));
        licenseRepository.save(LICENSE_1);
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2, CONTAINER_3, CONTAINER_4));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3, DATABASE_4));
    }

    @Test
    @Transactional
    @WithAnonymousUser
    public void list_anonymous_succeeds() {

        /* test */
        final List<IdentifierDto> response = generic_list(null, null, null, null, null);
        assertEquals(7, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE_DTO, title0.getTitleType());
        final IdentifierTitleDto title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG_DTO, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE_DTO, title1.getTitleType());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-identifiers"})
    public void list_hasRole_succeeds() {

        /* test */
        final List<IdentifierDto> response = generic_list(null, null, null, null, null);
        assertEquals(7, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE_DTO, title0.getTitleType());
        final IdentifierTitleDto title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG_DTO, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE_DTO, title1.getTitleType());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_4_USERNAME)
    public void list_noRole_succeeds() {

        /* test */
        final List<IdentifierDto> response = generic_list(null, null, null, null, null);
        assertEquals(7, response.size());
        final IdentifierDto identifier = response.get(0);
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE_DTO, title0.getTitleType());
        final IdentifierTitleDto title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG_DTO, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE_DTO, title1.getTitleType());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_1_USERNAME)
    public void list_databaseId_succeeds() {

        /* test */
        final List<IdentifierDto> response = generic_list(DATABASE_1_ID, null, null, null, null);
        assertEquals(4, response.size());
        final IdentifierDto identifier = response.get(0);
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE_DTO, title0.getTitleType());
        final IdentifierTitleDto title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG_DTO, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE_DTO, title1.getTitleType());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void list_viewId_succeeds() {

        /* test */
        final List<IdentifierDto> reponse = generic_list(null, null, VIEW_1_ID, null, null);
        assertEquals(1, reponse.size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void list_viewType_succeeds() {

        /* test */
        final List<IdentifierDto> reponse = generic_list(null, null, null, null, IdentifierTypeDto.VIEW);
        assertEquals(1, reponse.size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void list_databaseIdAndType_succeeds() {

        /* test */
        final List<IdentifierDto> response = generic_list(DATABASE_1_ID, null, null, null, IdentifierTypeDto.DATABASE);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(0, identifier.getTitles().size());
        assertEquals(0, identifier.getDescriptions().size());
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_1_USERNAME)
    public void list_subsetIdAndType_succeeds() {

        /* test */
        final List<IdentifierDto> response = generic_list(DATABASE_1_ID, null, null, null, IdentifierTypeDto.DATABASE);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE_DTO, title0.getTitleType());
        final IdentifierTitleDto title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG_DTO, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE_DTO, title1.getTitleType());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_5_DTO_REQUEST, USER_4_PRINCIPAL);
        });
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_accessNotExists_fails() {

        /* mock */
        containerRepository.save(CONTAINER_3);
        databaseRepository.save(DATABASE_3);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_6_DTO_REQUEST, USER_1_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected List<IdentifierDto> generic_list(Long databaseId, Long queryId, Long viewId, Long tableId, IdentifierTypeDto type) {

        /* test */
        final ResponseEntity<List<IdentifierDto>> response = identifierEndpoint.list(databaseId, queryId, viewId, tableId, type);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

}
