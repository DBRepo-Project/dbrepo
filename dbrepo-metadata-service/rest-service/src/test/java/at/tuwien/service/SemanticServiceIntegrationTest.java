package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.UnitNotFoundException;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class SemanticServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private SemanticService semanticService;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setAccesses(List.of());
        databaseRepository.save(DATABASE_1);
    }

    @Test
    @Transactional
    public void findAllConcepts_succeeds() {

        /* test */
        final List<TableColumnConcept> response = semanticService.findAllConcepts();
        assertEquals(1, response.size());
        assertTrue(response.stream().anyMatch(c -> c.getUri().equals(COLUMN_CONCEPT_PRECIPITATION_URI)));
        assertFalse(response.stream().anyMatch(c -> c.getUri().equals(COLUMN_CONCEPT_FAIR_DATA_URI)));
    }

    @Test
    @Transactional
    public void findAllUnits_succeeds() {

        /* test */
        final List<TableColumnUnit> response = semanticService.findAllUnits();
        assertEquals(1, response.size());
        assertTrue(response.stream().anyMatch(c -> c.getUri().equals(UNIT_MILLIMETRE_URI)));
        assertFalse(response.stream().anyMatch(c -> c.getUri().equals(UNIT_TONNE_URI)));
    }

    @Test
    @Transactional
    public void findUnit_succeeds() throws UnitNotFoundException {

        /* test */
        final TableColumnUnit response = semanticService.findUnit(UNIT_MILLIMETRE_URI);
        assertEquals(UNIT_MILLIMETRE_URI, response.getUri());
        assertEquals(UNIT_MILLIMETRE_NAME, response.getName());
        assertEquals(UNIT_MILLIMETRE_DESCRIPTION, response.getDescription());
    }

    @Test
    @Transactional
    public void findUnit_fails() {

        /* test */
        assertThrows(UnitNotFoundException.class, () -> {
            semanticService.findUnit("http://example.com/rdf");
        });
    }

    @Test
    @Transactional
    public void findConcept_succeeds() throws ConceptNotFoundException {

        /* test */
        final TableColumnConcept response = semanticService.findConcept(COLUMN_CONCEPT_PRECIPITATION_URI);
        assertEquals(COLUMN_CONCEPT_PRECIPITATION_URI, response.getUri());
        assertEquals(COLUMN_CONCEPT_PRECIPITATION_NAME, response.getName());
        assertEquals(COLUMN_CONCEPT_PRECIPITATION_DESCRIPTION, response.getDescription());
    }

    @Test
    @Transactional
    public void findConcept_fails() {

        /* test */
        assertThrows(ConceptNotFoundException.class, () -> {
            semanticService.findConcept("http://example.com/rdf");
        });
    }

}
