
package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.TableService;
import com.rabbitmq.client.Channel;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableHistoryEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private QueryService queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private TableService tableService;

    @Autowired
    private TableHistoryEndpoint tableHistoryEndpoint;

    @Test
    public void data_publicAnonymous_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void data_publicAnonymous2_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_publicResearcherRead_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_publicResearcherWriteOwn_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_publicResearcherWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_publicDeveloperRead_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_publicDeveloperWriteOwn_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_publicDataStewardRead_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_publicDataStewardWriteOwn_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */
    
    @Test
    public void data_privateAnonymous_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void data_privateAnonymous2_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_privateResearcherRead_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_privateResearcherWriteOwn_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_privateResearcherWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_privateDeveloperRead_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_privateDeveloperWriteOwn_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_privateDataStewardRead_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_privateDataStewardWriteOwn_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, NotAllowedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void data_generic(Long containerId, Long databaseId, Database database, Long tableId, Table table,
                                String username, Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, DatabaseConnectionException,
            QueryMalformedException, QueryStoreException, TableNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId))
                .thenReturn(table);
        if (access != null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        }

        /* test */
        final ResponseEntity<List<TableHistoryDto>> response = tableHistoryEndpoint.getAll(containerId, databaseId, tableId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
