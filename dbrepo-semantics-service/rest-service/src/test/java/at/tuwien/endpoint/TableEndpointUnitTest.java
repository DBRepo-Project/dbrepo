
package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.semantics.*;
import at.tuwien.endpoints.OntologyEndpoint;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableColumnNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.OntologyService;
import at.tuwien.service.TableService;
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
public class TableEndpointUnitTest extends BaseUnitTest {

    @Autowired
    private TableEndpoint tableEndpoint;

    @MockBean
    private TableService tableService;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @Test
    @WithAnonymousUser
    public void analyseTable_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            analyseTable_generic(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            analyseTable_generic(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"table-semantic-analyse"})
    public void findAll_hasRole_succeeds() throws TableNotFoundException, QueryMalformedException {

        /* test */
        analyseTable_generic(DATABASE_1_ID, TABLE_1_ID);
    }

    @Test
    @WithAnonymousUser
    public void analyseTableColumn_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void analyseTableColumn_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"table-semantic-analyse"})
    public void analyseTableColumn_hasRole_succeeds() throws QueryMalformedException, TableColumnNotFoundException {

        /* test */
        analyseTableColumn_generic(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void analyseTable_generic(Long databaseId, Long tableId) throws TableNotFoundException, QueryMalformedException {

        /* mock */
        when(tableService.suggestTableSemantics(databaseId, tableId))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<EntityDto>> response = tableEndpoint.analyseTable(databaseId, tableId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<EntityDto> body = response.getBody();
        assertNotNull(body);
    }

    public void analyseTableColumn_generic(Long databaseId, Long tableId, Long columnId) throws QueryMalformedException,
            TableColumnNotFoundException {

        /* mock */
        when(tableService.suggestTableColumnSemantics(databaseId, tableId, columnId))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<TableColumnEntityDto>> response = tableEndpoint.analyseTableColumn(databaseId, tableId, columnId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableColumnEntityDto> body = response.getBody();
        assertNotNull(body);
    }
}
