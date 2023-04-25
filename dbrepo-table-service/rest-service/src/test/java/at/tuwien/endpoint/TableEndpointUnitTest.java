package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private TableIdxRepository tableidxRepository;

    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private AccessService accessService;

    @Autowired
    private TableEndpoint tableEndpoint;

    @BeforeEach
    public void beforeEach() {
        DATABASE_1.setTables(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_7));
        DATABASE_3.setTables(List.of(TABLE_8));
    }

    @Test
    @WithAnonymousUser
    public void list_publicAnonymous_succeeds() throws NotAllowedException, DatabaseNotFoundException {

        /* test */
        generic_list(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_publicHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(CONTAINER_3_ID, DATABASE_3_ID, null, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_publicHasRole_succeeds() throws DatabaseNotFoundException, NotAllowedException {

        /* test */
        final ResponseEntity<List<TableBriefDto>> response = generic_list(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_RESEARCHER_READ_ACCESS);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableBriefDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void list_publicNoRole_succeeds() throws NotAllowedException, DatabaseNotFoundException {

        /* test */
        generic_list(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_4_USERNAME, USER_4_PRINCIPAL, null);
    }

    @Test
    @WithAnonymousUser
    public void create_publicAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_4_CREATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_create(CONTAINER_3_ID, DATABASE_3_ID, null, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_publicNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void list_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_list(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_privateHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(CONTAINER_1_ID, DATABASE_1_ID, null, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_privateHasRole_succeeds() throws DatabaseNotFoundException, NotAllowedException {

        /* test */
        final ResponseEntity<List<TableBriefDto>> response = generic_list(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_RESEARCHER_READ_ACCESS);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableBriefDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(4, body.size());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void list_privateNoRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_list(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_4_USERNAME, USER_4_PRINCIPAL, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_privateAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_4_CREATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, null, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_privateNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, TABLE_4_CREATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_RESEARCHER_READ_ACCESS);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected ResponseEntity<List<TableBriefDto>> generic_list(Long containerId, Long databaseId, Database database, String username, Principal principal, DatabaseAccess access) throws DatabaseNotFoundException, NotAllowedException {

        /* when */
        if (database != null) {
            when(databaseService.find(containerId, databaseId))
                    .thenReturn(database);
            log.trace("mock {} tables", database.getTables().size());
            when(tableRepository.findByDatabaseOrderByCreatedDesc(any(Database.class)))
                    .thenReturn(database.getTables());
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .find(containerId, databaseId);
            when(tableRepository.findByDatabaseOrderByCreatedDesc(any(Database.class)))
                    .thenReturn(List.of());
        }
        if (access != null) {
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            doThrow(NotAllowedException.class)
                    .when(accessService)
                    .find(databaseId, username);
        }

        /* test */
        return tableEndpoint.list(containerId, databaseId, principal);
    }

    protected ResponseEntity<TableBriefDto> generic_create(Long containerId, Long databaseId, Database database, TableCreateDto data, String username, Principal principal, DatabaseAccess access) throws DatabaseNotFoundException, NotAllowedException, UserNotFoundException, TableMalformedException, QueryMalformedException, ImageNotSupportedException, AmqpException, TableNameExistsException, ContainerNotFoundException {

        /* when */
        if (database != null) {
            when(databaseService.find(containerId, databaseId))
                    .thenReturn(database);
            log.trace("mock {} tables", database.getTables().size());
            when(tableRepository.findByDatabaseOrderByCreatedDesc(any(Database.class)))
                    .thenReturn(database.getTables());
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .find(containerId, databaseId);
            when(tableRepository.findByDatabaseOrderByCreatedDesc(any(Database.class)))
                    .thenReturn(List.of());
        }
        if (access != null) {
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            doThrow(NotAllowedException.class)
                    .when(accessService)
                    .find(databaseId, username);
        }

        /* test */
        return tableEndpoint.create(containerId, databaseId, data, principal);
    }
}
