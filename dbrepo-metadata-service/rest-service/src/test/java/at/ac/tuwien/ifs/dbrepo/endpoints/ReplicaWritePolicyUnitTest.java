package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccessNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplicaWritePolicyUnitTest {

    private final DatabaseService databaseService = mock(DatabaseService.class);
    private final TableService tableService = mock(TableService.class);
    private final AccessService accessService = mock(AccessService.class);
    private final DashboardService dashboard = mock(DashboardService.class);
    private final EndpointValidator validator = new EndpointValidator(null, accessService);
    private final DatabaseEndpoint databases = new DatabaseEndpoint(null, null, null, databaseService,
            null, dashboard, null, null, new ReplicationPeers(""));
    private final TableEndpoint tables = new TableEndpoint(tableService, null, databaseService, dashboard, validator, null);
    private final ViewEndpoint views = new ViewEndpoint(null, null, databaseService, dashboard, null);
    private final AccessEndpoint accesses = new AccessEndpoint(accessService, null, databaseService, null, dashboard);
    private final Database replica = Database.builder().id(UUID.randomUUID()).ownedBy("admin")
            .creationLocation("https://origin.example").isSchemaPublic(true).build();
    private final Table table = Table.builder().id(UUID.randomUUID()).ownedBy("admin").database(replica)
            .identifiers(List.of()).build();
    private Principal principal;

    @BeforeEach
    void setup() throws Exception {
        final var admin = User.withUsername("admin").password("unused")
                .authorities("system", "modify-foreign-table-column-semantics", "delete-foreign-table").build();
        principal = new UsernamePasswordAuthenticationToken(admin, "", admin.getAuthorities());
        for (RestEndpoint endpoint : List.of(databases, tables, views, accesses, validator)) {
            ReflectionTestUtils.setField(endpoint, "replicationSiteUrl", "https://local.example");
        }
        when(databaseService.findById(replica.getId())).thenReturn(replica);
        when(tableService.findById(replica, table.getId())).thenReturn(table);
    }

    @Test
    void privilegedNormalRequestsCannotChangeReplicaMetadata() {
        assertAll(
                () -> assertThrows(NotAllowedException.class, () -> tables.create(replica.getId(), CreateTableDto.builder().build(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> tables.update(replica.getId(), table.getId(), TableUpdateDto.builder().build(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> tables.updateColumn(replica.getId(), table.getId(), UUID.randomUUID(), ColumnSemanticsUpdateDto.builder().build(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> tables.delete(replica.getId(), table.getId(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> views.create(replica.getId(), CreateViewDto.builder().build(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> views.update(replica.getId(), UUID.randomUUID(), ViewUpdateDto.builder().build(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> views.delete(replica.getId(), UUID.randomUUID(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> databases.visibility(replica.getId(), DatabaseModifyVisibilityDto.builder().build(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> databases.transfer(replica.getId(), DatabaseTransferDto.builder().build(), principal)),
                () -> assertThrows(NotAllowedException.class, () -> databases.modifyImage(replica.getId(), DatabaseModifyImageDto.builder().build(), principal)));
        verifyNoInteractions(dashboard);
    }

    @Test
    void publicSchemaAndSystemRoleCannotBypassWriteValidation() {
        assertThrows(NotAllowedException.class, () -> validator.validateOnlyPrivateSchemaAccess(replica, principal, true));
        assertThrows(NotAllowedException.class, () -> validator.validateOnlyAccess(replica, principal, true));
        assertDoesNotThrow(() -> validator.validateOnlyAccess(replica, principal, false));
    }

    @Test
    void normalCreateCannotSpoofReplicationOrigin() {
        assertThrows(NotAllowedException.class, () -> databases.create(
                CreateDatabaseDto.builder().creationLocation("https://origin.example").build(), principal));
        assertThrows(NotAllowedException.class, () -> tables.create(replica.getId(),
                CreateTableDto.builder().creationLocation("https://origin.example").build(), principal));
    }

    @Test
    void localAccessManagementRemainsReadOnly() throws Exception {
        final CreateAccessDto write = CreateAccessDto.builder().type(AccessTypeDto.WRITE_ALL).build();
        assertThrows(NotAllowedException.class, () -> accesses.create(replica.getId(), "alice", write, principal));
        assertThrows(NotAllowedException.class, () -> accesses.update(replica.getId(), "alice", write, principal));
        when(accessService.find(replica, "alice")).thenThrow(new AccessNotFoundException("missing"));
        accesses.create(replica.getId(), "alice", CreateAccessDto.builder().type(AccessTypeDto.READ).build(), principal);
        verify(accessService).create(replica, "alice", AccessTypeDto.READ);
    }
}
