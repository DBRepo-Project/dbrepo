package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierDescriptionDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTitleDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.endpoints.IdentifierEndpoint;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierDescription;
import at.tuwien.entities.identifier.IdentifierTitle;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private IdentifierIdxRepository identifierIdxRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        userRepository.save(USER_3);
        userRepository.save(USER_4);
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        databaseRepository.save(DATABASE_2_SIMPLE);
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous_succeeds() {

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null, null);
        assertEquals(0, response.size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-identifiers"})
    public void list_hasRole_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(1, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void list_noRole_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(1, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void list_databaseId_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(DATABASE_1_ID, null, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(1, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void list_databaseIdAndType_succeeds() {

        /* mock */
        containerRepository.save(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_4_SIMPLE);
        databaseRepository.save(DATABASE_3_SIMPLE);
        databaseRepository.save(DATABASE_4_SIMPLE);
        identifierRepository.save(IDENTIFIER_1);
        identifierRepository.save(IDENTIFIER_2);
        identifierRepository.save(IDENTIFIER_3);
        identifierRepository.save(IDENTIFIER_4);

        /* test */
        final List<IdentifierDto> response = this.generic_list(DATABASE_4_ID, null, IdentifierTypeDto.DATABASE);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(0, identifier.getTitles().size());
        assertEquals(0, identifier.getDescriptions().size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void list_subsetIdAndType_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(DATABASE_1_ID, QUERY_1_ID, IdentifierTypeDto.DATABASE);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        final List<IdentifierTitleDto> titles = identifier.getTitles();
        assertEquals(1, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        final List<IdentifierDescriptionDto> descriptions = identifier.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_2_DTO_REQUEST, "ABC", USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_accessNotExists_fails() {

        /* mock */
        containerRepository.save(CONTAINER_3_SIMPLE);
        databaseRepository.save(DATABASE_3_SIMPLE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_3_DTO_REQUEST, "ABC", USER_1_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected List<IdentifierDto> generic_list(Long databaseId, Long queryId, IdentifierTypeDto type) {

        /* test */
        final ResponseEntity<List<IdentifierDto>> response = identifierEndpoint.list(databaseId, queryId, type);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

}
