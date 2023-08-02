package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.repository.sdb.ConceptIdxRepository;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import at.tuwien.repository.sdb.TableIdxRepository;
import at.tuwien.repository.sdb.UnitIdxRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class TableColumnEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private AccessService accessService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private TableService tableService;

    @Autowired
    private TableColumnEndpoint tableColumnEndpoint;

    @Test
    @WithAnonymousUser
    public void update_publicAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, DATABASE_3, TABLE_8, null, COLUMN_8_2_SEMANTICS_UPDATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, DATABASE_3, TABLE_8, null, COLUMN_8_2_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleHasOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, DATABASE_3, TABLE_8, null, COLUMN_8_2_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleHasOwnWriteAccess_succeeds() throws TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseNotFoundException, ContainerNotFoundException,
            SemanticEntityPersistException, SemanticEntityNotFoundException {

        /* test */
        generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, DATABASE_3, TABLE_8, COLUMN_8_2_WITH_SEMANTICS, COLUMN_8_2_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleForeignHasOwnWriteAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, DATABASE_3, TABLE_8, null, COLUMN_8_2_SEMANTICS_UPDATE_DTO, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_USER_2_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, null, TABLE_8, null, COLUMN_8_2_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicTableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, DATABASE_3, null, null, COLUMN_8_2_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleForeignHasAllWriteAccess_succeeds() throws TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseNotFoundException,
            ContainerNotFoundException, SemanticEntityPersistException, SemanticEntityNotFoundException {

        /* test */
        generic_update(DATABASE_3_ID, TABLE_8_ID, COLUMN_1_1_ID, DATABASE_3, TABLE_8, COLUMN_8_2_WITH_SEMANTICS, COLUMN_8_2_SEMANTICS_UPDATE_DTO, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_USER_2_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void update_privateAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, DATABASE_1, TABLE_1, null, COLUMN_1_4_SEMANTICS_UPDATE_DTO, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, DATABASE_1, TABLE_1, null, COLUMN_1_4_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleHasOnlyReadAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, DATABASE_1, TABLE_1, null, COLUMN_1_4_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleHasOwnWriteAccess_succeeds() throws TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseNotFoundException, ContainerNotFoundException,
            SemanticEntityPersistException, SemanticEntityNotFoundException {

        /* test */
        generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, DATABASE_1, TABLE_1, COLUMN_1_4_WITH_SEMANTICS, COLUMN_1_4_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleForeignHasOwnWriteAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, DATABASE_1, TABLE_1, null, COLUMN_1_4_SEMANTICS_UPDATE_DTO, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_USER_2_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, null, TABLE_1, null, COLUMN_1_4_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateTableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, DATABASE_1, null, null, COLUMN_1_4_SEMANTICS_UPDATE_DTO, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleForeignHasAllWriteAccess_succeeds() throws TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseNotFoundException,
            ContainerNotFoundException, SemanticEntityPersistException, SemanticEntityNotFoundException {

        /* test */
        generic_update(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID, DATABASE_1, TABLE_1, COLUMN_1_4_WITH_SEMANTICS, COLUMN_1_4_SEMANTICS_UPDATE_DTO, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_USER_2_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected ResponseEntity<ColumnDto> generic_update(Long databaseId, Long tableId, Long columnId,
                                                       Database database, Table table, TableColumn column,
                                                       ColumnSemanticsUpdateDto data, String username,
                                                       Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, NotAllowedException, TableNotFoundException, TableMalformedException,
            ContainerNotFoundException, SemanticEntityPersistException, SemanticEntityNotFoundException {

        /* mock */
        if (database != null) {
            when(databaseService.find(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .find(databaseId);
        }
        if (table != null) {
            when(tableService.findById(databaseId, tableId))
                    .thenReturn(table);
            when(tableService.update(databaseId, tableId, columnId, data, "abc"))
                    .thenReturn(column);
        } else {
            doThrow(TableNotFoundException.class)
                    .when(tableService)
                    .update(databaseId, tableId, columnId, data, "abc");
            doThrow(TableNotFoundException.class)
                    .when(tableService)
                    .findById(databaseId, tableId);
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
        return tableColumnEndpoint.update(databaseId, tableId, columnId, data, principal, "abc");
    }
}
