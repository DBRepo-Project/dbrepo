package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary.PrimaryKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import org.springframework.security.test.context.support.WithMockUser;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierStatusType;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class MetadataVisibilityUnitTest extends BaseTest {
    @Autowired
    private DatabaseEndpoint databaseEndpoint;
    @Autowired
    private TableEndpoint tableEndpoint;
    @Autowired
    private MetadataMapper mapper;
    @Autowired
    private ViewEndpoint viewEndpoint;
    @MockitoBean
    private DatabaseServiceImpl databases;
    @MockitoBean
    private TableService tables;
    @MockitoBean
    private ViewService views;
    @MockitoBean
    private DashboardService dashboards;
    @MockitoBean private at.ac.tuwien.ifs.dbrepo.service.UserService users;
    @MockitoBean private at.ac.tuwien.ifs.dbrepo.service.ContainerService containers;

    @BeforeEach
    void prepare() throws Exception {
        DATABASE_1.setIsPublic(true);
        TABLE_2.setIsPublic(true);
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(databases.findForUpdateById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(tables.findById(DATABASE_1, TABLE_2_ID)).thenReturn(TABLE_2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "nonmember", "member", "owner", "system"})
    void databaseResponseUsesCurrentMembershipForReferences(String role) throws Exception {
        final var response = databaseEndpoint.findById(DATABASE_1_ID, principal(role), false).getBody();
        assertNotNull(response);
        final TableDto table = response.getTables().stream().filter(t -> t.getId().equals(TABLE_2_ID)).findFirst().orElseThrow();
        assertEquals(privileged(role) ? 1 : 0, table.getConstraints().getForeignKeys().size());
        assertEquals(role.equals("owner") || role.equals("system") ? 3 : 0, response.getAccesses().size());
        assertEquals(privileged(role), response.getTables().stream().anyMatch(t -> t.getId().equals(TABLE_1_ID)));
        assertEquals(1, TABLE_2.getConstraints().getForeignKeys().size());
        assertSame(TABLE_1, TABLE_2.getConstraints().getForeignKeys().getFirst().getReferencedTable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "nonmember", "member", "owner", "system"})
    void tableResponseUsesCurrentMembershipForReferences(String role) throws Exception {
        final var table = tableEndpoint.findById(DATABASE_1_ID, TABLE_2_ID, principal(role)).getBody();
        assertNotNull(table);
        assertEquals(privileged(role) ? 1 : 0, table.getConstraints().getForeignKeys().size());
        assertFalse(table.getConstraints().getPrimaryKey().isEmpty());
        assertFalse(table.getConstraints().getUniques().isEmpty());
        assertEquals(1, TABLE_2.getConstraints().getForeignKeys().size());
    }

    @Test
    void visibleForeignReferenceRemainsInAnonymousResponse() throws Exception {
        TABLE_1.setIsSchemaPublic(true);
        final var table = tableEndpoint.findById(DATABASE_1_ID, TABLE_2_ID, null).getBody();
        assertNotNull(table);
        assertEquals(TABLE_1_ID, table.getConstraints().getForeignKeys().getFirst().getReferencedTable().getId());
        assertEquals(COLUMN_1_1_ID, table.getConstraints().getForeignKeys().getFirst().getReferences().getFirst()
                .getReferencedColumn().getId());
    }

    @Test
    void hiddenColumnCannotRemainInsideVisibleTableReference() {
        final var dto = mapper.tableToTableDto(TABLE_2);
        dto.getConstraints().getForeignKeys().getFirst().getReferences().getFirst().getReferencedColumn()
                .setTableId(TABLE_3_ID);
        databaseEndpoint.filterTableMetadata(dto, databaseEndpoint.visibleColumns(List.of(TABLE_1, TABLE_2)), null);
        assertTrue(dto.getConstraints().getForeignKeys().isEmpty());
        assertEquals(1, TABLE_2.getConstraints().getForeignKeys().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compositePrimaryKeyKeepsItsOrderAfterVisibilityFiltering(boolean hideColumn) {
        final var dto = mapper.tableToTableDto(TABLE_2);
        final var table = dto.getConstraints().getPrimaryKey().getFirst().getTable();
        final var columnOrder = List.of(COLUMN_2_3_ID, COLUMN_2_1_ID, COLUMN_2_2_ID);
        final var keys = columnOrder.stream().map(id -> PrimaryKeyDto.builder()
                .table(table)
                .column(ColumnBriefDto.builder().id(id).tableId(TABLE_2_ID).build())
                .build()).toList();
        dto.getConstraints().setPrimaryKey(keys);
        final var visible = hideColumn ? Set.of(COLUMN_2_3_ID, COLUMN_2_2_ID) : Set.copyOf(columnOrder);

        databaseEndpoint.filterTableMetadata(dto, Map.of(TABLE_2_ID, visible), null);

        assertEquals(hideColumn ? List.of(COLUMN_2_3_ID, COLUMN_2_2_ID) : columnOrder,
                dto.getConstraints().getPrimaryKey().stream().map(key -> key.getColumn().getId()).toList());
        assertEquals(columnOrder, keys.stream().map(key -> key.getColumn().getId()).toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "nonmember", "member", "owner", "system"})
    void nestedIdentifiersFollowIdentifierOwnershipRatherThanDatabaseGrants(String role) throws Exception {
        IDENTIFIER_1.setStatus(IdentifierStatusType.PUBLISHED);
        IDENTIFIER_2.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_2.setOwnedBy(USER_1_USERNAME);
        IDENTIFIER_3.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_3.setOwnedBy(USER_2_USERNAME);
        IDENTIFIER_5.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_5.setOwnedBy(null);
        final var identifiers = List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_5);
        DATABASE_1.setIdentifiers(identifiers);
        DATABASE_1.setSubsets(identifiers);
        TABLE_2.setIdentifiers(identifiers);
        VIEW_1.setIsPublic(true);
        VIEW_1.setIdentifiers(identifiers);
        when(views.findById(DATABASE_1, VIEW_1_ID)).thenReturn(VIEW_1);
        final var expected = switch (role) {
            case "owner" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID);
            case "member" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_3_ID);
            case "system" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID, IDENTIFIER_3_ID, IDENTIFIER_5_ID);
            default -> List.of(IDENTIFIER_1_ID);
        };
        final var database = databaseEndpoint.findById(DATABASE_1_ID, principal(role), false).getBody();
        assertNotNull(database);
        assertEquals(expected, database.getIdentifiers().stream().map(i -> i.getId()).toList());
        assertEquals(expected, database.getSubsets().stream().map(i -> i.getId()).toList());
        assertEquals(expected, database.getTables().stream().filter(t -> t.getId().equals(TABLE_2_ID)).findFirst()
                .orElseThrow().getIdentifiers().stream().map(i -> i.getId()).toList());
        assertEquals(expected, database.getViews().stream().filter(v -> v.getId().equals(VIEW_1_ID)).findFirst()
                .orElseThrow().getIdentifiers().stream().map(i -> i.getId()).toList());
        assertEquals(expected, tableEndpoint.findById(DATABASE_1_ID, TABLE_2_ID, principal(role)).getBody()
                .getIdentifiers().stream().map(i -> i.getId()).toList());
        assertEquals(expected, viewEndpoint.find(DATABASE_1_ID, VIEW_1_ID, principal(role)).getBody()
                .getIdentifiers().stream().map(i -> i.getId()).toList());
        assertEquals(4, TABLE_2.getIdentifiers().size());
        assertEquals(4, VIEW_1.getIdentifiers().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "nonmember", "member", "owner", "system"})
    void briefIdentifiersFollowIdentifierOwnership(String role) {
        IDENTIFIER_1.setStatus(IdentifierStatusType.PUBLISHED);
        IDENTIFIER_2.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_2.setOwnedBy(USER_1_USERNAME);
        IDENTIFIER_3.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_3.setOwnedBy(USER_2_USERNAME);
        IDENTIFIER_5.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_5.setOwnedBy(null);
        DATABASE_1.setIdentifiers(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_5));
        when(databases.findAllPublicOrSchemaPublic()).thenReturn(List.of(DATABASE_1));
        when(databases.findAllPublicOrSchemaPublicOrReadAccess(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(DATABASE_1));
        when(databases.findAll()).thenReturn(List.of(DATABASE_1));
        final var expected = switch (role) {
            case "owner" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID);
            case "member" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_3_ID);
            case "system" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID, IDENTIFIER_3_ID, IDENTIFIER_5_ID);
            default -> List.of(IDENTIFIER_1_ID);
        };
        final var response = databaseEndpoint.list(null, principal(role));
        assertEquals("1", response.getHeaders().getFirst("X-Count"));
        assertEquals(expected, response.getBody().getFirst().getIdentifiers().stream().map(i -> i.getId()).toList());
        assertEquals(4, DATABASE_1.getIdentifiers().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"table", "view", "visibility", "owner", "image", "create"})
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database", "modify-database-visibility",
            "modify-database-owner", "modify-database-image", "create-database"})
    void writeResponsesPreserveOwnDraftAndPreviewWhileExcludingForeignDrafts(String route) throws Exception {
        IDENTIFIER_1.setStatus(IdentifierStatusType.PUBLISHED);
        IDENTIFIER_2.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_2.setOwnedBy(USER_1_USERNAME);
        IDENTIFIER_3.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_3.setOwnedBy(USER_2_USERNAME);
        IDENTIFIER_5.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_5.setOwnedBy(null);
        DATABASE_1.setIdentifiers(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_5));
        DATABASE_1.setImage(new byte[]{1, 2, 3});
        final var expectedPreview = mapper.databaseDtoToDatabaseBriefDto(mapper.databaseToDatabaseDto(DATABASE_1))
                .getPreviewImage();
        when(databases.updateTableMetadata(DATABASE_1)).thenReturn(DATABASE_1);
        when(databases.updateViewMetadata(DATABASE_1)).thenReturn(DATABASE_1);
        when(databases.modifyVisibility(org.mockito.ArgumentMatchers.eq(DATABASE_1),
                org.mockito.ArgumentMatchers.any())).thenReturn(DATABASE_1);
        when(databases.modifyOwner(DATABASE_1, USER_2_USERNAME)).thenReturn(DATABASE_1);
        when(databases.modifyImage(DATABASE_1, null)).thenReturn(DATABASE_1);
        when(containers.find(CONTAINER_1_ID)).thenReturn(CONTAINER_1);
        when(users.findByUsername(USER_1_USERNAME)).thenReturn(USER_1_DTO);
        when(databases.create(org.mockito.ArgumentMatchers.eq(CONTAINER_1), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(USER_1_DTO))).thenReturn(DATABASE_1);
        when(dashboards.create(DATABASE_1)).thenReturn(at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto
                .builder().uid("test-dashboard").build());
        final var response = switch (route) {
            case "create" -> databaseEndpoint.create(CreateDatabaseDto.builder().cid(CONTAINER_1_ID)
                    .name("Example").isPublic(true).build(), USER_1_PRINCIPAL);
            case "table" -> databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
            case "view" -> databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
            case "visibility" -> databaseEndpoint.visibility(DATABASE_1_ID, DatabaseModifyVisibilityDto.builder()
                    .isPublic(true).isSchemaPublic(true).isDashboardEnabled(false).build(), USER_1_PRINCIPAL);
            case "owner" -> databaseEndpoint.transfer(DATABASE_1_ID,
                    DatabaseTransferDto.builder().username(USER_2_USERNAME).build(), USER_1_PRINCIPAL);
            default -> databaseEndpoint.modifyImage(DATABASE_1_ID, DatabaseModifyImageDto.builder().build(), USER_1_PRINCIPAL);
        };
        assertEquals(List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID), response.getBody().getIdentifiers().stream()
                .map(identifier -> identifier.getId()).toList());
        if (!route.equals("create")) {
            assertEquals(expectedPreview, response.getBody().getPreviewImage());
        }
        assertEquals(4, DATABASE_1.getIdentifiers().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "nonmember", "member", "owner", "system"})
    void batchPreservesNestedVisibilityAndEntityReferences(String role) throws Exception {
        IDENTIFIER_1.setStatus(IdentifierStatusType.PUBLISHED);
        IDENTIFIER_2.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_2.setOwnedBy(USER_1_USERNAME);
        IDENTIFIER_3.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_3.setOwnedBy(USER_2_USERNAME);
        IDENTIFIER_5.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_5.setOwnedBy(null);
        DATABASE_1.setIdentifiers(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_5));
        final var response = databaseEndpoint.findBatch(List.of(DATABASE_1_ID), principal(role));
        final var database = response.getBody().getFirst();
        final var table = database.getTables().stream().filter(t -> t.getId().equals(TABLE_2_ID)).findFirst().orElseThrow();
        assertEquals(privileged(role) ? 1 : 0, table.getConstraints().getForeignKeys().size());
        assertEquals(role.equals("owner") || role.equals("system") ? 3 : 0, database.getAccesses().size());
        final var expected = switch (role) {
            case "owner" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID);
            case "member" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_3_ID);
            case "system" -> List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID, IDENTIFIER_3_ID, IDENTIFIER_5_ID);
            default -> List.of(IDENTIFIER_1_ID);
        };
        assertEquals(expected, database.getIdentifiers().stream().map(identifier -> identifier.getId()).toList());
        assertEquals(4, DATABASE_1.getIdentifiers().size());
        assertEquals(1, TABLE_2.getConstraints().getForeignKeys().size());
        assertSame(TABLE_1, TABLE_2.getConstraints().getForeignKeys().getFirst().getReferencedTable());
    }

    private Principal principal(String role) {
        return switch (role) {
            case "nonmember" -> USER_4_PRINCIPAL;
            case "member" -> USER_2_PRINCIPAL;
            case "owner" -> USER_1_PRINCIPAL;
            case "system" -> USER_LOCAL_ADMIN_PRINCIPAL;
            default -> null;
        };
    }

    private boolean privileged(String role) {
        return role.equals("member") || role.equals("owner") || role.equals("system");
    }
}
