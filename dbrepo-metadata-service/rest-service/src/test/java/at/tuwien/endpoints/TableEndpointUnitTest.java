package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class TableEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private TableService tableService;

    @MockBean
    private MessageQueueService messageQueueService;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Test
    @WithAnonymousUser
    public void list_publicAnonymous_succeeds() throws NotAllowedException, DatabaseNotFoundException,
            at.tuwien.exception.AccessDeniedException {

        /* test */
        generic_list(DATABASE_3_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_publicHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(DATABASE_3_ID, null, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_publicHasRole_succeeds() throws DatabaseNotFoundException, NotAllowedException,
            at.tuwien.exception.AccessDeniedException {

        /* test */
        final ResponseEntity<List<TableBriefDto>> response = generic_list(DATABASE_3_ID, DATABASE_3, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableBriefDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void list_publicNoRole_succeeds() throws NotAllowedException, DatabaseNotFoundException, at.tuwien.exception.AccessDeniedException {

        /* test */
        generic_list(DATABASE_3_ID, DATABASE_3, USER_4_ID, USER_4_PRINCIPAL, null);
    }

    @Test
    @WithAnonymousUser
    public void create_publicAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_create(DATABASE_3_ID, null, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_publicNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_publicHasRoleOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicDecimalColumnSizeMissing_fails() {
        final TableCreateDto request = TableCreateDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.DECIMAL)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicDecimalColumnSizeTooSmall_fails() {
        final TableCreateDto request = TableCreateDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.DECIMAL)
                        .size(-1L)
                        .d(0L)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicDecimalColumnSizeTooBig_fails() {
        final TableCreateDto request = TableCreateDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.DECIMAL)
                        .size(66L)
                        .d(0L)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicDecimalColumnDTooBig_fails() {
        final TableCreateDto request = TableCreateDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.DECIMAL)
                        .size(0L)
                        .d(39L)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"create-table"})
    public void create_publicDecimalColumnDBiggerSize_fails() {
        final TableCreateDto request = TableCreateDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.DECIMAL)
                        .size(9L)
                        .d(10L)
                        .build()))
                .constraints(null)
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            generic_create(DATABASE_3_ID, DATABASE_3, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_publicAnonymous_succeeds() throws DatabaseNotFoundException, TableNotFoundException,
            at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {

        /* test */
        generic_findById(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_publicHasRoleTableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_findById(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_publicHasRoleDatabaseNotFound_succeeds() throws DatabaseNotFoundException, TableNotFoundException,
            at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {

        /* test */
        generic_findById(DATABASE_3_ID, TABLE_8_ID, null, TABLE_8, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_publicHasRole_succeeds() throws DatabaseNotFoundException, TableNotFoundException,
            at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {

        /* test */
        final ResponseEntity<TableDto> response = generic_findById(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final TableDto body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findById_publicNoRole_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
            at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {

        /* test */
        generic_findById(DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_ID, USER_1_PRINCIPAL, null);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void list_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_list(DATABASE_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_privateHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_list(DATABASE_1_ID, null, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void list_privateHasRole_succeeds() throws DatabaseNotFoundException, NotAllowedException,
            at.tuwien.exception.AccessDeniedException {

        /* test */
        final ResponseEntity<List<TableBriefDto>> response = generic_list(DATABASE_1_ID, DATABASE_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final List<TableBriefDto> body = response.getBody();
        assertNotNull(body);
        assertEquals(4, body.size());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void list_privateNoRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_list(DATABASE_1_ID, DATABASE_1, USER_4_ID, USER_4_PRINCIPAL, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_privateAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_create(DATABASE_1_ID, null, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_privateNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_privateHasRoleOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, TABLE_5_CREATE_DTO, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_privateAnonymous_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
            at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {

        /* test */
        generic_findById(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_privateHasRoleTableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_findById(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, null, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_privateHasRoleDatabaseNotFound_succeeds() throws DatabaseNotFoundException,
            TableNotFoundException, at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {

        /* test */
        generic_findById(DATABASE_1_ID, TABLE_1_ID, null, TABLE_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-table")
    public void findById_privateHasRole_succeeds() throws DatabaseNotFoundException, TableNotFoundException,
            at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {
        /* test */
        final ResponseEntity<TableDto> response = generic_findById(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final TableDto body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findById_privateNoRole_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
            at.tuwien.exception.AccessDeniedException, QueueNotFoundException, BrokerRemoteException {

        /* test */
        generic_findById(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, TABLE_1, USER_4_ID, USER_4_PRINCIPAL, null);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void delete_privateNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_delete(USER_4_PRINCIPAL, TABLE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table"})
    public void delete_succeeds() throws TableNotFoundException, TableMalformedException, NotAllowedException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        generic_delete(USER_1_PRINCIPAL, TABLE_1);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-table"})
    public void delete_foreign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_delete(USER_3_PRINCIPAL, TABLE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-foreign-table"})
    public void delete_foreign_succeeds() throws TableNotFoundException, TableMalformedException, NotAllowedException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        generic_delete(USER_2_PRINCIPAL, TABLE_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-table"})
    public void delete_hasIdentifiers_fails() {
        final Table response = Table.builder()
                .identifiers(List.of(IDENTIFIER_1))
                .owner(USER_1)
                .ownedBy(USER_1_ID)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_delete(USER_1_PRINCIPAL, response);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected ResponseEntity<List<TableBriefDto>> generic_list(Long databaseId, Database database, UUID userId,
                                                               Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, NotAllowedException, at.tuwien.exception.AccessDeniedException {

        /* mock */
        if (database != null) {
            when(databaseService.find(databaseId))
                    .thenReturn(database);
            when(tableService.findAll(databaseId))
                    .thenReturn(database.getTables());
            log.trace("mock {} table(s)", database.getTables().size());
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .find(databaseId);
            when(tableService.findAll(databaseId))
                    .thenReturn(List.of());
            log.trace("mock 0 tables");
        }
        if (access != null) {
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            doThrow(AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }

        /* test */
        return tableEndpoint.list(databaseId, principal);
    }

    protected ResponseEntity<TableDto> generic_create(Long databaseId, Database database, TableCreateDto data,
                                                           UUID userId, Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, NotAllowedException, TableMalformedException, QueryMalformedException,
            ImageNotSupportedException, TableNameExistsException, AccessDeniedException, TableNotFoundException,
            UserNotFoundException {

        /* mock */
        if (database != null) {
            when(databaseService.find(databaseId))
                    .thenReturn(database);
            log.trace("mock {} tables", database.getTables().size());
            when(tableService.findAll(databaseId))
                    .thenReturn(database.getTables());
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .find(databaseId);
            when(tableService.findAll(databaseId))
                    .thenReturn(List.of());
        }
        if (access != null) {
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            doThrow(AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }

        /* test */
        return tableEndpoint.create(databaseId, data, principal);
    }

    protected ResponseEntity<TableDto> generic_findById(Long databaseId, Long tableId, Database database,
                                                        Table table, UUID userId, Principal principal,
                                                        DatabaseAccess access) throws DatabaseNotFoundException,
            TableNotFoundException, at.tuwien.exception.AccessDeniedException, QueueNotFoundException,
            BrokerRemoteException {

        /* mock */
        if (table != null) {
            when(tableService.find(databaseId, tableId))
                    .thenReturn(table);
            when(databaseService.find(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(TableNotFoundException.class)
                    .when(tableService)
                    .find(databaseId, tableId);
            when(tableService.findAll(databaseId))
                    .thenReturn(List.of());
        }
        if (database != null) {
            when(databaseService.find(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .find(databaseId);
        }
        if (access != null) {
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            doThrow(AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }
        when(messageQueueService.findQueue("dbrepo"))
                .thenReturn(QUEUE_DTO);

        /* test */
        return tableEndpoint.findById(databaseId, tableId, principal);
    }

    protected ResponseEntity<?> generic_delete(Principal principal, Table table) throws TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException {

        /* mock */
        when(tableService.find(anyLong(), anyLong()))
                .thenReturn(table);

        /* test */
        return tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID, principal);
    }
}
