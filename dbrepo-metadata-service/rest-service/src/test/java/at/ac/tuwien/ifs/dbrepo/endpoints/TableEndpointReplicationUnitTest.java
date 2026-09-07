package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.LocalTableIdDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableEndpointReplicationUnitTest extends BaseTest {

    @MockitoBean
    private DatabaseService databaseService;

    @MockitoBean
    private AccessService accessService;

    @MockitoBean
    private TableService tableService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReplicationService replicationService;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table"})
    public void create_replicatedDatabaseAddsReplicationKeyUnique_succeeds() throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException, TableNotFoundException,
            DataServiceConnectionException, MalformedException, DataServiceException, DatabaseNotFoundException,
            AccessNotFoundException, OntologyNotFoundException, TableExistsException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        final Database database = Database.builder()
                .id(DATABASE_3_ID)
                .isPublic(DATABASE_3_PUBLIC)
                .isSchemaPublic(DATABASE_3_SCHEMA_PUBLIC)
                .ownedBy(USER_3_USERNAME)
                .replicaUrls(List.of(ReplicaLocation.builder()
                        .url("http://replica.test")
                        .replicaDatabaseId(UUID.randomUUID())
                        .build()))
                .build();
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.BIGINT)
                        .nullAllowed(false)
                        .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .uniques(List.of())
                        .build())
                .build();

        when(databaseService.findById(DATABASE_3_ID))
                .thenReturn(database);
        when(accessService.find(database, USER_1_USERNAME))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS);
        when(tableService.createTable(database, request, USER_1_PRINCIPAL))
                .thenReturn(TABLE_1);

        final ResponseEntity<TableBriefDto> response = tableEndpoint.create(DATABASE_3_ID, request, USER_1_PRINCIPAL);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(request.getColumns()
                .stream()
                .anyMatch(column -> "replication_key".equals(column.getName())));
        assertTrue(request.getConstraints().getUniques()
                .stream()
                .anyMatch(unique -> unique.equals(List.of("replication_key"))));
        verify(replicationService).replicateTable(eq(request), eq(DATABASE_3_ID), anyList(), eq(TABLE_1.getId()));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void replicate_existingReplica_returnsExistingTable() throws UserNotFoundException,
            SearchServiceException, NotAllowedException, SemanticEntityNotFoundException, TableNotFoundException,
            DataServiceConnectionException, MalformedException, DataServiceException, DatabaseNotFoundException,
            AccessNotFoundException, OntologyNotFoundException, TableExistsException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        final UUID creationId = UUID.randomUUID();
        final CreateTableDto request = CreateTableDto.builder()
                .name("Some Table")
                .description("Some Description")
                .columns(List.of(CreateTableColumnDto.builder()
                        .name("ID")
                        .type(ColumnTypeDto.BIGINT)
                        .nullAllowed(false)
                        .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .uniques(List.of())
                        .build())
                .creationLocation("http://local.test")
                .build();
        final TableNotificationDto notification = TableNotificationDto.builder()
                .creationId(creationId)
                .createTableDto(request)
                .build();

        when(databaseService.findById(DATABASE_3_ID))
                .thenReturn(DATABASE_3);
        when(tableService.findLocalTableIdByReplicaTableId(creationId))
                .thenReturn(new LocalTableIdDto(TABLE_1_ID, creationId));
        when(tableService.findById(DATABASE_3, TABLE_1_ID))
                .thenReturn(TABLE_1);

        final ResponseEntity<TableBriefDto> response = tableEndpoint.replicate(DATABASE_3_ID, notification,
                USER_1_PRINCIPAL);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(TABLE_1_ID, response.getBody().getId());
        verify(tableService, never()).createTable(eq(DATABASE_3), any(CreateTableDto.class), eq(USER_1_PRINCIPAL),
                eq(creationId));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void replicate_missingCreationId_fails() throws DatabaseNotFoundException {
        final TableNotificationDto notification = TableNotificationDto.builder()
                .createTableDto(CreateTableDto.builder()
                        .name("Some Table")
                        .build())
                .build();

        assertThrows(MalformedException.class, () -> {
            tableEndpoint.replicate(DATABASE_3_ID, notification, USER_1_PRINCIPAL);
        });
        verify(databaseService, never()).findById(DATABASE_3_ID);
    }
}
