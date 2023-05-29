
package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.semantics.OntologyBriefDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.endpoints.OntologyEndpoint;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.service.OntologyService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class OntologyEndpointUnitTest extends BaseUnitTest {

    @Autowired
    private OntologyEndpoint ontologyEndpoint;

    @MockBean
    private OntologyService ontologyService;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
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
    @WithAnonymousUser
    public void find_notFound_fails() {

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            find_generic(99999L, null);
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
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(ONTOLOGY_1_CREATE_DTO, null, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(ONTOLOGY_1_CREATE_DTO, USER_4_PRINCIPAL, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-ontology"})
    public void create_hasRole_succeeds() {

        /* test */
        create_generic(ONTOLOGY_1_CREATE_DTO, USER_3_PRINCIPAL, ONTOLOGY_1);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            update_generic(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, null, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void update_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            update_generic(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, USER_4_PRINCIPAL, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"update-ontology"})
    public void update_hasRoleNotFound_fails() {

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            update_generic(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, USER_3_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"update-ontology"})
    public void update_hasRole_succeeds() throws OntologyNotFoundException {

        /* test */
        update_generic(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, USER_3_PRINCIPAL, ONTOLOGY_1);
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-ontology"})
    public void delete_hasRoleNotFound_fails() {

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            delete_generic(ONTOLOGY_1_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-ontology"})
    public void delete_hasRole_succeeds() throws OntologyNotFoundException {

        /* test */
        delete_generic(ONTOLOGY_1_ID, ONTOLOGY_1);
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

    public void find_generic(Long ontologyId, Ontology ontology) throws OntologyNotFoundException {

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

    public void create_generic(OntologyCreateDto createDto, Principal principal, Ontology ontology) {

        /* mock */
        if (ontology != null) {
            when(ontologyService.create(createDto))
                    .thenReturn(ontology);
        } else {
            doThrow(HibernateException.class)
                    .when(ontologyService)
                    .create(createDto);
        }

        /* test */
        final ResponseEntity<OntologyDto> response = ontologyEndpoint.create(createDto, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final OntologyDto body = response.getBody();
        assertNotNull(body);
    }

    public void update_generic(Long ontologyId, OntologyModifyDto modifyDto, Principal principal, Ontology ontology)
            throws OntologyNotFoundException {

        /* mock */
        if (ontology != null) {
            when(ontologyService.update(ontologyId, modifyDto))
                    .thenReturn(ontology);
        } else {
            doThrow(OntologyNotFoundException.class)
                    .when(ontologyService)
                    .update(ontologyId, modifyDto);
        }

        /* test */
        final ResponseEntity<OntologyDto> response = ontologyEndpoint.update(ontologyId, modifyDto, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final OntologyDto body = response.getBody();
        assertNotNull(body);
    }

    public void delete_generic(Long ontologyId, Ontology ontology) throws OntologyNotFoundException {

        /* mock */
        if (ontology != null) {
            doNothing()
                    .when(ontologyService)
                    .delete(ontologyId);
        } else {
            doThrow(OntologyNotFoundException.class)
                    .when(ontologyService)
                    .delete(ontologyId);
        }

        /* test */
        final ResponseEntity<?> response = ontologyEndpoint.delete(ontologyId);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }
}
