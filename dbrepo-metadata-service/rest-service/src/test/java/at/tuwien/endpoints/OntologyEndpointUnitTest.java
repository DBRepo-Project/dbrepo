package at.tuwien.endpoints;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.semantics.*;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.EntityService;
import at.tuwien.service.OntologyService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class OntologyEndpointUnitTest extends AbstractUnitTest {

    @MockBean
    private OntologyService ontologyService;

    @MockBean
    private EntityService entityService;

    @MockBean
    private UserService userService;

    @Autowired
    private OntologyEndpoint ontologyEndpoint;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_succeeds() throws OntologyNotFoundException {

        /* test */
        find_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-semantic-query"})
    public void find_noSparql_fails() throws OntologyNotFoundException {
        final UUID id = UUID.randomUUID();
        final Ontology mock = Ontology.builder()
                .id(id)
                .uri(ONTOLOGY_1_URI)
                .uriPattern(ONTOLOGY_1_URI_PATTERN)
                .sparqlEndpoint(null) // <<<
                .build();

        /* mock */
        when(ontologyService.find(id))
                .thenReturn(mock);

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            ontologyEndpoint.find(id, null, UNIT_1_URI);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_notFound_fails() {

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            find_generic(UUID.randomUUID(), null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void find_noRole_succeeds() throws OntologyNotFoundException {

        /* test */
        find_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(ONTOLOGY_1_CREATE_DTO, null, null, null, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(ONTOLOGY_1_CREATE_DTO, USER_4_PRINCIPAL, USER_4_USERNAME, USER_4, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-ontology"})
    public void create_hasRole_succeeds() throws UserNotFoundException {

        /* test */
        create_generic(ONTOLOGY_1_CREATE_DTO, USER_3_PRINCIPAL, USER_3_USERNAME, USER_3, ONTOLOGY_1);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            update_generic(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void update_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            update_generic(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"update-ontology"})
    public void update_hasRole_succeeds() throws OntologyNotFoundException {

        /* test */
        update_generic(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, ONTOLOGY_1);
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-ontology"})
    public void delete_hasRole_succeeds() throws OntologyNotFoundException {

        /* test */
        delete_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            find_generic(ONTOLOGY_2_ID, "Apache Jena", null, ONTOLOGY_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void find_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            find_generic(ONTOLOGY_2_ID, "Apache Jena", null, ONTOLOGY_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-semantic-query"})
    public void find_hasRoleInvalidParams_succeeds() {

        /* test */
        assertThrows(FilterBadRequestException.class, () -> {
            find_generic(ONTOLOGY_2_ID, "Apache Jena", "http://www.wikidata.org/entity/Q1686799", ONTOLOGY_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-semantic-query"})
    public void find_hasRoleNotOntologyUri_succeeds() {

        /* test */
        assertThrows(UriMalformedException.class, () -> {
            find_generic(ONTOLOGY_2_ID, null, "https://wikidata.org/entity/Q1686799", ONTOLOGY_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-semantic-query"})
    public void find_hasRoleLabel_succeeds() throws MalformedException, UriMalformedException, OntologyNotFoundException,
            FilterBadRequestException {
        final EntityDto entityDto = EntityDto.builder()
                .label("Apache Jena")
                .uri("http://www.wikidata.org/entity/Q1686799")
                .build();

        /* test */
        final List<EntityDto> response = find_generic(ONTOLOGY_2_ID, "Apache Jena", null, ONTOLOGY_2, entityDto);
        final EntityDto entity0 = response.get(0);
        assertEquals("Apache Jena", entity0.getLabel());
        assertEquals("http://www.wikidata.org/entity/Q1686799", entity0.getUri());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-semantic-query"})
    public void find_hasRoleUri_succeeds() throws MalformedException, UriMalformedException, OntologyNotFoundException,
            FilterBadRequestException {
        final EntityDto entityDto = EntityDto.builder()
                .label("Apache Jena")
                .uri("http://www.wikidata.org/entity/Q1686799")
                .build();

        /* test */
        final List<EntityDto> response = find_generic(ONTOLOGY_2_ID, null, "http://www.wikidata.org/entity/Q1686799", ONTOLOGY_2, entityDto);
        final EntityDto entity0 = response.get(0);
        assertEquals("Apache Jena", entity0.getLabel());
        assertEquals("http://www.wikidata.org/entity/Q1686799", entity0.getUri());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAll_generic() {

        /* mock */
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4));

        /* test */
        final ResponseEntity<List<OntologyBriefDto>> response = ontologyEndpoint.findAll();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<OntologyBriefDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(4, body.size());
    }

    public void find_generic(UUID ontologyId, Ontology ontology) throws OntologyNotFoundException {

        /* mock */
        if (ontology != null) {
            when(ontologyService.find(ontologyId))
                    .thenReturn(ontology);
        } else {
            doThrow(OntologyNotFoundException.class)
                    .when(ontologyService)
                    .find(ontologyId);
        }

        /* test */
        final ResponseEntity<OntologyDto> response = ontologyEndpoint.find(ontologyId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final OntologyDto body = response.getBody();
        assertNotNull(body);
    }

    public void create_generic(OntologyCreateDto createDto, Principal principal, String username, User user,
                               Ontology ontology) throws UserNotFoundException {

        /* mock */
        if (ontology != null) {
            when(ontologyService.create(createDto, principal))
                    .thenReturn(ontology);
        } else {
            doThrow(HibernateException.class)
                    .when(ontologyService)
                    .create(createDto, principal);
        }
        if (user != null) {
            when(userService.findByUsername(username))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findByUsername(username);
        }

        /* test */
        final ResponseEntity<OntologyDto> response = ontologyEndpoint.create(createDto, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final OntologyDto body = response.getBody();
        assertNotNull(body);
    }

    public void update_generic(UUID ontologyId, OntologyModifyDto modifyDto, Ontology ontology)
            throws OntologyNotFoundException {

        /* mock */
        when(ontologyService.find(ontologyId))
                .thenReturn(ontology);
        if (ontology != null) {
            when(ontologyService.update(ontology, modifyDto))
                    .thenReturn(ontology);
        } else {
            doThrow(OntologyNotFoundException.class)
                    .when(ontologyService)
                    .update(ontology, modifyDto);
        }

        /* test */
        final ResponseEntity<OntologyDto> response = ontologyEndpoint.update(ontologyId, modifyDto);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final OntologyDto body = response.getBody();
        assertNotNull(body);
    }

    public void delete_generic(UUID ontologyId, Ontology ontology) throws OntologyNotFoundException {

        /* mock */
        doNothing()
                .when(ontologyService)
                .delete(ontology);

        /* test */
        final ResponseEntity<?> response = ontologyEndpoint.delete(ontologyId);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public List<EntityDto> find_generic(UUID ontologyId, String label, String uri, Ontology ontology,
                                        EntityDto entityDto) throws MalformedException, UriMalformedException,
            FilterBadRequestException, OntologyNotFoundException {

        /* mock */
        if (ontology != null) {
            when(ontologyService.find(ontologyId))
                    .thenReturn(ontology);
        } else {
            doThrow(OntologyNotFoundException.class)
                    .when(ontologyService)
                    .find(ontologyId);
        }
        if (entityDto != null) {
            when(entityService.findByLabel(ontology, label))
                    .thenReturn(List.of(entityDto));
            when(entityService.findByUri(uri))
                    .thenReturn(List.of(entityDto));
        } else {
            when(entityService.findByLabel(ontology, label))
                    .thenReturn(List.of());
            when(entityService.findByUri(uri))
                    .thenReturn(List.of());
        }

        /* test */
        final ResponseEntity<List<EntityDto>> response = ontologyEndpoint.find(ontologyId, label, uri);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<EntityDto> body = response.getBody();
        assertNotNull(body);
        return body;
    }
}
