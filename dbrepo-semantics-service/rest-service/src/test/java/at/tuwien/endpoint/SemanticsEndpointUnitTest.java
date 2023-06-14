
package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.endpoints.SemanticsEndpoint;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.repository.sdb.*;
import at.tuwien.service.SemanticService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SemanticsEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private SemanticService semanticService;

    @MockBean
    private UnitIdxRepository unitIdxRepository;

    @MockBean
    private ConceptIdxRepository conceptIdxRepository;

    @Autowired
    private SemanticsEndpoint semanticsEndpoint;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @Test
    @WithAnonymousUser
    public void findAllConcepts_anonymous_succeeds() {

        /* test */
        findAllConcepts_generic();
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void findAllConcepts_noRole_succeeds() {

        /* test */
        findAllConcepts_generic();
    }

    @Test
    @WithAnonymousUser
    public void findAllUnits_anonymous_succeeds() {

        /* test */
        findAllUnits_generic();
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void findAllUnits_noRole_succeeds() {

        /* test */
        findAllUnits_generic();
    }

    @Test
    @WithAnonymousUser
    public void saveConcept_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            saveConcept_generic(COLUMN_CONCEPT_TEMPERATURE_SAVE_DTO, COLUMN_CONCEPT_TEMPERATURE);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void saveConcept_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            saveConcept_generic(COLUMN_CONCEPT_TEMPERATURE_SAVE_DTO, COLUMN_CONCEPT_TEMPERATURE);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-semantic-concept"})
    public void saveConcept_hasRole_succeeds() {

        /* test */
        saveConcept_generic(COLUMN_CONCEPT_TEMPERATURE_SAVE_DTO, COLUMN_CONCEPT_TEMPERATURE);
    }

    @Test
    @WithAnonymousUser
    public void saveUnit_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            saveUnit_generic(COLUMN_UNIT_DEGREES_CELSIUS_SAVE_DTO, COLUMN_UNIT_DEGREES_CELSIUS);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void saveUnit_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            saveUnit_generic(COLUMN_UNIT_DEGREES_CELSIUS_SAVE_DTO, COLUMN_UNIT_DEGREES_CELSIUS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-semantic-unit"})
    public void saveUnit_hasRole_succeeds() {

        /* test */
        saveUnit_generic(COLUMN_UNIT_DEGREES_CELSIUS_SAVE_DTO, COLUMN_UNIT_DEGREES_CELSIUS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAllConcepts_generic() {

        /* mock */
        when(semanticService.findAllConcepts())
                .thenReturn(List.of(COLUMN_CONCEPT_TEMPERATURE, COLUMN_CONCEPT_FAIR_DATA));

        /* test */
        final ResponseEntity<List<ConceptDto>> response = semanticsEndpoint.findAllConcepts();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<ConceptDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    public void findAllUnits_generic() {

        /* mock */
        when(semanticService.findAllUnits())
                .thenReturn(List.of(COLUMN_UNIT_TON, COLUMN_UNIT_DEGREES_CELSIUS));

        /* test */
        final ResponseEntity<List<UnitDto>> response = semanticsEndpoint.findAllUnits();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<UnitDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    public void saveConcept_generic(ConceptSaveDto saveDto, TableColumnConcept concept) {

        /* mock */
        if (concept != null) {
            when(semanticService.saveConcept(saveDto))
                    .thenReturn(concept);
        } else {
            doThrow(HibernateException.class)
                    .when(semanticService)
                    .saveConcept(saveDto);
        }

        /* test */
        final ResponseEntity<ConceptDto> response = semanticsEndpoint.saveUnit(saveDto);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final ConceptDto body = response.getBody();
        assertNotNull(body);
    }

    public void saveUnit_generic(UnitSaveDto saveDto, TableColumnUnit unit) {

        /* mock */
        if (unit != null) {
            when(semanticService.saveUnit(saveDto))
                    .thenReturn(unit);
        } else {
            doThrow(HibernateException.class)
                    .when(semanticService)
                    .saveUnit(saveDto);
        }

        /* test */
        final ResponseEntity<UnitDto> response = semanticsEndpoint.saveUnit(saveDto);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final UnitDto body = response.getBody();
        assertNotNull(body);
    }
}
