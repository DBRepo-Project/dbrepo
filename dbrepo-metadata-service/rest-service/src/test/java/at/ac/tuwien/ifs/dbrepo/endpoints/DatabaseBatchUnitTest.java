package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DatabaseBatchUnitTest extends BaseTest {
    @Autowired private DatabaseEndpoint endpoint;
    @Autowired private MockMvc mvc;
    @MockitoBean private DatabaseServiceImpl databases;
    @MockitoBean private at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway keycloak;

    @ParameterizedTest
    @ValueSource(strings = {"anonymous", "nonmember", "member", "owner", "system"})
    void batchUsesCurrentDatabasePermissions(String role) throws Exception {
        DATABASE_1.setIsPublic(false);
        DATABASE_1.setIsSchemaPublic(false);
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        Principal principal = switch (role) {
            case "nonmember" -> USER_4_PRINCIPAL;
            case "member" -> USER_2_PRINCIPAL;
            case "owner" -> USER_1_PRINCIPAL;
            case "system" -> USER_LOCAL_ADMIN_PRINCIPAL;
            default -> null;
        };
        var result = endpoint.findBatch(List.of(DATABASE_1_ID), principal);
        assertNotNull(result.getBody());
        assertEquals(role.equals("anonymous") || role.equals("nonmember") ? 0 : 1, result.getBody().size());
        assertFalse(result.getHeaders().containsKey("X-Username"));
        assertFalse(result.getHeaders().containsKey("X-Password"));
        if (role.equals("member")) {
            DATABASE_1.setAccesses(List.of());
            assertTrue(endpoint.findBatch(List.of(DATABASE_1_ID), principal).getBody().isEmpty());
        }
    }

    @Test
    void duplicatesAreReadOnceAndOnlyMissingOrForbiddenAreOmitted() throws Exception {
        DATABASE_1.setIsPublic(true);
        DATABASE_2.setIsPublic(false);
        DATABASE_2.setIsSchemaPublic(false);
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(databases.findById(DATABASE_2_ID)).thenReturn(DATABASE_2);
        when(databases.findById(DATABASE_3_ID)).thenThrow(new DatabaseNotFoundException("missing"));
        var result = endpoint.findBatch(List.of(DATABASE_1_ID, DATABASE_2_ID, DATABASE_3_ID, DATABASE_1_ID), null);
        assertEquals(List.of(DATABASE_1_ID), result.getBody().stream().map(database -> database.getId()).toList());
        verify(databases, times(1)).findById(DATABASE_1_ID);
    }

    @Test
    void unexpectedFailureAbortsBatch() throws Exception {
        when(databases.findById(DATABASE_1_ID)).thenThrow(new IllegalStateException("unavailable"));
        assertThrows(IllegalStateException.class, () -> endpoint.findBatch(List.of(DATABASE_1_ID), null));
    }

    @Test
    void invalidRawCountsAndNullsAreRejectedBeforeReading() {
        for (List<UUID> ids : Arrays.asList(null, List.<UUID>of(), Arrays.asList((UUID) null),
                Collections.nCopies(101, DATABASE_1_ID))) {
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> endpoint.findBatch(ids, null))
                    .getStatusCode().value());
        }
        verifyNoInteractions(databases);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?id=", "?id=not-a-uuid"})
    void invalidHttpInputReturnsBadRequest(String query) throws Exception {
        mvc.perform(get("/api/v1/database/batch" + query)).andExpect(status().isBadRequest());
        verifyNoInteractions(databases);
    }

    @Test
    void maximumRawCountIsAcceptedAndLiteralBatchRouteIsUsed() throws Exception {
        DATABASE_1.setIsPublic(true);
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        String[] ids = Collections.nCopies(100, DATABASE_1_ID.toString()).toArray(String[]::new);
        mvc.perform(get("/api/v1/database/batch").param("id", ids))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(DATABASE_1_ID.toString()));
        verify(databases, times(1)).findById(DATABASE_1_ID);
    }

    @Test
    void literalHttpRouteReturnsRequestedDatabasesInOrderAndOmitsMissingIds() throws Exception {
        DATABASE_1.setIsPublic(true);
        DATABASE_2.setIsPublic(true);
        when(databases.findById(DATABASE_1_ID)).thenReturn(DATABASE_1);
        when(databases.findById(DATABASE_2_ID)).thenReturn(DATABASE_2);
        when(databases.findById(DATABASE_3_ID)).thenThrow(new DatabaseNotFoundException("missing"));
        mvc.perform(get("/api/v1/database/batch").param("id", DATABASE_2_ID.toString(),
                        DATABASE_3_ID.toString(), DATABASE_1_ID.toString(), DATABASE_2_ID.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(DATABASE_2_ID.toString()))
                .andExpect(jsonPath("$[1].id").value(DATABASE_1_ID.toString()));
        verify(databases, times(1)).findById(DATABASE_2_ID);
    }

    @Test
    void overLimitHttpInputIsRejectedBeforeDeduplication() throws Exception {
        String[] ids = Collections.nCopies(101, DATABASE_1_ID.toString()).toArray(String[]::new);
        mvc.perform(get("/api/v1/database/batch").param("id", ids)).andExpect(status().isBadRequest());
        verifyNoInteractions(databases);
    }

    @Test
    void suppliedInvalidCredentialsAreNotAnonymous() throws Exception {
        when(keycloak.getUserToken("not-a-user", "not-a-password"))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("invalid"));
        mvc.perform(get("/api/v1/database/batch").param("id", DATABASE_1_ID.toString())
                        .with(httpBasic("not-a-user", "not-a-password")))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(databases);
    }
}
