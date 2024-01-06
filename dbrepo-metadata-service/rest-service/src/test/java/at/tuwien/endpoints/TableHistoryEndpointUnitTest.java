package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
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

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class TableHistoryEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private TableService tableService;

    @Autowired
    private TableHistoryEndpoint tableHistoryEndpoint;

    @Test
    public void data_publicAnonymous_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        data_generic(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void data_publicAnonymous2_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        data_generic(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_publicResearcher_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        data_generic(DATABASE_1_ID, DATABASE_1, TABLE_1_ID, TABLE_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_privateResearcher_fails() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        data_generic(DATABASE_2_ID, DATABASE_2, TABLE_5_ID, TABLE_5, USER_1_ID, USER_1_PRINCIPAL, null);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void data_generic(Long databaseId, Database database, Long tableId, Table table,
                                UUID userId, Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, UserNotFoundException, DatabaseConnectionException,
            QueryMalformedException, QueryStoreException, TableNotFoundException {

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(tableService.find(databaseId, tableId))
                .thenReturn(table);

        /* test */
        final ResponseEntity<List<TableHistoryDto>> response = tableHistoryEndpoint.getAll(databaseId, tableId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
