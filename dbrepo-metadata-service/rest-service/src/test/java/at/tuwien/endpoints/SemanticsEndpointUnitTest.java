package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.*;
import at.tuwien.service.EntityService;
import at.tuwien.service.SemanticService;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class SemanticsEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private SemanticService semanticService;

    @MockBean
    private EntityService entityService;

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
    public void analyseTable_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTable_generic(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTable_generic(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"table-semantic-analyse"})
    public void findAll_hasRole_succeeds() throws TableNotFoundException, QueryMalformedException,
            DatabaseNotFoundException, OntologyInvalidException {

        /* test */
        analyseTable_generic(DATABASE_1_ID, TABLE_1_ID);
    }

    @Test
    @WithAnonymousUser
    public void analyseTableColumn_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId());
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void analyseTableColumn_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId());
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"table-semantic-analyse"})
    public void analyseTableColumn_hasRole_succeeds() throws QueryMalformedException, TableColumnNotFoundException,
            TableNotFoundException, DatabaseNotFoundException, OntologyInvalidException {

        /* test */
        analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAllConcepts_generic() {

        /* mock */
        when(semanticService.findAllConcepts())
                .thenReturn(List.of(COLUMN_CONCEPT_PRECIPITATION, COLUMN_CONCEPT_FAIR_DATA));

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
                .thenReturn(List.of(UNIT_MILLIMETRE, UNIT_TONNE));

        /* test */
        final ResponseEntity<List<UnitDto>> response = semanticsEndpoint.findAllUnits();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<UnitDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    public void analyseTable_generic(Long databaseId, Long tableId) throws TableNotFoundException,
            QueryMalformedException, DatabaseNotFoundException, OntologyInvalidException {

        /* mock */
        when(entityService.suggestTableSemantics(databaseId, tableId))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<EntityDto>> response = semanticsEndpoint.analyseTable(databaseId, tableId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<EntityDto> body = response.getBody();
        assertNotNull(body);
    }

    public void analyseTableColumn_generic(Long databaseId, Long tableId, Long columnId) throws QueryMalformedException,
            TableColumnNotFoundException, TableNotFoundException, DatabaseNotFoundException, OntologyInvalidException {

        /* mock */
        when(entityService.suggestTableColumnSemantics(databaseId, tableId, columnId))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<TableColumnEntityDto>> response = semanticsEndpoint.analyseTableColumn(databaseId, tableId, columnId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableColumnEntityDto> body = response.getBody();
        assertNotNull(body);
    }
}
