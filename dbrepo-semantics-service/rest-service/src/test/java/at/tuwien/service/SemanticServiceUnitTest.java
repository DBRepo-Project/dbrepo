package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.*;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SemanticServiceUnitTest extends BaseUnitTest {

    @MockBean
    private UnitIdxRepository unitIdxRepository;

    @MockBean
    private ConceptIdxRepository conceptIdxRepository;

    @MockBean
    private TableColumnConceptRepository tableColumnConceptRepository;

    @MockBean
    private TableColumnUnitRepository tableColumnUnitRepository;

    @Autowired
    private SemanticService semanticService;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @Test
    public void findAllConcepts_succeeds() {

        /* mock */
        when(tableColumnConceptRepository.findAll())
                .thenReturn(List.of(COLUMN_CONCEPT_TEMPERATURE));

        /* test */
        final List<TableColumnConcept> response = semanticService.findAllConcepts();
        assertEquals(1, response.size());
        final TableColumnConcept concept0 = response.get(0);
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_URI, concept0.getUri());
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_NAME, concept0.getName());
    }

    @Test
    public void findAllUnit_succeeds() {

        /* mock */
        when(tableColumnUnitRepository.findAll())
                .thenReturn(List.of(UNIT_1));

        /* test */
        final List<TableColumnUnit> response = semanticService.findAllUnits();
        assertEquals(1, response.size());
        final TableColumnUnit unit0 = response.get(0);
        assertEquals(UNIT_1_URI, unit0.getUri());
        assertEquals(UNIT_1_NAME, unit0.getName());
        assertEquals(UNIT_1_DESCRIPTION, unit0.getDescription());
    }

    @Test
    public void saveUnit_exists_succeeds() {

        /* mock */
        when(unitIdxRepository.save(any(UnitDto.class)))
                .thenReturn(UNIT_1_DTO);
        when(tableColumnUnitRepository.save(any(TableColumnUnit.class)))
                .thenReturn(UNIT_1);

        /* test */
        final TableColumnUnit response = semanticService.saveUnit(UNIT_1_SAVE_DTO);
        assertEquals(UNIT_1_URI, response.getUri());
        assertEquals(UNIT_1_NAME, response.getName());
        assertEquals(UNIT_1_DESCRIPTION, response.getDescription());
    }

    @Test
    public void saveUnit_succeeds() {

        /* mock */
        when(unitIdxRepository.save(any(UnitDto.class)))
                .thenReturn(UNIT_2_DTO);
        when(tableColumnUnitRepository.save(any(TableColumnUnit.class)))
                .thenReturn(UNIT_2);

        /* test */
        final TableColumnUnit response = semanticService.saveUnit(UNIT_2_SAVE_DTO);
        assertEquals(UNIT_2_URI, response.getUri());
        assertEquals(UNIT_2_NAME, response.getName());
        assertEquals(UNIT_2_DESCRIPTION, response.getDescription());
    }

    @Test
    public void saveConcept_exists_succeeds() {

        /* mock */
        when(conceptIdxRepository.save(any(ConceptDto.class)))
                .thenReturn(COLUMN_CONCEPT_TEMPERATURE_DTO);
        when(tableColumnConceptRepository.save(any(TableColumnConcept.class)))
                .thenReturn(COLUMN_CONCEPT_TEMPERATURE);

        /* test */
        final TableColumnConcept response = semanticService.saveConcept(COLUMN_CONCEPT_TEMPERATURE_SAVE_DTO);
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_URI, response.getUri());
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_NAME, response.getName());
        assertEquals(COLUMN_CONCEPT_TEMPERATURE_DESCRIPTION, response.getDescription());
    }

    @Test
    public void saveConcept_succeeds() {

        /* mock */
        when(conceptIdxRepository.save(any(ConceptDto.class)))
                .thenReturn(COLUMN_CONCEPT_FAIR_DATA_DTO);
        when(tableColumnConceptRepository.save(any(TableColumnConcept.class)))
                .thenReturn(COLUMN_CONCEPT_FAIR_DATA);

        /* test */
        final TableColumnConcept response = semanticService.saveConcept(COLUMN_CONCEPT_FAIR_DATA_SAVE_DTO);
        assertEquals(COLUMN_CONCEPT_FAIR_DATA_URI, response.getUri());
        assertEquals(COLUMN_CONCEPT_FAIR_DATA_NAME, response.getName());
        assertEquals(COLUMN_CONCEPT_FAIR_DATA_DESCRIPTION, response.getDescription());
    }
}
