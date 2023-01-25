package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.IdentifierEndpoint;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.IdentifierService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

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
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private EndpointConfig endpointConfig;

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void list_anonymous_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        assertEquals(IDENTIFIER_1_TITLE, identifier.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, identifier.getDescription());
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous2_succeeds() {

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null);
        assertEquals(0, response.size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void list_researcher_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        assertEquals(IDENTIFIER_1_TITLE, identifier.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, identifier.getDescription());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void list_researcherDatabaseId_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(DATABASE_1_ID, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        assertEquals(IDENTIFIER_1_TITLE, identifier.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, identifier.getDescription());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void list_developer_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        assertEquals(IDENTIFIER_1_TITLE, identifier.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, identifier.getDescription());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void list_dataSteward_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<IdentifierDto> response = this.generic_list(null, null);
        assertEquals(1, response.size());
        final IdentifierDto identifier = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier.getId());
        assertEquals(IDENTIFIER_1_TITLE, identifier.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, identifier.getDescription());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcherDatabaseNotExists_fails() {

        /* mock */
        containerRepository.save(CONTAINER_2);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_2_DTO_REQUEST, "ABC", USER_1_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected List<IdentifierDto> generic_list(Long databaseId, Long queryId) {

        /* test */
        final ResponseEntity<List<IdentifierDto>> response = identifierEndpoint.list(databaseId, queryId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

}
