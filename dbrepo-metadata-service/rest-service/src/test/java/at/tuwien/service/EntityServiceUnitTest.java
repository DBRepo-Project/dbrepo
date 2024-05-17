package at.tuwien.service;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class EntityServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private OntologyService ontologyService;

    @Autowired
    private EntityService entityService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void findByLabel_wikidataSparql_succeeds() throws MalformedException {

        /* mock */
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4));

        /* test */
        final List<EntityDto> response = entityService.findByLabel(ONTOLOGY_2, "temperature");
        assertFalse(response.isEmpty());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found concept {}", entity0);
    }

    @Test
    public void findByUri_wikidataSparql_succeeds() throws MalformedException, OntologyNotFoundException {

        /* mock */
        when(ontologyService.find(CONCEPT_1_URI))
                .thenReturn(ONTOLOGY_1);
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4));

        /* test */
        final List<EntityDto> response = entityService.findByUri(CONCEPT_1_URI);
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found concept {}", entity0);
    }

    @Test
    public void findOneByUri_wikidataSparql_succeeds() throws MalformedException, SemanticEntityNotFoundException,
            OntologyNotFoundException {

        /* mock */
        when(ontologyService.find(CONCEPT_1_URI))
                .thenReturn(ONTOLOGY_1);
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4));

        /* test */
        final EntityDto response = entityService.findOneByUri(CONCEPT_1_URI);
        assertNotNull(response.getUri());
        log.trace("found concept {}", response);
    }

    @Test
    public void findByLabel_om2Rdf_succeeds() throws MalformedException {

        /* mock */
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4));

        /* test */
        final List<EntityDto> response = entityService.findByLabel(ONTOLOGY_1, "millimetre");
        assertFalse(response.isEmpty());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found unit {}", entity0);
    }

    @Test
    public void findByUri_om2Rdf_succeeds() throws MalformedException, OntologyNotFoundException {

        /* mock */
        when(ontologyService.find(UNIT_1_URI))
                .thenReturn(ONTOLOGY_1);
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4, ONTOLOGY_5));

        /* test */
        final List<EntityDto> response = entityService.findByUri(UNIT_1_URI);
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found unit {}", entity0);
    }

    @Test
    @Disabled("integration")
    public void suggestByTable_succeeds() throws MalformedException {

        /* mock */
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4, ONTOLOGY_5));
        when(ontologyService.findAllProcessable())
                .thenReturn(List.of(ONTOLOGY_2, ONTOLOGY_5));

        /* test */
        final List<EntityDto> response = entityService.suggestByTable(TABLE_2);
        assertEquals(1, response.size());
    }

    @Test
    public void suggestTableColumnSemantics_succeeds() throws MalformedException {

        /* mock */
        when(ontologyService.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4, ONTOLOGY_5));
        when(ontologyService.findAllProcessable())
                .thenReturn(List.of(ONTOLOGY_2, ONTOLOGY_5));

        /* test */
        final List<TableColumnEntityDto> response = entityService.suggestByColumn(TABLE_1_COLUMNS.get(0));
        assertFalse(response.isEmpty());
    }

    @Test
    public void findByUri_noRdfNoSparql_fails() throws OntologyNotFoundException {

        /* mock */
        doThrow(OntologyNotFoundException.class)
                .when(ontologyService)
                .find(anyString());

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            entityService.findByUri("http://schema.org/MedicalCondition");
        });
    }

}
