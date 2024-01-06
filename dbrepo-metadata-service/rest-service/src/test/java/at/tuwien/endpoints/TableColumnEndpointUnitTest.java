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
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.UUID;

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
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .conceptUri(COLUMN_CONCEPT_FAIR_DATA_URI)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), DATABASE_3, TABLE_8, null, request, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleNoAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .conceptUri(COLUMN_CONCEPT_FAIR_DATA_URI)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), DATABASE_3, TABLE_8, null, request, USER_1_ID, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleHasOnlyReadAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .conceptUri(COLUMN_CONCEPT_FAIR_DATA_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), DATABASE_3, TABLE_8, null, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleHasOwnWriteAccess_succeeds() throws TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseNotFoundException, ContainerNotFoundException,
            SemanticEntityPersistException, SemanticEntityNotFoundException, QueryMalformedException, at.tuwien.exception.AccessDeniedException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), DATABASE_3, TABLE_8, TABLE_1_COLUMNS.get(0), request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleForeignHasOwnWriteAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), DATABASE_3, TABLE_8, null, request, USER_2_ID, USER_2_PRINCIPAL, DATABASE_3_USER_2_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicDatabaseNotFound_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), null, TABLE_8, null, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicTableNotFound_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), DATABASE_3, null, null, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_publicHasRoleForeignHasAllWriteAccess_succeeds() throws TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseNotFoundException,
            ContainerNotFoundException, SemanticEntityPersistException, SemanticEntityNotFoundException,
            QueryMalformedException, at.tuwien.exception.AccessDeniedException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        generic_update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_COLUMNS.get(0).getId(), DATABASE_3, TABLE_8, TABLE_8_COLUMNS.get(0), request, USER_2_ID, USER_2_PRINCIPAL, DATABASE_3_USER_2_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void update_privateAnonymous_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), DATABASE_1, TABLE_1, null, request, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleNoAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), DATABASE_1, TABLE_1, null, request, USER_1_ID, USER_1_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleHasOnlyReadAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), DATABASE_1, TABLE_1, null, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleHasOwnWriteAccess_succeeds() throws TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseNotFoundException, ContainerNotFoundException,
            SemanticEntityPersistException, SemanticEntityNotFoundException, QueryMalformedException,
            at.tuwien.exception.AccessDeniedException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), DATABASE_1, TABLE_1, TABLE_1_COLUMNS.get(0), request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleForeignHasOwnWriteAccess_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), DATABASE_1, TABLE_1, null, request, USER_2_ID, USER_2_PRINCIPAL, DATABASE_1_USER_2_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateDatabaseNotFound_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), null, TABLE_1, null, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateTableNotFound_fails() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), DATABASE_1, null, null, request, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-table-column-semantics"})
    public void update_privateHasRoleForeignHasAllWriteAccess_succeeds() throws TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseNotFoundException,
            ContainerNotFoundException, SemanticEntityPersistException, SemanticEntityNotFoundException,
            QueryMalformedException, at.tuwien.exception.AccessDeniedException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .build();

        /* test */
        generic_update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), DATABASE_1, TABLE_1, TABLE_1_COLUMNS.get(0), request, USER_2_ID, USER_2_PRINCIPAL, DATABASE_1_USER_2_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected ResponseEntity<ColumnDto> generic_update(Long databaseId, Long tableId, Long columnId,
                                                       Database database, Table table, TableColumn column,
                                                       ColumnSemanticsUpdateDto data, UUID userId,
                                                       Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, NotAllowedException, TableNotFoundException, TableMalformedException,
            ContainerNotFoundException, SemanticEntityPersistException, SemanticEntityNotFoundException,
            QueryMalformedException, at.tuwien.exception.AccessDeniedException {

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
            when(tableService.find(databaseId, tableId))
                    .thenReturn(table);
            when(tableService.update(databaseId, tableId, columnId, data, "abc"))
                    .thenReturn(column);
        } else {
            doThrow(TableNotFoundException.class)
                    .when(tableService)
                    .update(databaseId, tableId, columnId, data, "abc");
            doThrow(TableNotFoundException.class)
                    .when(tableService)
                    .find(databaseId, tableId);
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
        return tableColumnEndpoint.update(databaseId, tableId, columnId, data, principal, "abc");
    }
}
