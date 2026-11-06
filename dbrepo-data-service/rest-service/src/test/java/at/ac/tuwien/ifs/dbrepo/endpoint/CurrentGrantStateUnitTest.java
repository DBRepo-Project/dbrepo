package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccessNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.AccessEndpoint;
import at.ac.tuwien.ifs.dbrepo.endpoints.GrantEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.GrantService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentGrantStateUnitTest extends BaseTest {
    private final MetadataService cache = mock(MetadataService.class);
    private final MetadataServiceGateway gateway = mock(MetadataServiceGateway.class);
    private final AccessService access = mock(AccessService.class);
    private final GrantService grants = mock(GrantService.class);
    private final AccessEndpoint accessEndpoint = new AccessEndpoint(access, cache, gateway);
    private final GrantEndpoint grantEndpoint = new GrantEndpoint(grants, cache, gateway);
    private Database current;

    @BeforeEach
    void prepare() throws Exception {
        when(cache.getDatabase(DATABASE_1_ID)).thenReturn(DATABASE_1_CACHE);
        when(cache.getUser(USER_2_USERNAME)).thenReturn(USER_2_CACHE);
        current = Database.builder().id(DATABASE_1_ID).ownedBy(USER_3_USERNAME).accesses(List.of()).build();
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenAnswer(invocation -> current);
        when(grants.find(current, USER_2_CACHE)).thenReturn(READ_GRANT_DTO);
    }

    @Test
    void createUsesCurrentAbsenceDespiteCachedMembership() throws Exception {
        assertEquals(201, accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO).getStatusCode().value());
        verify(access).create(current, USER_2_CACHE, UPDATE_DATABASE_ACCESS_READ_DTO.getType());
        freshOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "revoke"})
    void existingGrantMutationUsesCurrentMembershipDespiteCachedAbsence(String operation) throws Exception {
        DATABASE_1_CACHE.setAccesses(List.of(DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE));
        current.setAccesses(List.of(DATABASE_1_USER_2_WRITE_OWN_ACCESS_CACHE));
        if (operation.equals("update")) {
            assertEquals(202, accessEndpoint.update(DATABASE_1_ID, USER_2_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO).getStatusCode().value());
            verify(access).update(current, USER_2_CACHE, UPDATE_DATABASE_ACCESS_READ_DTO.getType());
        } else {
            assertEquals(202, accessEndpoint.revoke(DATABASE_1_ID, USER_2_USERNAME).getStatusCode().value());
            verify(access).delete(current, USER_2_CACHE);
        }
        freshOnce();
    }

    @Test
    void currentMembershipBlocksDuplicateCreate() throws Exception {
        current.setAccesses(List.of(DATABASE_1_USER_2_WRITE_OWN_ACCESS_CACHE));
        assertThrows(AccessNotFoundException.class, () -> accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO));
        verifyNoInteractions(access);
        freshOnce();
    }

    @Test
    void transferredOwnerCannotInspectAnotherUsersGrants() throws Exception {
        assertThrows(NotAllowedException.class, () -> grantEndpoint.find(DATABASE_1_ID, USER_2_USERNAME,
                USER_1_PRINCIPAL, new MockHttpServletRequest("GET", "/")));
        verifyNoInteractions(grants);
        freshOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "self"})
    void currentOwnerAndSelfInspectionRemainAvailable(String caller) throws Exception {
        assertEquals(200, grantEndpoint.find(DATABASE_1_ID, USER_2_USERNAME,
                caller.equals("owner") ? USER_3_PRINCIPAL : USER_2_PRINCIPAL,
                new MockHttpServletRequest("GET", "/")).getStatusCode().value());
        verify(grants).find(current, USER_2_CACHE);
        freshOnce();
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "update", "revoke", "find"})
    void unavailableCurrentMetadataNeverFallsBack(String operation) throws Exception {
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenThrow(new RemoteUnavailableException("unavailable"));
        assertThrows(RemoteUnavailableException.class, () -> {
            switch (operation) {
                case "create" -> accessEndpoint.create(DATABASE_1_ID, USER_2_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
                case "update" -> accessEndpoint.update(DATABASE_1_ID, USER_2_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
                case "revoke" -> accessEndpoint.revoke(DATABASE_1_ID, USER_2_USERNAME);
                case "find" -> grantEndpoint.find(DATABASE_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, new MockHttpServletRequest("GET", "/"));
                default -> throw new IllegalArgumentException(operation);
            }
        });
        verifyNoInteractions(access, grants, cache);
        verify(gateway).getDatabaseById(DATABASE_1_ID);
    }

    private void freshOnce() throws Exception {
        verify(gateway).getDatabaseById(DATABASE_1_ID);
        verifyNoMoreInteractions(gateway);
        verify(cache, never()).getDatabase(any());
    }
}
