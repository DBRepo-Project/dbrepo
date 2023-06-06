package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.repository.mdb.OntologyRepository;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class QueryServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OntologyRepository ontologyRepository;

    @Autowired
    private QueryService queryService;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        ontologyRepository.saveAll(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4));
    }

    @Test
    public void findByLabel_wikidata_succeeds() throws QueryMalformedException {

        /* test */
        final List<EntityDto> response = queryService.findByLabel(ONTOLOGY_2, "Apache Jena");
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertEquals("Apache Jena", entity0.getLabel());
        assertNotNull(entity0.getDescription()) /* user provided */;
    }

    @Test
    @Disabled
    public void findByLabel_measurements_fails() throws QueryMalformedException {

        /* test */
        final List<EntityDto> response = queryService.findByLabel(ONTOLOGY_1, "tonne");
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertEquals(COLUMN_UNIT_TON_NAME, entity0.getLabel());
        assertNull(COLUMN_UNIT_TON_URI);
        assertNull(COLUMN_UNIT_TON_DESCRIPTION);
    }

    @Test
    public void findByLabel_dbpedia_succeeds() throws QueryMalformedException {

        /* test */
        final List<EntityDto> response = queryService.findByLabel(ONTOLOGY_5, "person");
        assertTrue(response.size() >= 1);
        final EntityDto entity0 = response.get(0);
        assertEquals("person", entity0.getLabel());
        assertNull(entity0.getDescription());
    }

    @Test
    public void findByLabel_fails() throws QueryMalformedException {

        /* test */
        final List<EntityDto> response = queryService.findByLabel(ONTOLOGY_2, "apache jena");
        assertEquals(0, response.size());
    }

    @Test
    public void findByUri_wikidata_succeeds() throws QueryMalformedException {

        /* test */
        final List<EntityDto> response = queryService.findByUri(ONTOLOGY_2, COLUMN_CONCEPT_TEMPERATURE_URI);
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_URI, entity0.getUri());
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_NAME, entity0.getLabel());
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_DESCRIPTION, entity0.getDescription());
    }

    @Test
    @Disabled
    public void findByUri_measurements_fails() throws QueryMalformedException {

        /* test */
        final List<EntityDto> response = queryService.findByUri(ONTOLOGY_1, COLUMN_UNIT_TON_URI);
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertEquals(COLUMN_UNIT_TON_URI, entity0.getUri());
        assertNull(COLUMN_UNIT_TON_NAME);
        assertNull(COLUMN_UNIT_TON_DESCRIPTION);
    }

    @Test
    public void findByUri_dbpedia_fails() throws QueryMalformedException {

        /* test */
        final List<EntityDto> response = queryService.findByUri(ONTOLOGY_5, "http://dbpedia.org/ontology/person");
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertEquals("http://dbpedia.org/ontology/person", entity0.getUri());
        assertEquals("person", entity0.getLabel());
        assertNull(entity0.getDescription());
    }
}
