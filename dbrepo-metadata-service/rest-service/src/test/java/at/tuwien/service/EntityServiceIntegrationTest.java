package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
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
@MockAmqp
@MockListeners
@MockOpensearch
public class EntityServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private OntologyRepository ontologyRepository;

    @Autowired
    private EntityService entityService;

    @Autowired
    private LicenseRepository licenseRepository;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        ontologyRepository.saveAll(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4, ONTOLOGY_5));
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.save(USER_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setAccesses(List.of());
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void findByLabel_wikidataSparql_succeeds() throws QueryMalformedException, OntologyInvalidException {

        /* test */
        final List<EntityDto> response = entityService.findByLabel(ONTOLOGY_2, "temperature");
        assertFalse(response.isEmpty());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found concept {}", entity0);
    }

    @Test
    public void findByUri_wikidataSparql_succeeds() throws QueryMalformedException, OntologyInvalidException {

        /* test */
        final List<EntityDto> response = entityService.findByUri(ONTOLOGY_2, COLUMN_CONCEPT_PRECIPITATION_URI);
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found concept {}", entity0);
    }

    @Test
    public void findOneByUri_wikidataSparql_succeeds() throws QueryMalformedException, SemanticEntityNotFoundException, OntologyInvalidException {

        /* test */
        final EntityDto response = entityService.findOneByUri(ONTOLOGY_2, COLUMN_CONCEPT_PRECIPITATION_URI);
        assertNotNull(response.getUri());
        log.trace("found concept {}", response);
    }

    @Test
    public void findByLabel_om2Rdf_succeeds() throws QueryMalformedException, OntologyInvalidException {

        /* test */
        final List<EntityDto> response = entityService.findByLabel(ONTOLOGY_1, "millimetre");
        assertFalse(response.isEmpty());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found unit {}", entity0);
    }

    @Test
    public void findByUri_om2Rdf_succeeds() throws QueryMalformedException, OntologyInvalidException {

        /* test */
        final List<EntityDto> response = entityService.findByUri(ONTOLOGY_1, UNIT_MILLIMETRE_URI);
        assertEquals(1, response.size());
        final EntityDto entity0 = response.get(0);
        assertNotNull(entity0.getUri());
        log.trace("found unit {}", entity0);
    }

    @Test
    public void suggestTableSemantics_succeeds() throws QueryMalformedException, OntologyInvalidException,
            TableNotFoundException, DatabaseNotFoundException {

        /* test */
        final List<EntityDto> response = entityService.suggestTableSemantics(DATABASE_1_ID, TABLE_1_ID);
//        assertFalse(response.isEmpty());
    }

    @Test
    public void suggestTableColumnSemantics_succeeds() throws QueryMalformedException, OntologyInvalidException,
            TableNotFoundException, DatabaseNotFoundException, TableColumnNotFoundException {

        /* test */
        final List<TableColumnEntityDto> response = entityService.suggestTableColumnSemantics(DATABASE_1_ID, TABLE_1_ID, 1L);
        assertFalse(response.isEmpty());
    }

    @Test
    public void findByUri_noRdfNoSparql_fails() {

        /* test */
        assertThrows(OntologyInvalidException.class, () -> {
            entityService.findByUri(ONTOLOGY_4, "http://schema.org/MedicalCondition");
        });
    }

}
