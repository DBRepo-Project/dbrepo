
package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.semantics.*;
import at.tuwien.endpoints.QueryEndpoint;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.FilterBadRequestException;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.UriMalformedException;
import at.tuwien.repository.sdb.*;
import at.tuwien.service.OntologyService;
import at.tuwien.service.QueryService;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private QueryService queryService;

    @MockBean
    private OntologyService ontologyService;

    @MockBean
    private UnitIdxRepository unitIdxRepository;

    @MockBean
    private ConceptIdxRepository conceptIdxRepository;

    @Autowired
    private QueryEndpoint queryEndpoint;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            find_generic(ONTOLOGY_2_ID, "Apache Jena", null, ONTOLOGY_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void find_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
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
    public void find_hasRoleLabel_succeeds() throws UriMalformedException, QueryMalformedException,
            OntologyNotFoundException, FilterBadRequestException {
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
    public void find_hasRoleUri_succeeds() throws UriMalformedException, QueryMalformedException,
            OntologyNotFoundException, FilterBadRequestException {
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

    public List<EntityDto> find_generic(Long ontologyId, String label, String uri, Ontology ontology, EntityDto entityDto)
            throws OntologyNotFoundException, QueryMalformedException, UriMalformedException, FilterBadRequestException {

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
            when(queryService.findByLabel(ontology, label))
                    .thenReturn(List.of(entityDto));
            when(queryService.findByUri(ontology, uri))
                    .thenReturn(List.of(entityDto));
        } else {
            when(queryService.findByLabel(ontology, label))
                    .thenReturn(List.of());
            when(queryService.findByUri(ontology, uri))
                    .thenReturn(List.of());
        }

        /* test */
        final ResponseEntity<List<EntityDto>> response = queryEndpoint.find(ontologyId, label, uri);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<EntityDto> body = response.getBody();
        assertNotNull(body);
        return body;
    }
}
