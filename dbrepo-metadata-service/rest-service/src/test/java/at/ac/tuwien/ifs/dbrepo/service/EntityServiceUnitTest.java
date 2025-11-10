package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.semantics.EntityDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.TableColumnEntityDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.OntologyNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SemanticEntityNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class EntityServiceUnitTest extends BaseTest {

    @MockitoBean
    private OntologyService ontologyService;

    @Autowired
    private EntityService entityService;

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
    @Disabled
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
